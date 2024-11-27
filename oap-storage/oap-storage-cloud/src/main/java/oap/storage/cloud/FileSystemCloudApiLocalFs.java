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
import java.util.Map;

public class FileSystemCloudApiLocalFs implements FileSystemCloudApi {
    private final Path root;

    public FileSystemCloudApiLocalFs( FileSystemConfiguration fileSystemConfiguration, String container ) {
        Object baseDir = fileSystemConfiguration.get( "file", "", "jclouds.filesystem.basedir" );
        root = baseDir != null ? Path.of( baseDir.toString() ) : Path.of( "/" );
    }

    @Override
    public boolean blobExists( CloudURI path ) throws CloudException {
        return getPath( path ).toFile().exists();
    }

    private Path getPath( CloudURI path ) {
        return root.resolve( path.container, path.path );
    }

    @Override
    public boolean containerExists( CloudURI path ) throws CloudException {
        return true;
    }

    @Override
    public void deleteBlob( CloudURI path ) throws CloudException {
        try {
            Files.delete( getPath( path ) );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void deleteContainer( CloudURI path ) throws CloudException {

    }

    @Override
    public boolean createContainer( CloudURI path ) throws CloudException {
        return false;
    }

    @Override
    public boolean deleteContainerIfEmpty( CloudURI path ) throws CloudException {
        return false;
    }

    @Override
    public FileSystem.StorageItem getMetadata( CloudURI path ) throws CloudException {
        return new FileSystem.StorageItem() {
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
        };
    }

    @Override
    public void downloadFile( CloudURI source, Path destination ) throws CloudException {
        try {
            Files.copy( getPath( source ), destination );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void copy( CloudURI source, CloudURI destination ) throws CloudException {
        Preconditions.checkArgument( source.scheme.equals( destination.scheme ) );

        try {
            Files.copy( getPath( source ), getPath( destination ) );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public InputStream getInputStream( CloudURI path ) throws CloudException {
        try {
            return Files.newInputStream( getPath( path ) );
        } catch( IOException e ) {
            throw new CloudException( e );
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
    public void upload( CloudURI destination, BlobData blobData ) throws CloudException {
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
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public PageSet<? extends FileSystem.StorageItem> list( CloudURI path, ListOptions listOptions ) {
        try {
            Path filePath = getPath( path );

            return new PageSet<>( null, Files.walk( filePath )
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
                .toList() );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }
}
