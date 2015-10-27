import oap.io.Closeables;
import oap.io.Files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import static java.lang.System.out;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

public final class TestSequentialIoPerf {
    public static final int PAGE_SIZE = 1024 * 4;
    public static final long FILE_SIZE = PAGE_SIZE * 2000L * 50L;
    public static final String FILE_NAME = "d:\\workspace\\test.dat";
    public static final byte[] BLANK_PAGE = new byte[PAGE_SIZE];

    public static void main( final String[] arg ) throws Exception {
        for( final Case testCase : testCases ) {
            long avg = 0;
            int experiments = 5;
            for( int i = 0; i < experiments; i++ ) {
                deleteFile( FILE_NAME );
                System.gc();
                long writeDurationMs = testCase.test( FILE_NAME );
                long bytesWrittenPerSec = (FILE_SIZE * 1000L) / writeDurationMs;
                avg += bytesWrittenPerSec;
//                out.format( "%s\t%,d\t data/sec\n", testCase.getName(), bytesWrittenPerSec );
            }
            out.format( "%,d\t data/sec AVG: %s\n", avg / experiments, testCase.getName() );
//            out.println();
        }

        deleteFile( FILE_NAME );
    }

    private static void deleteFile( final String testFileName ) throws Exception {
        Files.delete( new File( testFileName ).toPath() );
    }

    public abstract static class Case {
        private final String name;

        public Case( final String name ) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public long test( final String fileName ) {
            long start = System.currentTimeMillis();

            try {
                testWrite( fileName );
            } catch( Exception ex ) {
                ex.printStackTrace();
            }

            return System.currentTimeMillis() - start;
        }

        public abstract void testWrite( final String fileName ) throws Exception;
    }

    private static Case[] testCases =
        {
            new RandAccessFile(),
            new RandAccessFile(),
            new RandAccessFile(),
            new RandAccessFile(),

            new BufferedStreamFile( PAGE_SIZE * 2 ),
            new BufferedStreamFile( PAGE_SIZE * 10 ),
            new BufferedStreamFile( PAGE_SIZE * 15 ),
            new BufferedStreamFile( PAGE_SIZE * 20 ),
            new BufferedStreamFile( PAGE_SIZE * 25 ),
            new BufferedStreamFile( PAGE_SIZE * 50 ),
            new BufferedStreamFile( PAGE_SIZE * 100 ),
            new BufferedStreamFile( PAGE_SIZE * 500 ),


            new BufferedChannelFile( PAGE_SIZE * 20 ),
            new BufferedChannelFile( PAGE_SIZE * 50 ),
            new BufferedChannelFile( PAGE_SIZE * 100 ),
            new BufferedChannelFile( PAGE_SIZE * 500 ),

            new MemoryMappedFile( PAGE_SIZE * 10 ),
            new MemoryMappedFile( PAGE_SIZE * 100 ),
            new MemoryMappedFile( PAGE_SIZE * 500 ),
            new MemoryMappedFile( PAGE_SIZE * 1000 )
        };

    private static class BufferedChannelFile extends Case {
        private int bufferSize;

        public BufferedChannelFile( int bufferSize ) {
            super( "BufferedChannelFile: " + bufferSize );
            this.bufferSize = bufferSize;
        }

        public void testWrite( final String fileName ) throws Exception {
            RandomAccessFile rw = new RandomAccessFile( fileName, "rw" );
            FileChannel channel =
                rw.getChannel();
            ByteBuffer buffer = ByteBuffer.wrap( new byte[bufferSize] );

            for( long i = 0; i < FILE_SIZE / bufferSize; i++ ) {
                buffer.rewind();
                channel.write( buffer );
            }
            channel.close();
            rw.close();
        }

    }

    private static class MemoryMappedFile extends Case {

        private int bufferSize;

        public MemoryMappedFile( int bufferSize ) {
            super( "MemoryMappedFile: " + bufferSize );
            this.bufferSize = bufferSize;
        }

        public void testWrite( final String fileName ) throws Exception {
            RandomAccessFile rw = new RandomAccessFile( fileName, "rw" );
            FileChannel channel = rw.getChannel();
            MappedByteBuffer buffer = channel.map( READ_WRITE, 0, bufferSize );

            ArrayList<MappedByteBuffer> buffers = new ArrayList<>();
            buffers.add( buffer );

            ByteBuffer data = ByteBuffer.wrap( BLANK_PAGE );

            for( long i = 0; i < FILE_SIZE / PAGE_SIZE; i++ ) {
                if( !buffer.hasRemaining() ) {
                    buffer = channel.map( READ_WRITE, channel.size(), bufferSize );
                    buffers.add( buffer );
                }
                data.rewind();
                buffer.put( data );
            }

            Closeables.close( channel );
            Closeables.close( rw );
            buffers.forEach( Closeables::close );
        }

    }

    private static class BufferedStreamFile extends Case {

        private int bufferSize;

        public BufferedStreamFile( int bufferSize ) {
            super( "BufferedStreamFile: " + bufferSize );
            this.bufferSize = bufferSize;
        }

        public void testWrite( final String fileName ) throws Exception {
            OutputStream out = new BufferedOutputStream( new FileOutputStream( fileName ), bufferSize );

            for( long i = 0; i < FILE_SIZE / PAGE_SIZE; i++ ) out.write( BLANK_PAGE );
            out.close();
        }
    }

    private static class RandAccessFile extends Case {
        public RandAccessFile() {
            super( "RandomAccessFile" );
        }

        public void testWrite( final String fileName ) throws Exception {
            RandomAccessFile file = new RandomAccessFile( fileName, "rw" );

            for( long i = 0; i < FILE_SIZE / PAGE_SIZE; i++ ) file.write( BLANK_PAGE );

            file.close();
        }
    }
}
