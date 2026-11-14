/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.logstream.disk;

import oap.compression.Compression;
import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.logstream.formats.rowbinary.RowBinaryUtils;
import oap.template.TemplateEngineFixture;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.FastByteArrayOutputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static oap.logstream.LogStreamProtocol.ProtocolVersion.ROW_BINARY_V4;
import static oap.logstream.Timestamp.BPH_12;

/**
 * RowBinaryWriter / LogFile write + commit cost for 64 KB client buffers. The commit is the fdatasync of the data
 * ({@link SyncMode#ALWAYS}, default); {@code -Dlogstream.perftest.sync=PERIODIC} (or ON_CLOSE) measures the write without it.
 * <p>
 * The codec does not change the commit cost (only the payload size), so (a) and (b) use ZSTD (the client default);
 * (c) runs all codecs because the file header is compressed with the file codec.
 * <p>
 * Run on the target Linux disk, not on Windows (fsync and rename behave differently):
 * {@code -Dlogstream.perftest.dir=/data/logs-perftest}; without the property the test directory is used.
 */
@Test( enabled = false )
public class LogFilePerftest extends Fixtures {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final String FILE_PATTERN = "{{ p }}/{{ LOG_TYPE }}-{{ INTERVAL }}-{{ LOG_VERSION }}";
    private static final String[] HEADERS = { "TIMESTAMP", "REQUEST_ID", "COUNTRY", "DEVICE", "BID", "PRICE", "DOMAIN", "SEGMENTS" };
    private static final String[] COUNTRIES = { "US", "UA", "DE", "GB", "FR", "PL", "IN", "BR" };
    private static final String[] DEVICES = { "phone", "tablet", "desktop", "ctv" };

    private static final int SINGLE_WARMING = 200;
    private static final int SINGLE_SAMPLES = 2_000;
    private static final int THREADS = 16;
    private static final int MULTI_WARMING = 50;
    private static final int MULTI_SAMPLES = 500;
    private static final int FILES_WARMING = 100;
    private static final int FILES = 1_000;

    private final TestDirectoryFixture testDirectoryFixture;
    private final TemplateEngineFixture templateEngineFixture;
    /**
     * -Dlogstream.perftest.dir/logfile-perftest-UUID, deleted after the test
     */
    private Path externalRoot;

    public LogFilePerftest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        templateEngineFixture = fixture( new TemplateEngineFixture() );
    }

    private static LogId logId( String logType, String p ) {
        byte[][] types = new byte[HEADERS.length][];
        for( int i = 0; i < types.length; i++ ) types[i] = new byte[] { Types.STRING.id };
        return new LogId( "", logType, Map.of( "p", p ), HEADERS, types );
    }

    /**
     * ~64 KB of RowBinary rows (uncompressed client buffer), compressed with the codec - the payload the server appends to the file
     */
    private static byte[] payload( LogStreamCompression compression ) throws IOException {
        Random random = new Random( 1 );
        DateTime time = new DateTime( 2026, 11, 14, 13, 0, DateTimeZone.UTC );
        FastByteArrayOutputStream rows = new FastByteArrayOutputStream( BUFFER_SIZE );
        while( true ) {
            byte[] line = RowBinaryUtils.line( List.of(
                time.plusMillis( random.nextInt( 3_600_000 ) ),
                new UUID( random.nextLong(), random.nextLong() ).toString(),
                COUNTRIES[random.nextInt( COUNTRIES.length )],
                DEVICES[random.nextInt( DEVICES.length )],
                random.nextInt( 10 ) == 0,
                random.nextInt( 5000 ) / 1000.0,
                "site" + random.nextInt( 2000 ) + ".com",
                List.of( "s" + random.nextInt( 50 ), "s" + random.nextInt( 50 ), "s" + random.nextInt( 50 ) ) ) );
            if( rows.length + line.length > BUFFER_SIZE ) break;
            rows.write( line );
        }

        return switch( compression ) {
            case GZIP -> Compression.gzip( rows.array, 0, rows.length );
            case LZ4 -> Compression.lz4( rows.array, 0, rows.length );
            case ZSTD -> Compression.zstd( rows.array, 0, rows.length, LogStreamCompression.ZSTD.defaultLevel );
        };
    }

    private static void print( String name, long[] latenciesNs, long totalNs, long bytes ) {
        long[] sorted = latenciesNs.clone();
        Arrays.sort( sorted );
        double seconds = totalNs / 1e9;
        System.out.printf( "%-28s ops %6d  %9.1f ops/s  %8.1f MB/s  avg %8.1f us  p50 %8.1f us  p99 %8.1f us  max %9.1f us%n",
            name, sorted.length, sorted.length / seconds, bytes / seconds / ( 1024 * 1024 ),
            Arrays.stream( sorted ).average().orElse( 0 ) / 1e3,
            percentile( sorted, 0.50 ) / 1e3, percentile( sorted, 0.99 ) / 1e3, sorted[sorted.length - 1] / 1e3 );
    }

    private static long percentile( long[] sorted, double p ) {
        return sorted[Math.min( sorted.length - 1, ( int ) ( sorted.length * p ) )];
    }

    @BeforeMethod
    public void fixTime() {
        // no file rotation (INTERVAL) during a run
        Dates.setTimeFixed( 2026, 11, 14, 13, 1 );
    }

    @AfterMethod
    public void cleanup() {
        DateTimeUtils.setCurrentMillisSystem();
        if( externalRoot != null ) {
            oap.io.Files.delete( externalRoot );
            externalRoot = null;
        }
    }

    private Path directory( String name ) throws IOException {
        String dir = System.getProperty( "logstream.perftest.dir" );
        Path path;
        if( dir != null ) {
            if( externalRoot == null ) externalRoot = Path.of( dir ).resolve( "logfile-perftest-" + UUID.randomUUID() );
            path = externalRoot.resolve( name );
        } else {
            path = testDirectoryFixture.testPath( name );
        }
        Files.createDirectories( path );
        return path;
    }

    private void printDirectory( Path directory ) throws IOException {
        System.out.println( "directory " + directory + " (" + Files.getFileStore( directory ).type() + ") sync "
            + System.getProperty( "logstream.perftest.sync", SyncMode.ALWAYS.name() ) );
    }

    private RowBinaryWriter writer( Path directory, LogStreamCompression compression, LogId logId ) {
        SyncMode syncMode = SyncMode.valueOf( System.getProperty( "logstream.perftest.sync", SyncMode.ALWAYS.name() ) );
        return new RowBinaryWriter( templateEngineFixture.templateEngine, compression, syncMode, directory, FILE_PATTERN, logId,
            BUFFER_SIZE, BPH_12, 20, "localhost", null );
    }

    /**
     * (a) 1 file, 1 thread
     */
    @Test( enabled = false )
    public void singleFileSingleThread() throws IOException {
        LogStreamCompression compression = LogStreamCompression.ZSTD;
        byte[] payload = payload( compression );
        Path directory = directory( "single" );
        printDirectory( directory );
        System.out.println( compression + " payload " + payload.length + " bytes" );

        try( RowBinaryWriter writer = writer( directory, compression, logId( "single", "1" ) ) ) {
            for( int i = 0; i < SINGLE_WARMING; i++ ) writer.write( ROW_BINARY_V4, payload );

            long[] latencies = new long[SINGLE_SAMPLES];
            long start = System.nanoTime();
            for( int i = 0; i < SINGLE_SAMPLES; i++ ) {
                long t = System.nanoTime();
                writer.write( ROW_BINARY_V4, payload );
                latencies[i] = System.nanoTime() - t;
            }
            long total = System.nanoTime() - start;

            print( "1 file x 1 thread", latencies, total, ( long ) payload.length * SINGLE_SAMPLES );
        }
    }

    /**
     * (b) 16 files x 16 threads, a writer (file) per thread
     */
    @Test( enabled = false )
    public void filesPerThread() throws Exception {
        LogStreamCompression compression = LogStreamCompression.ZSTD;
        byte[] payload = payload( compression );
        Path directory = directory( "multi" );
        printDirectory( directory );
        System.out.println( compression + " payload " + payload.length + " bytes" );

        List<RowBinaryWriter> writers = new ArrayList<>();
        for( int i = 0; i < THREADS; i++ ) writers.add( writer( directory, compression, logId( "multi" + i, "1" ) ) );

        try( ExecutorService executor = Executors.newFixedThreadPool( THREADS ) ) {
            run( executor, writers, payload, MULTI_WARMING );

            long start = System.nanoTime();
            long[][] latencies = run( executor, writers, payload, MULTI_SAMPLES );
            long total = System.nanoTime() - start;

            long[] all = Arrays.stream( latencies ).flatMapToLong( Arrays::stream ).toArray();
            print( THREADS + " files x " + THREADS + " threads", all, total, ( long ) payload.length * all.length );
        } finally {
            for( RowBinaryWriter writer : writers ) writer.close();
        }
    }

    private static long[][] run( ExecutorService executor, List<RowBinaryWriter> writers, byte[] payload, int samples ) throws Exception {
        List<Future<long[]>> futures = new ArrayList<>();
        for( RowBinaryWriter writer : writers ) {
            futures.add( executor.submit( () -> {
                long[] latencies = new long[samples];
                for( int i = 0; i < samples; i++ ) {
                    long t = System.nanoTime();
                    writer.write( ROW_BINARY_V4, payload );
                    latencies[i] = System.nanoTime() - t;
                }
                return latencies;
            } ) );
        }

        long[][] result = new long[futures.size()][];
        for( int i = 0; i < futures.size(); i++ ) result[i] = futures.get( i ).get();
        return result;
    }

    /**
     * (c) 1000 new files, one buffer each: file + metadata (yaml, VERSION property), compressed header, payload; then close (.completed)
     */
    @Test( enabled = false )
    public void createFiles() throws IOException {
        for( LogStreamCompression compression : LogStreamCompression.values() ) {
            byte[] payload = payload( compression );
            Path directory = directory( "files-" + compression.name().toLowerCase() );
            if( compression.ordinal() == 0 ) printDirectory( directory );
            System.out.println( compression + " payload " + payload.length + " bytes" );

            createFiles( directory.resolve( "warming" ), compression, payload, FILES_WARMING, null, null );

            long[] write = new long[FILES];
            long[] close = new long[FILES];
            long start = System.nanoTime();
            createFiles( directory.resolve( "files" ), compression, payload, FILES, write, close );
            long total = System.nanoTime() - start;

            print( compression + " create+write", write, total, ( long ) payload.length * FILES );
            print( compression + " close", close, total, 0 );
        }
    }

    private void createFiles( Path directory, LogStreamCompression compression, byte[] payload, int count, long[] write, long[] close ) {
        for( int i = 0; i < count; i++ ) {
            RowBinaryWriter writer = writer( directory, compression, logId( "files", String.valueOf( i ) ) );

            long t = System.nanoTime();
            writer.write( ROW_BINARY_V4, payload );
            long t2 = System.nanoTime();
            writer.close();
            long t3 = System.nanoTime();

            if( write != null ) write[i] = t2 - t;
            if( close != null ) close[i] = t3 - t2;
        }
    }
}
