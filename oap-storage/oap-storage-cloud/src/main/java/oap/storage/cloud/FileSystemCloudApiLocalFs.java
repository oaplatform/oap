package oap.storage.cloud;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.io.IoStreams;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class FileSystemCloudApiLocalFs implements FileSystemCloudApi {

    private final Path basedir;

    public FileSystemCloudApiLocalFs( FileSystemConfiguration fileSystemConfiguration, String container ) {
        String basedir = ( String ) fileSystemConfiguration.get( "file", container, "jclouds.filesystem.basedir" );
        if( basedir == null ) {
            basedir = SystemUtils.IS_OS_WINDOWS ? "c:/" : "/";
        }

        this.basedir = Paths.get( basedir );
    }

    @Override
    public CompletableFuture<Boolean> blobExistsAsync( CloudURI path ) throws CloudException {
        return CompletableFuture.completedFuture( getPath( path ).toFile().exists() );
    }

    private Path getPath( CloudURI path ) {
        return Paths.get( basedir.toString(), path.container, path.path ).normalize();
    }

    @Override
    public CompletableFuture<Boolean> containerExistsAsync( CloudURI path ) throws CloudException {
        return CompletableFuture.completedFuture( Files.isDirectory( getPath( path ) ) );
    }

    @Override
    public CompletableFuture<Void> deleteBlobAsync( CloudURI path ) {
        try {
            Files.delete( getPath( path ) );

            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public CompletableFuture<Void> deleteContainerAsync( CloudURI path ) {

        Path fsPath = getPath( path );
        if( !Files.isDirectory( fsPath ) ) {
            return CompletableFuture.failedFuture( new CloudException( "Not a directory" ) );
        }

        try {
            oap.io.Files.delete( fsPath );
        } catch( Exception e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }

        return CompletableFuture.completedFuture( null );
    }

    @Override
    public CompletableFuture<Boolean> createContainerAsync( CloudURI path ) {
        return CompletableFuture.completedFuture( false );
    }

    @Override
    public CompletableFuture<Boolean> deleteContainerIfEmptyAsync( CloudURI path ) {
        return CompletableFuture.completedFuture( false );
    }

    @Override
    public CompletableFuture<FileSystem.StorageItem> getMetadataAsync( CloudURI path ) {
        try {
            Path fsPath = getPath( path );
            return CompletableFuture.completedFuture( new FileSystem.StorageItemImpl(
                fsPath.toString(),
                "",
                fsPath.toUri(),
                new DateTime( Files.getLastModifiedTime( fsPath ).toMillis(), DateTimeZone.UTC ),
                Files.size( fsPath ),
                Files.isDirectory( fsPath ) ? "application/x-directory" : "" ) );
        } catch( NoSuchFileException e ) {
            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public CompletableFuture<Void> downloadFileAsync( CloudURI source, Path destination ) {
        try {
            Files.copy( getPath( source ), destination );

            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public CompletableFuture<Void> copyAsync( CloudURI source, CloudURI destination ) {
        Preconditions.checkArgument( source.scheme.equals( destination.scheme ) );

        try {
            Files.copy( getPath( source ), getPath( destination ) );

            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public CompletableFuture<? extends InputStream> getInputStreamAsync( CloudURI path ) {
        try {
            Path fsPath = getPath( path );

            log.debug( "getInputStreamAsync '{}' -> '{}'", path, fsPath );

            return CompletableFuture.completedFuture( Files.newInputStream( fsPath ) );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public OutputStream getOutputStream( CloudURI path, Map<String, String> tags ) throws CloudException {
        try {
            Path fsPath = getPath( path );

            log.debug( "getOutputStream '{}' -> '{}'", path, fsPath );

            oap.io.Files.ensureFile( fsPath );
            return Files.newOutputStream( fsPath );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public CompletableFuture<Void> uploadAsync( CloudURI destination, BlobData blobData ) throws CloudException {
        try {
            Path destinationFs = getPath( destination );

            log.debug( "uploadAsync '{}' -> '{}'", destination, destinationFs );

            switch( blobData.content ) {
                case InputStream inputStream -> IoStreams.write( destinationFs, IoStreams.Encoding.PLAIN, inputStream );
                case String str -> IoStreams.write( destinationFs, IoStreams.Encoding.PLAIN, str );
                case byte[] bytes -> IoStreams.write( destinationFs, IoStreams.Encoding.PLAIN, new ByteArrayInputStream( bytes ) );
                case ByteBuffer byteBuffer -> IoStreams.write( destinationFs, IoStreams.Encoding.PLAIN, new ByteArrayInputStream( byteBuffer.array() ) );
                case File file -> {
                    oap.io.Files.ensureFile( destinationFs );
                    Files.copy( file.toPath(), destinationFs );
                }
                case Path path -> {
                    oap.io.Files.ensureFile( destinationFs );
                    Files.copy( path, destinationFs );
                }
                case null, default -> throw new CloudException( "Unknown content type " + blobData.content.getClass() );
            }
            return CompletableFuture.completedFuture( null );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public CompletableFuture<PageSet<? extends FileSystem.StorageItem>> listAsync( CloudURI path, ListOptions listOptions ) {
        try {
            Path filePath = getPath( path );

            ArrayList<FileSystem.StorageItemImpl> list = new ArrayList<>();

            Stream<Path> pathStream = Files.walk( filePath )
                .filter( p -> !Files.isDirectory( p ) )
                .sorted();

            if( listOptions.continuationToken != null ) {
                int skip = Integer.parseInt( listOptions.continuationToken );
                pathStream = pathStream.skip( skip );
            }

            if( listOptions.maxKeys != null ) {
                pathStream = pathStream.limit( listOptions.maxKeys );
            }

            List<Path> files = pathStream.toList();

            for( Path file : files ) {
                try {
                    list.add( new FileSystem.StorageItemImpl(
                        FilenameUtils.separatorsToUnix( basedir.relativize( file ).toString() ),
                        "",
                        file.toUri(),
                        new DateTime( Files.getLastModifiedTime( file ).toMillis(), DateTimeZone.UTC ),
                        Files.size( file ),
                        "" ) );
                } catch( IOException e ) {
                    return CompletableFuture.failedFuture( new CloudException( e ) );
                }

            }

            return CompletableFuture.completedFuture( new PageSet<>( listOptions.maxKeys != null ? listOptions.maxKeys.toString() : null, list ) );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public void close() {
    }
}
