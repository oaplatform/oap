package oap.logstream.disk;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Executors;
import oap.io.Files;
import oap.logstream.Timestamp;
import oap.util.Dates;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.joda.time.DateTimeZone.UTC;

@Slf4j
public abstract class AbstractFinisher implements Runnable {
    public static final String CORRUPTED_DIRECTORY = ".corrupted";
    public final Path sourceDirectory;
    public final long safeInterval;
    public final List<String> mask;
    public final Path corruptedDirectory;
    private final Timestamp timestamp;
    public int threads = Runtime.getRuntime().availableProcessors();
    protected int bufferSize = 1024 * 256 * 4 * 4;


    @SneakyThrows
    protected AbstractFinisher( Path sourceDirectory, long safeInterval, List<String> mask, Timestamp timestamp ) {
        this.sourceDirectory = sourceDirectory;
        this.safeInterval = safeInterval;
        this.mask = mask;
        this.corruptedDirectory = sourceDirectory.resolve( CORRUPTED_DIRECTORY );
        this.timestamp = timestamp;
    }

    public void start() {
        log.info( "threads = {}, sourceDirectory = {}, corruptedDirectory = {}, mask = {}, safeInterval = {}, bufferSize = {}",
            threads, sourceDirectory, corruptedDirectory, mask, Dates.durationToString( safeInterval ), bufferSize );
    }

    @Override
    public void run() {
        run( false );
    }

    @SneakyThrows
    public void run( boolean forceSync ) {
        log.debug( "force {} let's start packing of {} in {}", forceSync, mask, sourceDirectory );

        log.debug( "current timestamp is {}", timestamp.toStartOfBucket( DateTime.now( UTC ) ) );
        var bucketStartTime = timestamp.currentBucketStartMillis();
        var elapsed = DateTimeUtils.currentTimeMillis() - bucketStartTime;
        if( elapsed < safeInterval ) {
            log.debug( "not safe to process yet ({}ms), some of the files could still be open, waiting...", elapsed );
            cleanup();
            log.debug( "packing is skipped" );
            return;
        }
        var pool = Executors.newFixedBlockingThreadPool( threads, new ThreadFactoryBuilder().setNameFormat( "finisher-%d" ).build() );
        for( Path path : Files.wildcard( sourceDirectory, mask ) ) {
            if( path.startsWith( corruptedDirectory ) ) continue;
            if( LogMetadata.isMetadata( path ) ) continue;

            DateTime lastModifiedTime = timestamp.toStartOfBucket( new DateTime( Files.getLastModifiedTime( path ), UTC ) );
            if( forceSync || lastModifiedTime.isBefore( bucketStartTime ) ) {
                pool.execute( () -> process( path, lastModifiedTime ) );
            } else log.debug( "skipping (current timestamp) {}", path );
        }
        pool.shutdown();
        long fullTimeout = DateTime.now().getMillis() + TimeUnit.MINUTES.toMillis( 20 );
        while( !pool.awaitTermination( 1, TimeUnit.MINUTES ) ) {
            if( DateTime.now().getMillis() <= fullTimeout ) {
                log.debug( "Timeout passed, but pool still is working... {} tasks left", pool.shutdownNow().size() );
                break;
            }
            log.debug( "Waiting for finishing..." );
        }
        cleanup();
        log.debug( "packing is done" );
    }

    protected abstract void cleanup();

    protected abstract void process( Path path, DateTime bucketTime );
}
