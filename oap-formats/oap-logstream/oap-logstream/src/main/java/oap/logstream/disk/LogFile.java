package oap.logstream.disk;

import oap.json.Binder;
import oap.logstream.CompletedLogLoggerException;
import oap.logstream.LogId;
import oap.logstream.LoggerException;

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

import static java.nio.charset.StandardCharsets.UTF_8;

public class LogFile {
    public static final String EXTENSION_LOG_METADATA = ".metadata.yaml";
    public static final String EXTENSION_LOG_TRANSACTION = ".metadata.transaction";
    public static final String EXTENSION_LOG_COMPLETED = ".metadata.completed";

    public final Path outFilename;
    @Nullable
    protected FileChannel out;

    public LogFile( Path outFilename ) {
        this( outFilename, null );
    }

    protected LogFile( Path outFilename, @Nullable FileChannel out ) throws LoggerException {
        this.outFilename = outFilename;
        this.out = out;
    }

    public static Path pathFor( Path file, String extension ) {
        return Path.of( file.toString() + extension );
    }

    public static LogFile loadFromPath( Path path ) {
        if( path.endsWith( EXTENSION_LOG_METADATA ) ) {
            return new LogFile( getMainFilePath( path, EXTENSION_LOG_METADATA ) );
        } else if( path.endsWith( EXTENSION_LOG_TRANSACTION ) ) {
            return new LogFile( getMainFilePath( path, EXTENSION_LOG_TRANSACTION ) );
        } else if( path.endsWith( EXTENSION_LOG_COMPLETED ) ) {
            return new LogFile( getMainFilePath( path, EXTENSION_LOG_COMPLETED ) );
        }
        return new LogFile( path );
    }

    private static Path getMainFilePath( Path path, String ext ) {
        return Paths.get( path.toString().substring( 0, path.toString().length() - ext.length() ) );
    }

    public LogFile create( LogId logId ) {
        try {
            oap.io.Files.ensureDirectory( outFilename.getParent() );
            FileChannel out = FileChannel.open( outFilename, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.APPEND );

            LogFile logFile = new LogFile( outFilename, out );
            logFile.syncLogMetadata( new LogMetadata( logId ) );

            return this;
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public long beginTransaction() throws LoggerException {
        try {
            Path path = pathFor( outFilename, EXTENSION_LOG_TRANSACTION );

            long dataSize;
            if( Files.exists( path ) ) {
                dataSize = Long.parseLong( Files.readString( path, UTF_8 ) );
            } else {
                dataSize = 0;
            }

            return dataSize;
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public void commitTransaction( int length ) throws LoggerException {
        try {
            Path path = pathFor( outFilename, EXTENSION_LOG_TRANSACTION );
            Path tmpPath = pathFor( outFilename, EXTENSION_LOG_TRANSACTION + ".tmp" );

            long dataSize;
            if( Files.exists( path ) ) {
                dataSize = Long.parseLong( Files.readString( path, UTF_8 ) );
            } else {
                dataSize = 0;
            }


            dataSize += length;

            Files.writeString( tmpPath, String.valueOf( dataSize ), UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE );
            Files.move( tmpPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public void readyForUpload() throws LoggerException {
        try {
            Files.createFile( pathFor( outFilename, EXTENSION_LOG_COMPLETED ) );
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

    public void close() {
        try {
            out.close();
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public void writeAndCommitTransaction( byte[] bytes, int offset, int length ) throws LoggerException {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap( bytes, offset, length );
            do {
                out.write( byteBuffer );
            } while( byteBuffer.hasRemaining() );
            out.force( true );

            commitTransaction( length );
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public void beginTransactionWriteAndCommitTransaction( byte[] buffer, int offset, int length ) throws LoggerException {
        try {
            long position = beginTransaction();

            ByteBuffer byteBuffer = ByteBuffer.wrap( buffer, offset, length );
            do {
                out.position( position );
                out.write( byteBuffer );
            } while( byteBuffer.hasRemaining() );
            out.force( true );

            commitTransaction( length );
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public LogMetadata getLogMetadata() {
        return Binder.yaml.unmarshal( LogMetadata.class, pathFor( outFilename, EXTENSION_LOG_METADATA ) );
    }

    public void addProperty( String name, String value ) {
        LogMetadata metadata = getLogMetadata();
        metadata.setProperty( name, value );
        syncLogMetadata( metadata );
    }

    private void syncLogMetadata( LogMetadata logMetadata ) {
        try {
            Path path = pathFor( outFilename, EXTENSION_LOG_METADATA );
            Path tmpFile = Path.of( path + ".tmp" );
            Binder.yaml.marshal( tmpFile, logMetadata );

            Files.move( tmpFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public boolean existsAndValid() {
        boolean mainFile = Files.exists( outFilename );
        boolean transactionFile = Files.exists( pathFor( outFilename, EXTENSION_LOG_TRANSACTION ) );
        boolean metadataFile = Files.exists( pathFor( outFilename, EXTENSION_LOG_METADATA ) );
        boolean completedFile = Files.exists( pathFor( outFilename, EXTENSION_LOG_COMPLETED ) );

        if( mainFile ) {
            if( completedFile ) {
                throw new CompletedLogLoggerException( outFilename + EXTENSION_LOG_COMPLETED + " already exists" );
            }

            return transactionFile && metadataFile;
        }

        return false;
    }
}
