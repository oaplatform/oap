package oap.storage.cloud;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;
import oap.io.Resources;
import oap.util.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Slf4j
public class FileSystem implements AutoCloseable {
    private static final HashMap<String, Class<? extends FileSystemCloudApi>> providers = new HashMap<>();

    private static final Cache<String, FileSystemCloudApi> apis = CacheBuilder
        .newBuilder()
        .removalListener( rl -> Closeables.close( ( FileSystemCloudApi ) rl.getValue() ) )
        .build();

    static {
        try {
            List<URL> urls = Resources.urls( FileSystem.class, "/cloud-service.properties" );

            for( var url : urls ) {
                log.debug( "url {}", url );
                try( var is = url.openStream() ) {
                    Properties properties = new Properties();
                    properties.load( is );

                    for( String scheme : properties.stringPropertyNames() ) {
                        providers.put( scheme, ( Class<? extends FileSystemCloudApi> ) Class.forName( properties.getProperty( scheme ) ) );
                    }
                }
            }

            log.info( "tags {}", Maps.toList( providers, ( k, v ) -> k + " : " + v ) );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    public final FileSystemConfiguration fileSystemConfiguration;

    public FileSystem( FileSystemConfiguration fileSystemConfiguration ) {
        this.fileSystemConfiguration = fileSystemConfiguration;
    }

    private FileSystemCloudApi getCloudApi( CloudURI cloudURI ) {
        try {
            return apis.get( cloudURI.scheme,
                () -> providers.get( cloudURI.scheme ).getConstructor( FileSystemConfiguration.class, String.class ).newInstance( fileSystemConfiguration, cloudURI.container ) );
        } catch( ExecutionException e ) {
            throw new CloudException( e.getCause() );
        }
    }

    public InputStream getInputStream( CloudURI path ) {
        log.debug( "getInputStream {}", path );

        return getCloudApi( path ).getInputStream( path );
    }

    public OutputStream getOutputStream( CloudURI cloudURI, Map<String, String> tags ) throws CloudException {
        return getCloudApi( cloudURI ).getOutputStream( cloudURI, tags );
    }

    public void downloadFile( String source, Path destination ) {
        downloadFile( new CloudURI( source ), destination );
    }

    public void downloadFile( CloudURI source, Path destination ) throws CloudException {
        log.debug( "downloadFile {} to {}", source, destination );

        getCloudApi( source ).downloadFile( source, destination );
    }

    public void upload( CloudURI destination, BlobData blobData ) throws CloudException {
        log.debug( "upload byte[] to {} (blobData {})", destination, blobData );

        getCloudApi( destination ).upload( destination, blobData );
    }

    public URI getPublicURI( CloudURI cloudURI ) throws CloudException {
        throw new NotImplementedException();
    }

    public void copy( CloudURI source, CloudURI destination, Map<String, String> tags ) {
        log.debug( "copy {} to {} (tags {})", source, destination, tags );

        FileSystemCloudApi sourceCloudApi = getCloudApi( source );
        FileSystemCloudApi destinationCloudApi = getCloudApi( destination );

        try( InputStream inputStream = sourceCloudApi.getInputStream( source ) ) {
            destinationCloudApi.upload( destination, BlobData.builder().content( inputStream ).tags( tags ).build() );

        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    private DateTime toDateTime( Date date ) {
        if( date == null ) {
            return null;
        }
        return new DateTime( date );
    }

    public PageSet<? extends StorageItem> list( CloudURI path, ListOptions listOptions ) throws CloudException {
        return getCloudApi( path ).list( path, listOptions );
    }

    @Nullable
    public StorageItem getMetadata( CloudURI path ) {
        log.debug( "getMetadata {}", path );

        return getCloudApi( path ).getMetadata( path );
    }

    public void deleteBlob( String path ) {
        CloudURI pathURI = new CloudURI( path );

        deleteBlob( pathURI );
    }

    public void deleteBlob( CloudURI path ) {
        log.debug( "deleteBlob {}", path );

        getCloudApi( path ).deleteBlob( path );
    }

    public boolean deleteContainerIfEmpty( String path ) {
        CloudURI pathURI = new CloudURI( path );

        return deleteContainerIfEmpty( pathURI );
    }

    public boolean deleteContainerIfEmpty( CloudURI path ) {
        log.debug( "deleteContainerIfEmpty {}", path );

        return getCloudApi( path ).deleteContainerIfEmpty( path );
    }

    public void deleteContainer( String path ) {
        CloudURI pathURI = new CloudURI( path );

        deleteContainer( pathURI );
    }

    public void deleteContainer( CloudURI path ) {
        log.debug( "deleteContainer {}", path );

        getCloudApi( path ).deleteContainer( path );
    }

    public boolean blobExists( String path ) {
        CloudURI pathURI = new CloudURI( path );

        return blobExists( pathURI );
    }

    public boolean blobExists( CloudURI path ) {
        log.debug( "blobExists {}", path );

        return getCloudApi( path ).blobExists( path );
    }

    public boolean containerExists( String path ) {
        CloudURI pathURI = new CloudURI( path );

        return containerExists( pathURI );
    }

    public boolean containerExists( CloudURI path ) {
        log.debug( "containerExists {}", path );

        return getCloudApi( path ).containerExists( path );
    }

    public boolean createContainer( String path ) {
        return createContainer( new CloudURI( path ) );
    }

    public boolean createContainer( CloudURI path ) {
        log.debug( "createContainer {}", path );

        return getCloudApi( path ).createContainer( path );
    }

    public CloudURI getDefaultURL( String path ) {
        log.debug( "getDefaultURL {}", path );

        return new CloudURI( fileSystemConfiguration.getDefaultScheme(),
            fileSystemConfiguration.getDefaultContainer(),
            FilenameUtils.separatorsToUnix( path )
        );
    }

    public CloudURI toLocalFilePath( Path path ) {
        log.debug( "toLocalFilePath {}", path );

        Object baseDir = fileSystemConfiguration.getOrThrow( "file", "", "jclouds.filesystem.basedir" );

        return new CloudURI( FilenameUtils.separatorsToUnix( "file://" + Paths.get( baseDir.toString() ).relativize( path ) ) );
    }

    public File toFile( CloudURI cloudURI ) {
        Preconditions.checkArgument( "file".equals( cloudURI.scheme ) );

        String basedir = ( String ) fileSystemConfiguration.getOrThrow( "file", cloudURI.container, "jclouds.filesystem.basedir" );

        return Paths.get( basedir ).resolve( cloudURI.container ).resolve( cloudURI.path ).toFile();
    }

    @Override
    public void close() {
        apis.invalidateAll();
    }

    public interface StorageItem {
        String getName();

        URI getUri();

        String getETag();

        DateTime getLastModified();

        Long getSize();
    }

    @ToString
    @AllArgsConstructor
    @Getter
    public static class StorageItemImpl implements StorageItem, Serializable {
        @Serial
        private static final long serialVersionUID = -6579999488530048887L;

        private final String name;
        private final String eTag;
        private final URI uri;
        private final DateTime creationDate;
        private final DateTime lastModified;
        private final Long size;
    }
}
