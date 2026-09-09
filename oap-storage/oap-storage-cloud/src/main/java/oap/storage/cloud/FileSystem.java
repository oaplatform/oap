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
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static dev.khbd.interp4j.core.Interpolations.s;

@Slf4j
public class FileSystem implements AutoCloseable {
    private static final HashMap<String, Class<? extends FileSystemCloudApi>> providers = new HashMap<>();

    static {
        try {
            List<URL> urls = Resources.urls( FileSystem.class, "/cloud-service.properties" );

            for( URL url : urls ) {
                log.debug( "url {}", url );
                try( InputStream is = url.openStream() ) {
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
    private final Cache<String, FileSystemCloudApi> apis = CacheBuilder
        .newBuilder()
        .removalListener( rl -> Closeables.close( ( FileSystemCloudApi ) rl.getValue() ) )
        .build();

    public FileSystem( FileSystemConfiguration fileSystemConfiguration ) {
        this.fileSystemConfiguration = fileSystemConfiguration;
    }

    private String resolveScheme( String configurationId ) {
        String scheme = fileSystemConfiguration.getScheme( configurationId );
        if( scheme != null ) return scheme;
        if( providers.containsKey( configurationId ) ) return configurationId;
        throw new CloudException( "fs: cannot resolve configurationId '" + configurationId + "' to any known scheme" );
    }

    private FileSystemCloudApi getCloudApi( CloudURI cloudURI ) throws CloudException {
        String scheme = resolveScheme( cloudURI.configurationId );
        Class<? extends FileSystemCloudApi> impl = providers.get( scheme );
        if( impl == null ) {
            throw new CloudException( s( "Unknown provider for the scheme ${scheme}" ) );
        }

        String cacheKey = s( "${scheme}://${cloudURI.configurationId}" );

        try {
            return apis.get( cacheKey,
                () -> {
                    try {
                        return impl.getConstructor( FileSystemConfiguration.class, String.class ).newInstance( fileSystemConfiguration, cloudURI.configurationId );
                    } catch( Exception e ) {
                        throw new CloudException( "Invlid provider for the scheme " + scheme, e );
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

    /**
     * Derives {@code path} from a legacy {@code scheme://container/path} URI, per scheme's addressing shape
     * (e.g. for {@code smb} the leading {@code share} segment is stripped off, since it's part of the
     * container, not the path). Used by {@link #resolve(String, String)}.
     */
    private String parsePath( URI u, String scheme ) throws CloudException {
        String host = u.getHost();
        String uriPath = FilenameUtils.separatorsToUnix( u.getPath() );
        if( uriPath.startsWith( "/" ) ) uriPath = uriPath.substring( 1 );

        if( "ftp".equals( scheme ) || "ftps".equals( scheme ) ) {
            if( host == null || host.isEmpty() ) {
                throw new CloudException( s( "fs.${scheme}: container (ftp server host[:port]) is required in the URI, e.g. ${scheme}://host:port/path" ) );
            }
            return uriPath;
        } else if( "smb".equals( scheme ) ) {
            if( host == null || host.isEmpty() ) {
                throw new CloudException( "fs.smb: container (smb server host[:port]) is required in the URI, e.g. smb://host:port/share/path" );
            }

            int slashIdx = uriPath.indexOf( '/' );
            String share = slashIdx >= 0 ? uriPath.substring( 0, slashIdx ) : uriPath;
            if( share.isEmpty() ) {
                throw new CloudException( "fs.smb: share is required in the URI, e.g. smb://host:port/share/path" );
            }
            return slashIdx >= 0 ? uriPath.substring( slashIdx + 1 ) : "";
        } else {
            if( host == null || host.isEmpty() ) {
                throw new CloudException( s( "fs.${scheme}: container is required in the URI, e.g. ${scheme}://container/path" ) );
            }
            return uriPath;
        }
    }

    /**
     * Resolves a legacy {@code scheme://container/path} URI string (ftp/ftps/smb/s3/gcs/ab) to a
     * {@code CloudURI}, tagging the result with the given `configurationId` directly — the URI's
     * container doesn't need to be registered in config at all. {@code fs://} input is already in
     * the new format and passes straight through (its own configurationId is replaced with `configurationId`);
     * {@code file://} has no meaningful container-based mapping and always throws.
     */
    public CloudURI resolve( String configurationId, String uri ) throws CloudException {
        Preconditions.checkNotNull( configurationId, "configurationId is required" );

        try {
            URI u = new URI( uri.endsWith( "://" ) ? uri + "/" : uri );
            String scheme = u.getScheme();

            if( "fs".equals( scheme ) ) {
                return new CloudURI( uri ).withConfigurationId( configurationId );
            }
            if( "file".equals( scheme ) ) {
                throw new CloudException( "fs: file:// URIs cannot be resolved this way; use fs://file/<path> or new CloudURI(\"file\", path)" );
            }

            String path = parsePath( u, scheme );

            return new CloudURI( configurationId, path );
        } catch( URISyntaxException e ) {
            throw new CloudException( e );
        }
    }

    public InputStream getInputStream( CloudURI path ) throws CloudException {
        log.debug( "getInputStream {}", path );

        return getCloudApi( path ).getInputStream( path );
    }

    public OutputStream getOutputStream( CloudURI cloudURI, Map<String, String> tags ) throws CloudException {
        return getCloudApi( cloudURI ).getOutputStream( cloudURI, tags );
    }

    public void downloadFile( String source, Path destination ) throws CloudException {
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

    public void copy( CloudURI source, CloudURI destination, Map<String, String> tags ) throws CloudException {
        log.debug( "copy {} to {} (tags {})", source, destination, tags );

        FileSystemCloudApi destinationCloudApi = getCloudApi( destination );

        if( isLocalFile( source ) ) {
            destinationCloudApi.upload( destination, BlobData.builder().content( toFile( source ).toPath() ).tags( tags ).build() );
            return;
        }

        FileSystemCloudApi sourceCloudApi = getCloudApi( source );

        try( InputStream inputStream = sourceCloudApi.getInputStream( source ) ) {
            destinationCloudApi.upload( destination, BlobData.builder().content( inputStream ).tags( tags ).build() );

        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    public void copy( Path source, CloudURI destination, Map<String, String> tags ) throws CloudException {
        log.debug( "copy {} to {} (tags {})", source, destination, tags );

        getCloudApi( destination ).upload( destination, BlobData.builder().content( source ).tags( tags ).build() );
    }

    public void copy( File source, CloudURI destination, Map<String, String> tags ) throws CloudException {
        log.debug( "copy {} to {} (tags {})", source, destination, tags );

        getCloudApi( destination ).upload( destination, BlobData.builder().content( source ).tags( tags ).build() );
    }

    public PageSet<? extends StorageItem> list( CloudURI path, ListOptions listOptions ) throws CloudException {
        return getCloudApi( path ).list( path, listOptions );
    }

    @Nullable
    public StorageItem getMetadata( CloudURI path ) throws CloudException {
        log.debug( "getMetadata {}", path );

        return getCloudApi( path ).getMetadata( path );
    }

    public void deleteBlob( CloudURI path ) throws CloudException {
        log.debug( "deleteBlob {}", path );

        getCloudApi( path ).deleteBlob( path );
    }

    public boolean deleteContainerIfEmpty( CloudURI path ) {
        log.debug( "deleteContainerIfEmpty {}", path );

        return getCloudApi( path ).deleteContainerIfEmpty( path );
    }

    public void deleteContainer( CloudURI path ) throws CloudException {
        log.debug( "deleteContainer {}", path );

        getCloudApi( path ).deleteContainer( path );
    }

    public boolean blobExists( CloudURI path ) throws CloudException {
        log.debug( "blobExists {}", path );

        return getCloudApi( path ).blobExists( path );
    }

    public boolean containerExists( CloudURI path ) {
        log.debug( "containerExists {}", path );

        return getCloudApi( path ).containerExists( path );
    }

    public boolean createContainer( CloudURI path ) throws CloudException {
        log.debug( "createContainer {}", path );

        return getCloudApi( path ).createContainer( path );
    }

    public CloudURI getDefaultURL( String configurationId, String path ) {
        log.debug( "getDefaultURL configurationId {} path {}", configurationId, path );

        return new CloudURI( configurationId, FilenameUtils.separatorsToUnix( path ) );
    }

    /**
     * Renders a {@code CloudURI} as a "native"-looking URI string using the configurationId's resolved backend
     * scheme and connection, instead of the {@code fs://<configurationId>/<path>} address:
     * <ul>
     *     <li>{@code s3} → {@code s3://<bucket>/<path>}</li>
     *     <li>{@code ftp}/{@code ftps} → {@code ftp(s)://<host[:port]>/<path>}</li>
     *     <li>{@code smb} → {@code smb://<host[:port]/share>/<path>}</li>
     *     <li>{@code file} → {@code file://<basedir>/<path>}</li>
     *     <li>anything else → falls back to {@code fs://<configurationId>/<path>} ({@code cloudURI.toString()})</li>
     * </ul>
     */
    public String toUri( CloudURI cloudURI ) {
        String scheme = resolveScheme( cloudURI.configurationId );

        switch( scheme ) {
            case "s3", "ftp", "ftps", "smb" -> {
                String container = ( String ) fileSystemConfiguration.getOrThrow( scheme, cloudURI.configurationId, "container" );
                Object basedirObj = fileSystemConfiguration.get( scheme, cloudURI.configurationId, "filesystem.basedir" );
                String basedir = "";
                if( basedirObj != null ) {
                    String str = basedirObj.toString();
                    int start = 0, end = str.length();
                    while( start < end && str.charAt( start ) == '/' ) start++;
                    while( end > start && str.charAt( end - 1 ) == '/' ) end--;
                    basedir = str.substring( start, end );
                }
                String resolvedPath = basedir.isEmpty() ? cloudURI.path : s( "${basedir}/${cloudURI.path}" );
                return s( "${scheme}://${container}/${resolvedPath}" );
            }
            case "file" -> {
                String basedir = ( String ) fileSystemConfiguration.get( scheme, cloudURI.configurationId, "filesystem.basedir" );
                if( basedir == null ) {
                    basedir = SystemUtils.IS_OS_WINDOWS ? "C:/" : "/";
                }
                basedir = FilenameUtils.separatorsToUnix( basedir );
                if( basedir.endsWith( "/" ) ) {
                    basedir = basedir.substring( 0, basedir.length() - 1 );
                }
                return s( "file://${basedir}/${cloudURI.path}" );
            }
            default -> {
                return cloudURI.toString();
            }
        }
    }

    public CloudURI toLocalFileURI( String configurationId, Path path ) {
        log.debug( "toLocalFileURI {} {}", configurationId, path );

        String basedir = ( String ) fileSystemConfiguration.get( "file", configurationId, "filesystem.basedir" );

        return new CloudURI( configurationId, basedir != null ? Paths.get( basedir ).relativize( path ).toString()
            : Paths.get( "/" ).relativize( path ).toString() );
    }

    public CloudURI toLocalFileURI( String configurationId, String path ) {
        return toLocalFileURI( configurationId, Paths.get( path ) );
    }

    public boolean isLocalFile( CloudURI cloudURI ) {
        return "file".equals( resolveScheme( cloudURI.configurationId ) );
    }

    public File toFile( CloudURI cloudURI ) {
        Preconditions.checkArgument( "file".equals( resolveScheme( cloudURI.configurationId ) ) );

        try( FileSystemCloudApiLocalFs fileSystemCloudApiLocalFs = new FileSystemCloudApiLocalFs( fileSystemConfiguration, cloudURI.configurationId ) ) {
            return fileSystemCloudApiLocalFs.getPath( cloudURI ).toFile();
        }
    }

    public Path toLocalFilePath( String configurationId, String path ) {
        Preconditions.checkArgument( "file".equals( resolveScheme( configurationId ) ) );

        try( FileSystemCloudApiLocalFs fileSystemCloudApiLocalFs = new FileSystemCloudApiLocalFs( fileSystemConfiguration, configurationId ) ) {
            return fileSystemCloudApiLocalFs.getPath( new CloudURI( configurationId, path ) );
        }
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

        String getContentType();
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
        private final DateTime lastModified;
        private final Long size;
        private final String contentType;
    }
}
