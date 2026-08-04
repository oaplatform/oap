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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class FileSystemCloudApiLocalFs implements FileSystemCloudApi {

    private final Path basedir;
    private final boolean removeEmptyFolders;

    public FileSystemCloudApiLocalFs( FileSystemConfiguration fileSystemConfiguration, String container ) {
        String basedir = ( String ) fileSystemConfiguration.get( "file", container, "jclouds.filesystem.basedir" );
        if( basedir == null ) {
            basedir = SystemUtils.IS_OS_WINDOWS ? "C:/" : "/";
        }

        this.basedir = Paths.get( basedir );

        Object removeEmptyFolders = fileSystemConfiguration.get( "file", container, "jclouds.filesystem.remove_empty_folders" );
        this.removeEmptyFolders = removeEmptyFolders != null && Boolean.parseBoolean( removeEmptyFolders.toString() );
    }

    @Override
    public boolean blobExists( CloudURI path ) throws CloudException {
        return getPath( path ).toFile().exists();
    }

    protected Path getPath( CloudURI path ) {
        if( path.path.startsWith( "/" ) ) {
            return Paths.get( path.path ).normalize();
        }
        return Paths.get( basedir.toString(), path.container, path.path ).normalize();
    }

    @Override
    public boolean containerExists( CloudURI path ) throws CloudException {
        return Files.isDirectory( getPath( path ) );
    }

    @Override
    public void deleteBlob( CloudURI path ) {
        try {
            Path fsPath = getPath( path );
            Files.delete( fsPath );

            if( removeEmptyFolders ) {
                Path parent = fsPath.getParent();
                while( parent != null && !parent.equals( basedir ) && parent.startsWith( basedir )
                    && oap.io.Files.isDirectoryEmpty( parent ) ) {
                    Files.delete( parent );
                    parent = parent.getParent();
                }
            }
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void deleteContainer( CloudURI path ) {
        Path fsPath = getPath( path );
        if( !Files.isDirectory( fsPath ) ) {
            throw new CloudException( "Not a directory" );
        }

        try {
            oap.io.Files.delete( fsPath );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public boolean createContainer( CloudURI path ) {
        return false;
    }

    @Override
    public boolean deleteContainerIfEmpty( CloudURI path ) {
        return false;
    }

    @Override
    public FileSystem.StorageItem getMetadata( CloudURI path ) {
        try {
            Path fsPath = getPath( path );
            return new FileSystem.StorageItemImpl(
                fsPath.toString(),
                "",
                fsPath.toUri(),
                new DateTime( Files.getLastModifiedTime( fsPath ).toMillis(), DateTimeZone.UTC ),
                Files.size( fsPath ),
                Files.isDirectory( fsPath ) ? "application/x-directory" : "" );
        } catch( NoSuchFileException e ) {
            return null;
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void downloadFile( CloudURI source, Path destination ) {
        try {
            Files.copy( getPath( source ), destination );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void copy( CloudURI source, CloudURI destination ) {
        Preconditions.checkArgument( source.scheme.equals( destination.scheme ) );

        try {
            Files.copy( getPath( source ), getPath( destination ) );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public InputStream getInputStream( CloudURI path ) {
        try {
            Path fsPath = getPath( path );

            log.debug( "getInputStream '{}' -> '{}'", path, fsPath );

            return Files.newInputStream( fsPath );
        } catch( IOException e ) {
            throw new CloudException( e );
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
    public void upload( CloudURI destination, BlobData blobData ) throws CloudException {
        try {
            Path destinationFs = getPath( destination );

            log.debug( "upload '{}' -> '{}'", destination, destinationFs );

            switch( blobData.content ) {
                case InputStream inputStream -> IoStreams.write( destinationFs, IoStreams.Encoding.PLAIN, inputStream );
                case String str -> IoStreams.write( destinationFs, IoStreams.Encoding.PLAIN, str );
                case byte[] bytes ->
                    IoStreams.write( destinationFs, IoStreams.Encoding.PLAIN, new ByteArrayInputStream( bytes ) );
                case ByteBuffer byteBuffer ->
                    IoStreams.write( destinationFs, IoStreams.Encoding.PLAIN, new ByteArrayInputStream( byteBuffer.array() ) );
                case File file -> {
                    oap.io.Files.ensureFile( destinationFs );
                    Files.copy( file.toPath(), destinationFs, StandardCopyOption.REPLACE_EXISTING );
                }
                case Path path -> {
                    oap.io.Files.ensureFile( destinationFs );
                    Files.copy( path, destinationFs, StandardCopyOption.REPLACE_EXISTING );
                }
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
                    throw new CloudException( e );
                }

            }

            return new PageSet<>( listOptions.maxKeys != null ? listOptions.maxKeys.toString() : null, list );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void close() {
    }
}
