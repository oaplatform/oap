package oap.logstream.disk;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.logstream.LogId;
import oap.logstream.LogStreamCompression;
import oap.logstream.LoggerException;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A log data file and its sidecars:
 * <ul>
 *     <li>{@code <file>.metadata.yaml} - {@link LogMetadata} (LogId, schema, properties)</li>
 *     <li>{@code <file>.metadata.completed} - the file is closed, the whole file is valid, ready for upload</li>
 *     <li>{@code <file>.metadata.transaction} - legacy (written by the previous versions): the committed data offset</li>
 * </ul>
 * The data file is a sequence of self-contained compressed frames (gzip members / LZ4 frames / zstd frames).
 * A write is committed when it is synced to the device ({@link SyncMode}); there is no separate commit record.
 * A file left without {@code .completed} by a crash is repaired by {@link #recover()}.
 */
@Slf4j
@ToString( of = "outFilename" )
@EqualsAndHashCode( of = "outFilename" )
public class LogFile implements AutoCloseable {
    public static final String EXTENSION_LOG_METADATA = ".metadata.yaml";
    /**
     * written by the previous versions only: the committed data offset. New files do not have it;
     * the files left on disk by the previous version are still moved / deleted / recovered with it.
     */
    @Deprecated
    public static final String EXTENSION_LOG_TRANSACTION = ".metadata.transaction";
    public static final String EXTENSION_LOG_COMPLETED = ".metadata.completed";

    public final Path outFilename;
    private final Path transactionFile;
    private final Path metadataFile;
    private final Path completedFile;
    @Nullable
    protected FileChannel out;
    /**
     * end of the data written to out
     */
    private long position;
    /**
     * data written to out but not yet synced
     */
    private boolean unsynced;

    public LogFile( Path outFilename ) {
        this( outFilename, null );
    }

    protected LogFile( Path outFilename, @Nullable FileChannel out ) throws LoggerException {
        this.outFilename = outFilename;
        this.out = out;
        this.transactionFile = pathFor( EXTENSION_LOG_TRANSACTION );
        this.metadataFile = pathFor( EXTENSION_LOG_METADATA );
        this.completedFile = pathFor( EXTENSION_LOG_COMPLETED );
    }

    public static LogFile loadFromPath( Path path ) {
        String s = path.toString();
        if( s.endsWith( EXTENSION_LOG_METADATA ) ) {
            return new LogFile( getMainFilePath( path, EXTENSION_LOG_METADATA ) );
        } else if( s.endsWith( EXTENSION_LOG_TRANSACTION ) ) {
            return new LogFile( getMainFilePath( path, EXTENSION_LOG_TRANSACTION ) );
        } else if( s.endsWith( EXTENSION_LOG_COMPLETED ) ) {
            return new LogFile( getMainFilePath( path, EXTENSION_LOG_COMPLETED ) );
        }
        return new LogFile( path );
    }

    private static Path getMainFilePath( Path path, String ext ) {
        return Paths.get( path.toString().substring( 0, path.toString().length() - ext.length() ) );
    }

    /**
     * the codec of a data file by its extension (.rb.gz / .rb.lz4 / .rb.zst)
     */
    public static Optional<LogStreamCompression> compressionOf( Path dataFile ) {
        String name = dataFile.getFileName().toString();
        for( LogStreamCompression compression : LogStreamCompression.values() ) {
            if( name.endsWith( "." + compression.logFormat.extension ) ) return Optional.of( compression );
        }
        return Optional.empty();
    }

    public Path pathFor( String extension ) {
        return Path.of( outFilename.toString() + extension );
    }

    public LogFile create( LogId logId ) {
        return create( logId, Map.of() );
    }

    /**
     * creates the data file (must not exist) and writes the metadata once
     */
    public LogFile create( LogId logId, Map<String, String> properties ) {
        try {
            oap.io.Files.ensureDirectory( outFilename.getParent() );
            out = FileChannel.open( outFilename, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE );
            position = 0;
            unsynced = false;

            LogMetadata metadata = new LogMetadata( logId );
            properties.forEach( metadata::setProperty );
            syncLogMetadata( metadata );

            return this;
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    /**
     * Appends the buffers with one gathering write. On failure the file is truncated back to the previous end,
     * so it always ends at a frame boundary.
     *
     * @param sync fdatasync before return
     */
    public void append( boolean sync, ByteBuffer... buffers ) throws LoggerException {
        Preconditions.checkState( out != null, "%s is not open", outFilename );

        long length = 0;
        for( ByteBuffer buffer : buffers ) length += buffer.remaining();

        long start = position;
        try {
            out.position( start );
            long written = 0;
            while( written < length ) {
                written += out.write( buffers );
            }
            if( sync ) out.force( false );
            else unsynced = true;

            position = start + length;
        } catch( IOException e ) {
            try {
                out.truncate( start );
                out.position( start );
            } catch( IOException truncateError ) {
                e.addSuppressed( truncateError );
            }
            throw new LoggerException( e );
        }
    }

    public void append( boolean sync, byte[] buffer, int offset, int length ) throws LoggerException {
        append( sync, ByteBuffer.wrap( buffer, offset, length ) );
    }

    /**
     * fdatasync of the data appended without sync
     */
    public void sync() throws LoggerException {
        if( out == null || !unsynced ) return;
        try {
            out.force( false );
            unsynced = false;
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public long position() {
        return position;
    }

    /**
     * data appended without sync and not synced yet
     */
    public boolean hasUnsyncedData() {
        return unsynced;
    }

    public void readyForUpload() throws LoggerException {
        try {
            Files.createFile( completedFile );
        } catch( FileAlreadyExistsException ignored ) {
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public long getDataSize() throws LoggerException {
        try {
            return Files.size( outFilename );
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    /**
     * syncs the unsynced data and closes the file
     */
    @Override
    public void close() {
        try {
            if( out != null ) {
                try {
                    if( unsynced && out.isOpen() ) out.force( false );
                } finally {
                    unsynced = false;
                    out.close();
                    out = null;
                }
            }
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public LogMetadata getLogMetadata() {
        return Binder.yaml.unmarshal( LogMetadata.class, getMetadataFile() );
    }

    public void addProperty( String name, String value ) {
        LogMetadata metadata = getLogMetadata();
        metadata.setProperty( name, value );
        syncLogMetadata( metadata );
    }

    private void syncLogMetadata( LogMetadata logMetadata ) {
        try {
            Path path = getMetadataFile();
            Path tmpFile = Path.of( path + ".tmp" );
            Binder.yaml.marshal( tmpFile, logMetadata );

            Files.move( tmpFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    /**
     * any file of this log exists (data or a sidecar) - the name is taken
     */
    public boolean exists() {
        return Files.exists( outFilename ) || Files.exists( metadataFile ) || Files.exists( completedFile ) || Files.exists( transactionFile );
    }

    public boolean existsAndValid() {
        return isValid();
    }

    public boolean isCompleted() {
        return Files.exists( getCompletedFile() );
    }

    public boolean isValid() {
        return Files.exists( outFilename ) && Files.exists( metadataFile );
    }

    /**
     * written by the previous version (has {@code .metadata.transaction})
     */
    public boolean isLegacy() {
        return Files.exists( transactionFile );
    }

    /**
     * the committed data offset of a legacy file; empty - no transaction file or it is empty / not a number
     */
    public OptionalLong legacyCommittedPosition() {
        try {
            if( !Files.exists( transactionFile ) ) return OptionalLong.empty();
            String value = Files.readString( transactionFile, UTF_8 ).trim();
            if( value.isEmpty() ) return OptionalLong.empty();
            long committed = Long.parseLong( value );
            return committed >= 0 ? OptionalLong.of( committed ) : OptionalLong.empty();
        } catch( IOException | NumberFormatException e ) {
            log.warn( "[{}] invalid transaction file: {}", outFilename, e.getMessage() );
            return OptionalLong.empty();
        }
    }

    /**
     * Repairs a file left without {@code .completed} (crash): truncates it to the end of the last valid frame
     * (for a legacy file - to the committed offset if it is a frame boundary), syncs it and marks it completed.
     * A file without data frames (only the RowBinary header or less) is deleted with all its sidecars.
     * <p>
     * The file must not be open by a writer.
     */
    public Recovery recover() throws LoggerException {
        Preconditions.checkState( out == null, "%s is open", outFilename );
        if( isCompleted() ) return Recovery.SKIPPED;

        Optional<LogStreamCompression> compression = compressionOf( outFilename );
        if( compression.isEmpty() ) {
            log.warn( "[{}] recovery: unknown data file format, skipped", outFilename );
            return Recovery.SKIPPED;
        }

        try {
            if( !Files.exists( outFilename ) ) {
                log.warn( "[{}] recovery: no data file, the sidecars are deleted", outFilename );
                delete();
                return Recovery.DELETED;
            }
            if( !Files.exists( metadataFile ) ) {
                if( Files.size( outFilename ) == 0 ) {
                    log.warn( "[{}] recovery: empty data file without metadata, deleted", outFilename );
                    delete();
                    return Recovery.DELETED;
                }
                log.warn( "[{}] recovery: no metadata, skipped", outFilename );
                return Recovery.SKIPPED;
            }

            boolean legacy = isLegacy();
            OptionalLong committed = legacy ? legacyCommittedPosition() : OptionalLong.empty();

            long boundary;
            int frames;
            long size;
            try( FileChannel channel = FileChannel.open( outFilename, StandardOpenOption.READ, StandardOpenOption.WRITE ) ) {
                FrameScanner.Scan scan = FrameScanner.scan( channel, compression.get() );
                size = scan.size();
                boundary = scan.validEnd();

                if( legacy ) {
                    if( committed.isPresent() && committed.getAsLong() <= scan.validEnd() && scan.isBoundary( committed.getAsLong() ) ) {
                        boundary = committed.getAsLong();
                    } else {
                        log.warn( "[{}] recovery: committed offset {} is not a frame boundary (valid frames end at {}, size {}), the scan result is used",
                            outFilename, committed.isPresent() ? committed.getAsLong() : "<none>", scan.validEnd(), size );
                    }
                }

                frames = scan.framesUpTo( boundary );
                if( frames > 1 ) {
                    if( boundary < size ) {
                        log.warn( "[{}] recovery: {} bytes after offset {} dropped", outFilename, size - boundary, boundary );
                        channel.truncate( boundary );
                    }
                    channel.force( true );
                }
            }

            // the first frame is the RowBinary header
            if( frames <= 1 ) {
                log.warn( "[{}] recovery: no data frames (size {}), deleted", outFilename, size );
                delete();
                return Recovery.DELETED;
            }

            readyForUpload();
            log.info( "[{}] recovered: {} frames, {} bytes{}", outFilename, frames, boundary, legacy ? " (legacy)" : "" );
            return Recovery.RECOVERED;
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public long getMaxModificationTime() throws LoggerException {
        try {
            boolean mainFile = Files.exists( outFilename );
            boolean transactionFileExists = Files.exists( transactionFile );
            boolean metadataFileExists = Files.exists( metadataFile );
            boolean completedFileExists = Files.exists( completedFile );

            long time = -1;

            if( mainFile ) {
                time = Long.max( time, Files.getLastModifiedTime( outFilename ).toMillis() );
            }
            if( transactionFileExists ) {
                time = Long.max( time, Files.getLastModifiedTime( transactionFile ).toMillis() );
            }
            if( metadataFileExists ) {
                time = Long.max( time, Files.getLastModifiedTime( metadataFile ).toMillis() );
            }
            if( completedFileExists ) {
                time = Long.max( time, Files.getLastModifiedTime( completedFile ).toMillis() );
            }

            return time;
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public Path getCompletedFile() {
        return completedFile;
    }

    public Path getMetadataFile() {
        return metadataFile;
    }

    /**
     * legacy, see {@link #EXTENSION_LOG_TRANSACTION}
     */
    @Deprecated
    public Path getTransactionFile() {
        return transactionFile;
    }

    public void delete() throws LoggerException {
        try {
            Files.deleteIfExists( outFilename );
            Files.deleteIfExists( getMetadataFile() );
            Files.deleteIfExists( transactionFile );
            Files.deleteIfExists( getCompletedFile() );
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public void moveTo( Path destinationDirectory, Path baseDirectory ) throws LoggerException {
        boolean mainFileExists = Files.exists( outFilename );
        boolean transactionFileExists = Files.exists( transactionFile );
        boolean metadataFileExists = Files.exists( metadataFile );
        boolean completedFileExists = Files.exists( completedFile );

        if( mainFileExists ) {
            moveFile( outFilename, baseDirectory, destinationDirectory );
        }
        if( transactionFileExists ) {
            moveFile( transactionFile, baseDirectory, destinationDirectory );
        }
        if( metadataFileExists ) {
            moveFile( metadataFile, baseDirectory, destinationDirectory );
        }
        if( completedFileExists ) {
            moveFile( completedFile, baseDirectory, destinationDirectory );
        }
    }

    private void moveFile( Path file, Path baseDirectory, Path destinationDirectory ) throws LoggerException {
        try {
            Path targetPath = getTargetPath( baseDirectory, file, destinationDirectory );
            Files.createDirectories( targetPath.getParent() );
            Files.move( file, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    private Path getTargetPath( Path baseDirectory, Path file, Path destinationDirectory ) {
        return destinationDirectory.resolve( FilenameUtils.separatorsToUnix( file.toString().substring( baseDirectory.toString().length() + 1 ) ) );
    }

    public enum Recovery {
        /**
         * truncated to the last valid frame and marked completed
         */
        RECOVERED,
        /**
         * no data frames - the file and its sidecars are deleted
         */
        DELETED,
        /**
         * completed already, unknown format or no metadata - not changed
         */
        SKIPPED
    }
}
