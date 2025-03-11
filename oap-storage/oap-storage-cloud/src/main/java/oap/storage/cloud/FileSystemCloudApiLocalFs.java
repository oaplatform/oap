package oap.storage.cloud;

import com.google.common.base.Preconditions;
import oap.io.IoStreams;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FileSystemCloudApiLocalFs implements FileSystemCloudApi {
    public FileSystemCloudApiLocalFs( FileSystemConfiguration fileSystemConfiguration, String container ) {
    }

    @Override
    public CompletableFuture<Boolean> blobExistsAsync( CloudURI path ) throws CloudException {
        return CompletableFuture.completedFuture( getPath( path ).toFile().exists() );
    }

    private Path getPath( CloudURI path ) {
        return Paths.get( "/" + path.path );
    }

    @Override
    public CompletableFuture<Boolean> containerExistsAsync( CloudURI path ) throws CloudException {
        return CompletableFuture.completedFuture( true );
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
        return CompletableFuture.completedFuture( new FileSystem.StorageItem() {
            @Override
            public String getName() {
                return getPath( path ).toString();
            }

            @Override
            public URI getUri() {
                return getPath( path ).toUri();
            }

            @Override
            public String getETag() {
                return "";
            }

            @Override
            public DateTime getLastModified() {
                try {
                    return new DateTime( Files.getLastModifiedTime( getPath( path ) ).toMillis(), DateTimeZone.UTC );
                } catch( IOException e ) {
                    throw new CloudException( e );
                }
            }

            @Override
            public Long getSize() {
                try {
                    return Files.size( getPath( path ) );
                } catch( IOException e ) {
                    throw new CloudException( e );
                }
            }
        } );
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
            return CompletableFuture.completedFuture( Files.newInputStream( getPath( path ) ) );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public OutputStream getOutputStream( CloudURI path, Map<String, String> tags ) throws CloudException {
        try {
            return Files.newOutputStream( getPath( path ) );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public CompletableFuture<Void> uploadAsync( CloudURI destination, BlobData blobData ) throws CloudException {
        try {
            switch( blobData.content ) {
                case InputStream inputStream -> IoStreams.write( getPath( destination ), IoStreams.Encoding.PLAIN, inputStream );
                case String str -> IoStreams.write( getPath( destination ), IoStreams.Encoding.PLAIN, str );
                case byte[] bytes -> IoStreams.write( getPath( destination ), IoStreams.Encoding.PLAIN, new ByteArrayInputStream( bytes ) );
                case ByteBuffer byteBuffer -> IoStreams.write( getPath( destination ), IoStreams.Encoding.PLAIN, new ByteArrayInputStream( byteBuffer.array() ) );
                case File file -> Files.copy( file.toPath(), getPath( destination ) );
                case Path path -> Files.copy( path, getPath( destination ) );
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

            return CompletableFuture.completedFuture( new PageSet<>( null, Files.walk( filePath )
                .filter( p -> !Files.isDirectory( p ) )
                .map( p -> new FileSystem.StorageItem() {
                    @Override
                    public String getName() {
                        return p.toString();
                    }

                    @Override
                    public URI getUri() {
                        return p.toUri();
                    }

                    @Override
                    public String getETag() {
                        return "";
                    }

                    @Override
                    public DateTime getLastModified() {
                        try {
                            return new DateTime( Files.getLastModifiedTime( p ).toMillis(), DateTimeZone.UTC );
                        } catch( IOException e ) {
                            throw new CloudException( e );
                        }
                    }

                    @Override
                    public Long getSize() {
                        try {
                            return Files.size( p );
                        } catch( IOException e ) {
                            throw new CloudException( e );
                        }
                    }
                } )
                .toList() ) );
        } catch( IOException e ) {
            return CompletableFuture.failedFuture( new CloudException( e ) );
        }
    }

    @Override
    public void close() {
    }
}
