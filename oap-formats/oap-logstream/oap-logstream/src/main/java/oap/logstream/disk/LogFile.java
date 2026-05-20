package oap.logstream.disk;

import lombok.ToString;
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

@ToString( exclude = "out" )
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

    public Path pathFor( String extension ) {
        return Path.of( outFilename.toString() + extension );
    }

    public LogFile create( LogId logId ) {
        try {
            oap.io.Files.ensureDirectory( outFilename.getParent() );
            out = FileChannel.open( outFilename, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.APPEND );

            syncLogMetadata( new LogMetadata( logId ) );

            return this;
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }

    public long beginTransaction() throws LoggerException {
        try {
            Path path = pathFor( EXTENSION_LOG_TRANSACTION );

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

    public long getTransactionPosition() throws LoggerException {
        return beginTransaction();
    }

    public void commitTransaction( int length ) throws LoggerException {
        try {
            Path path = pathFor( EXTENSION_LOG_TRANSACTION );
            Path tmpPath = pathFor( EXTENSION_LOG_TRANSACTION + ".tmp" );

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
            Files.createFile( pathFor( EXTENSION_LOG_COMPLETED ) );
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
            if( out != null ) {
                out.close();
            }
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
        return Binder.yaml.unmarshal( LogMetadata.class, pathFor( EXTENSION_LOG_METADATA ) );
    }

    public void addProperty( String name, String value ) {
        LogMetadata metadata = getLogMetadata();
        metadata.setProperty( name, value );
        syncLogMetadata( metadata );
    }

    private void syncLogMetadata( LogMetadata logMetadata ) {
        try {
            Path path = pathFor( EXTENSION_LOG_METADATA );
            Path tmpFile = Path.of( path + ".tmp" );
            Binder.yaml.marshal( tmpFile, logMetadata );

            Files.move( tmpFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public boolean existsAndValid() {
        boolean mainFile = Files.exists( outFilename );
        boolean transactionFile = Files.exists( pathFor( EXTENSION_LOG_TRANSACTION ) );
        boolean metadataFile = Files.exists( pathFor( EXTENSION_LOG_METADATA ) );
        boolean completedFile = Files.exists( pathFor( EXTENSION_LOG_COMPLETED ) );

        if( mainFile ) {
            if( completedFile ) {
                throw new CompletedLogLoggerException( outFilename + EXTENSION_LOG_COMPLETED + " already exists" );
            }

            return transactionFile && metadataFile;
        }

        return false;
    }

    public boolean isCompleted() {
        return Files.exists( pathFor( EXTENSION_LOG_COMPLETED ) );
    }

    public boolean isValid() {
        boolean mainFile = Files.exists( outFilename );
        boolean transactionFile = Files.exists( pathFor( EXTENSION_LOG_TRANSACTION ) );
        boolean metadataFile = Files.exists( pathFor( EXTENSION_LOG_METADATA ) );

        return mainFile && transactionFile && metadataFile;
    }

    public long getMaxModificationTime() throws LoggerException {
        try {
            Path transactionFile = pathFor( EXTENSION_LOG_TRANSACTION );
            Path metadataFile = pathFor( EXTENSION_LOG_METADATA );
            Path completedFile = pathFor( EXTENSION_LOG_COMPLETED );

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

    public void delete() throws LoggerException {
        try {
            Files.deleteIfExists( outFilename );
            Files.deleteIfExists( pathFor( EXTENSION_LOG_METADATA ) );
            Files.deleteIfExists( pathFor( EXTENSION_LOG_TRANSACTION ) );
            Files.deleteIfExists( pathFor( EXTENSION_LOG_COMPLETED ) );
        } catch( IOException e ) {
            throw new LoggerException( e );
        }
    }
}
