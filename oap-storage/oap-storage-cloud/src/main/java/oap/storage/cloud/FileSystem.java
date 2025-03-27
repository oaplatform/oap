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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
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

    private FileSystemCloudApi getCloudApi( CloudURI cloudURI ) throws CloudException {
        try {
            return apis.get( cloudURI.scheme,
                () -> {
                    try {
                        Class<? extends FileSystemCloudApi> impl = providers.get( cloudURI.scheme );

                        if( impl == null ) {
                            throw new CloudException( "Unknown provider for the scheme " + cloudURI.scheme );
                        }

                        return impl.getConstructor( FileSystemConfiguration.class, String.class ).newInstance( fileSystemConfiguration, cloudURI.container );
                    } catch( Exception e ) {
                        throw new CloudException( "Invlid provider for the scheme " + cloudURI.scheme, e );
                    }
                } );
        } catch( ExecutionException e ) {
            if( e.getCause() instanceof CloudException ) {
                throw ( CloudException ) e.getCause();
            } else {
                throw new CloudException( e.getCause() );
            }
        }
    }

    public CompletableFuture<? extends InputStream> getInputStreamAsync( CloudURI path ) {
        log.debug( "getInputStream {}", path );

        return getCloudApi( path ).getInputStreamAsync( path );
    }

    public InputStream getInputStream( CloudURI path ) throws CloudException {
        log.debug( "getInputStream {}", path );

        return getCloudApi( path ).getInputStream( path );
    }

    public OutputStream getOutputStream( CloudURI cloudURI, Map<String, String> tags ) throws CloudException {
        return getCloudApi( cloudURI ).getOutputStream( cloudURI, tags );
    }

    public CompletableFuture<Void> downloadFileAsync( String source, Path destination ) {
        return downloadFileAsync( new CloudURI( source ), destination );
    }

    public CompletableFuture<Void> downloadFileAsync( CloudURI source, Path destination ) {
        log.debug( "downloadFile {} to {}", source, destination );

        return getCloudApi( source ).downloadFileAsync( source, destination );
    }

    public void downloadFile( String source, Path destination ) throws CloudException {
        downloadFile( new CloudURI( source ), destination );
    }

    public void downloadFile( CloudURI source, Path destination ) throws CloudException {
        log.debug( "downloadFile {} to {}", source, destination );

        getCloudApi( source ).downloadFile( source, destination );
    }

    public CompletableFuture<Void> uploadAsync( CloudURI destination, BlobData blobData ) {
        log.debug( "upload byte[] to {} (blobData {})", destination, blobData );

        return getCloudApi( destination ).uploadAsync( destination, blobData );
    }

    public void upload( CloudURI destination, BlobData blobData ) throws CloudException {
        log.debug( "upload byte[] to {} (blobData {})", destination, blobData );

        getCloudApi( destination ).upload( destination, blobData );
    }

    public CompletableFuture<Void> copyAsync( CloudURI source, CloudURI destination, Map<String, String> tags ) {
        log.debug( "copy {} to {} (tags {})", source, destination, tags );

        FileSystemCloudApi sourceCloudApi = getCloudApi( source );
        FileSystemCloudApi destinationCloudApi = getCloudApi( destination );

        return sourceCloudApi.getInputStreamAsync( source )
            .thenCompose( inputStream ->
                destinationCloudApi.uploadAsync( destination, BlobData.builder().content( inputStream ).tags( tags ).build() )
                    .thenAccept( _ -> Closeables.close( inputStream ) ) );
    }

    public void copy( CloudURI source, CloudURI destination, Map<String, String> tags ) throws CloudException {
        log.debug( "copy {} to {} (tags {})", source, destination, tags );

        FileSystemCloudApi sourceCloudApi = getCloudApi( source );
        FileSystemCloudApi destinationCloudApi = getCloudApi( destination );

        try( InputStream inputStream = sourceCloudApi.getInputStream( source ) ) {
            destinationCloudApi.upload( destination, BlobData.builder().content( inputStream ).tags( tags ).build() );

        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    public CompletableFuture<? extends PageSet<? extends StorageItem>> listAsync( CloudURI path, ListOptions listOptions ) {
        return getCloudApi( path ).listAsync( path, listOptions );
    }

    public PageSet<? extends StorageItem> list( CloudURI path, ListOptions listOptions ) throws CloudException {
        return getCloudApi( path ).list( path, listOptions );
    }

    public CompletableFuture<? extends StorageItem> getMetadataAsync( CloudURI path ) {
        log.debug( "getMetadata {}", path );

        return getCloudApi( path ).getMetadataAsync( path );
    }

    @Nullable
    public StorageItem getMetadata( CloudURI path ) throws CloudException {
        log.debug( "getMetadata {}", path );

        return getCloudApi( path ).getMetadata( path );
    }

    public CompletableFuture<Void> deleteBlobAsync( CloudURI path ) {
        return getCloudApi( path ).deleteBlobAsync( path );
    }

    public void deleteBlob( CloudURI path ) throws CloudException {
        log.debug( "deleteBlob {}", path );

        getCloudApi( path ).deleteBlob( path );
    }

    public CompletableFuture<Boolean> deleteContainerIfEmptyAsync( CloudURI path ) {
        return getCloudApi( path ).deleteContainerIfEmptyAsync( path );
    }

    public boolean deleteContainerIfEmpty( CloudURI path ) {
        log.debug( "deleteContainerIfEmpty {}", path );

        return getCloudApi( path ).deleteContainerIfEmpty( path );
    }

    public CompletableFuture<Void> deleteContainerAsync( CloudURI path ) {
        return getCloudApi( path ).deleteContainerAsync( path );
    }

    public void deleteContainer( CloudURI path ) throws CloudException {
        log.debug( "deleteContainer {}", path );

        getCloudApi( path ).deleteContainer( path );
    }

    public CompletableFuture<Boolean> blobExistsAsync( CloudURI path ) {
        log.debug( "blobExists {}", path );

        return getCloudApi( path ).blobExistsAsync( path );
    }

    public boolean blobExists( CloudURI path ) throws CloudException {
        log.debug( "blobExists {}", path );

        return getCloudApi( path ).blobExists( path );
    }

    public CompletableFuture<Boolean> containerExistsAsync( CloudURI path ) {
        log.debug( "containerExists {}", path );

        return getCloudApi( path ).containerExistsAsync( path );
    }

    public boolean containerExists( CloudURI path ) {
        log.debug( "containerExists {}", path );

        return getCloudApi( path ).containerExists( path );
    }

    public CompletableFuture<Boolean> createContainerAsync( CloudURI path ) {
        log.debug( "createContainer {}", path );

        return getCloudApi( path ).createContainerAsync( path );
    }

    public boolean createContainer( CloudURI path ) throws CloudException {
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

        return new CloudURI( "file", null, path.toString() );
    }

    public File toFile( CloudURI cloudURI ) {
        Preconditions.checkArgument( "file".equals( cloudURI.scheme ) );

        return Paths.get( "/" + cloudURI.path ).toFile();
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
