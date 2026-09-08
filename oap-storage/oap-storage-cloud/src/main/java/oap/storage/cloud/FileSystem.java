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
    private final Cache<String, FileSystemCloudApi> apis = CacheBuilder
        .newBuilder()
        .removalListener( rl -> Closeables.close( ( FileSystemCloudApi ) rl.getValue() ) )
        .build();

    public FileSystem( FileSystemConfiguration fileSystemConfiguration ) {
        this.fileSystemConfiguration = fileSystemConfiguration;
    }

    private String resolveScheme( String alias ) {
        return fileSystemConfiguration.findScheme( alias )
            .orElseGet( () -> {
                if( providers.containsKey( alias ) ) return alias;
                throw new CloudException( "fs: cannot resolve alias '" + alias + "' to any known scheme" );
            } );
    }

    private FileSystemCloudApi getCloudApi( CloudURI cloudURI ) throws CloudException {
        String scheme = resolveScheme( cloudURI.alias );
        Class<? extends FileSystemCloudApi> impl = providers.get( scheme );
        if( impl == null ) {
            throw new CloudException( "Unknown provider for the scheme " + scheme );
        }

        String cacheKey = scheme + "://" + cloudURI.alias;

        try {
            return apis.get( cacheKey,
                () -> {
                    try {
                        return impl.getConstructor( FileSystemConfiguration.class, String.class ).newInstance( fileSystemConfiguration, cloudURI.alias );
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
     * Resolves a legacy {@code scheme://container/path} URI string (ftp/ftps/smb/s3/gcs/ab) to a
     * {@code CloudURI} by finding the alias registered for that scheme+container, falling back to the
     * default alias when the scheme matches the configured default. {@code fs://} input is already in
     * the new format and passes straight through; {@code file://} has no meaningful container-based
     * mapping and always throws.
     */
    public CloudURI resolve( String uri ) throws CloudException {
        try {
            URI u = new URI( uri.endsWith( "://" ) ? uri + "/" : uri );
            String scheme = u.getScheme();

            if( "fs".equals( scheme ) ) {
                return new CloudURI( uri );
            }
            if( "file".equals( scheme ) ) {
                throw new CloudException( "fs: file:// URIs cannot be resolved to an alias; use fs://file/<path> or new CloudURI(\"file\", path)" );
            }

            String host = u.getHost();
            String uriPath = FilenameUtils.separatorsToUnix( u.getPath() );
            if( uriPath.startsWith( "/" ) ) uriPath = uriPath.substring( 1 );

            String container;
            String path;

            if( "ftp".equals( scheme ) || "ftps".equals( scheme ) ) {
                if( host == null || host.isEmpty() ) {
                    throw new CloudException( "fs." + scheme + ": container (ftp server host[:port]) is required in the URI, e.g. " + scheme + "://host:port/path" );
                }
                int port = u.getPort();
                container = port >= 0 ? host + ":" + port : host;
                path = uriPath;
            } else if( "smb".equals( scheme ) ) {
                if( host == null || host.isEmpty() ) {
                    throw new CloudException( "fs.smb: container (smb server host[:port]) is required in the URI, e.g. smb://host:port/share/path" );
                }
                int port = u.getPort();
                String hostPort = port >= 0 ? host + ":" + port : host;

                int slashIdx = uriPath.indexOf( '/' );
                String share = slashIdx >= 0 ? uriPath.substring( 0, slashIdx ) : uriPath;
                if( share.isEmpty() ) {
                    throw new CloudException( "fs.smb: share is required in the URI, e.g. smb://host:port/share/path" );
                }
                container = hostPort + "/" + share;
                path = slashIdx >= 0 ? uriPath.substring( slashIdx + 1 ) : "";
            } else {
                if( host == null || host.isEmpty() ) {
                    throw new CloudException( "fs." + scheme + ": container is required in the URI, e.g. " + scheme + "://container/path" );
                }
                container = host;
                path = uriPath;
            }

            String resolvedContainer = container;
            String alias = fileSystemConfiguration.findAliasByContainer( scheme, container )
                .orElseThrow( () -> new CloudException( "fs: cannot resolve legacy uri '" + uri + "' to any alias (scheme " + scheme + ", container " + resolvedContainer + ")" ) );

            return new CloudURI( alias, path );
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

    public CloudURI getDefaultURL( String path ) {
        log.debug( "getDefaultURL {}", path );

        return getDefaultURL( fileSystemConfiguration.getDefaultAlias(), path );
    }

    public CloudURI getDefaultURL( String alias, String path ) {
        log.debug( "getDefaultURL alias {} path {}", alias, path );

        return new CloudURI( alias, FilenameUtils.separatorsToUnix( path ) );
    }

    /**
     * Renders a {@code CloudURI} as a "native"-looking URI string using the alias's resolved backend
     * scheme and connection, instead of the {@code fs://<alias>/<path>} address:
     * <ul>
     *     <li>{@code s3} → {@code s3://<bucket>/<path>}</li>
     *     <li>{@code ftp}/{@code ftps} → {@code ftp(s)://<host[:port]>/<path>}</li>
     *     <li>{@code smb} → {@code smb://<host[:port]/share>/<path>}</li>
     *     <li>{@code file} → {@code file://<basedir>/<path>}</li>
     *     <li>anything else → falls back to {@code fs://<alias>/<path>} ({@code cloudURI.toString()})</li>
     * </ul>
     */
    public String toUri( CloudURI cloudURI ) {
        String scheme = resolveScheme( cloudURI.alias );

        switch( scheme ) {
            case "s3", "ftp", "ftps", "smb" -> {
                String container = ( String ) fileSystemConfiguration.getOrThrow( scheme, cloudURI.alias, "container" );
                Object basedirObj = fileSystemConfiguration.get( scheme, cloudURI.alias, "filesystem.basedir" );
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
                String basedir = ( String ) fileSystemConfiguration.get( scheme, cloudURI.alias, "filesystem.basedir" );
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

    public CloudURI toLocalFilePath( String alias, Path path ) {
        log.debug( "toLocalFilePath {} {}", alias, path );

        String basedir = ( String ) fileSystemConfiguration.get( "file", alias, "filesystem.basedir" );

        return new CloudURI( alias, basedir != null ? Paths.get( basedir ).relativize( path ).toString()
            : Paths.get( "/" ).relativize( path ).toString() );
    }

    public boolean isLocalFile( CloudURI cloudURI ) {
        return "file".equals( resolveScheme( cloudURI.alias ) );
    }

    public File toFile( CloudURI cloudURI ) {
        Preconditions.checkArgument( "file".equals( resolveScheme( cloudURI.alias ) ) );

        try( FileSystemCloudApiLocalFs fileSystemCloudApiLocalFs = new FileSystemCloudApiLocalFs( fileSystemConfiguration, cloudURI.alias ) ) {
            return fileSystemCloudApiLocalFs.getPath( cloudURI ).toFile();
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
