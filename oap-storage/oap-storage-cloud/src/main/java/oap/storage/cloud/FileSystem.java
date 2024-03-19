package oap.storage.cloud;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;
import oap.io.Resources;
import oap.util.Maps;
import org.apache.commons.io.FilenameUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class FileSystem {
    private static final HashMap<String, Tags> tagSupport = new HashMap<>();

    static {
        try {
            List<URL> urls = Resources.urls( FileSystem.class, "/META-INF/tags.properties" );

            for( var url : urls ) {
                log.debug( "url {}", url );
                try( var is = url.openStream() ) {
                    Properties properties = new Properties();
                    properties.load( is );

                    for( String scheme : properties.stringPropertyNames() ) {
                        tagSupport.put( scheme, ( Tags ) Class.forName( properties.getProperty( scheme ) ).getConstructor().newInstance() );
                    }
                }
            }

            log.info( "tags {}", Maps.toList( tagSupport, ( k, v ) -> k + " : " + v.getClass() ) );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    private final FileSystemConfiguration fileSystemConfiguration;

    public FileSystem( FileSystemConfiguration fileSystemConfiguration ) {
        this.fileSystemConfiguration = fileSystemConfiguration;
    }

    /**
     * The core api does not allow passing custom headers. This is a workaround.
     */
    private static void putBlob( BlobStore blobStore, Blob blob, CloudURI blobURI, Map<String, String> tags ) throws CloudException {
        if( !tags.isEmpty() ) {
            Tags putObject = tagSupport.get( blobURI.scheme );
            if( putObject != null ) {
                putObject.putBlob( blobStore, blob, blobURI, tags );
            } else {
                throw new CloudException( "tags are only supported for " + tagSupport.keySet() );
            }
        } else {
            blobStore.putBlob( blobURI.container, blob );
        }
    }

    public CloudInputStream getInputStream( String path ) {
        return getInputStream( new CloudURI( path ) );
    }

    public CloudInputStream getInputStream( CloudURI path ) {
        log.debug( "getInputStream {}", path );

        BlobStoreContext context = null;
        try {
            context = getContext( path );

            BlobStore blobStore = context.getBlobStore();

            Blob blob = blobStore.getBlob( path.container, path.path );

            if( blob == null ) {
                throw new CloudException( new FileNotFoundException( path.toString() ) );
            }

            return new CloudInputStream( blob.getPayload().openStream(), blob.getMetadata().getUserMetadata(), context );
        } catch( Exception e ) {
            throw new CloudException( e );
        } finally {
            Closeables.close( context );
        }
    }

    public void uploadFile( String destination, Path path ) {
        uploadFile( destination, path, Map.of() );
    }

    public void uploadFile( String destination, Path path, Map<String, String> userMetadata ) {
        uploadFile( destination, path, userMetadata, Map.of() );
    }

    public void uploadFile( String destination, Path path, Map<String, String> userMetadata, Map<String, String> tags ) {
        log.debug( "uploadFile {} path {} userMetadata {} tags {}", destination, path, userMetadata, tags );

        CloudURI destinationURI = new CloudURI( destination );

        try( BlobStoreContext sourceContext = getContext( destinationURI ) ) {
            BlobStore blobStore = sourceContext.getBlobStore();

            Blob blob = blobStore
                .blobBuilder( destinationURI.path )
                .userMetadata( userMetadata )
                .payload( path.toFile() )
                .build();

            putBlob( blobStore, blob, destinationURI, tags );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    public void copy( String source, String destination ) {
        copy( source, destination, Map.of(), Map.of() );
    }

    public void copy( CloudURI source, CloudURI destination ) {
        copy( source, destination, Map.of(), Map.of() );
    }

    public void copy( String source, String destination, Map<String, String> userMetadata ) {
        copy( source, destination, userMetadata, Map.of() );
    }

    public void copy( CloudURI source, CloudURI destination, Map<String, String> userMetadata ) {
        copy( source, destination, userMetadata, Map.of() );
    }

    public void copy( String source, String destination, Map<String, String> userMetadata, Map<String, String> tags ) {
        CloudURI sourceURI = new CloudURI( source );
        CloudURI destinationURI = new CloudURI( destination );

        copy( sourceURI, destinationURI, userMetadata, tags );
    }

    public void copy( CloudURI source, CloudURI destination, Map<String, String> userMetadata, Map<String, String> tags ) {
        log.debug( "copy {} tp {} userMetadata {} tags {}", source, destination, userMetadata, tags );


        BlobStoreContext spurceContext = null;
        BlobStoreContext destinationContext = null;
        try {
            spurceContext = getContext( source );
            destinationContext = getContext( destination );

            BlobStore sourceBlobStore = spurceContext.getBlobStore();
            BlobStore destinationBlobStore = destinationContext.getBlobStore();

            Blob sourceBlob = sourceBlobStore.getBlob( source.container, source.path );
            Long contentLength = sourceBlob.getMetadata().getContentMetadata().getContentLength();

            try( InputStream is = sourceBlob.getPayload().openStream() ) {
                Blob destinationBlob = destinationBlobStore
                    .blobBuilder( destination.path )
                    .userMetadata( userMetadata )
                    .payload( is )
                    .contentLength( contentLength )
                    .build();

                putBlob( destinationBlobStore, destinationBlob, destination, tags );
            }

        } catch( Exception e ) {
            throw new CloudException( e );
        } finally {
            Closeables.close( spurceContext );
            Closeables.close( destinationContext );
        }
    }

    public void deleteBlob( String path ) {
        CloudURI pathURI = new CloudURI( path );

        deleteBlob( pathURI );
    }

    public void deleteBlob( CloudURI path ) {
        log.debug( "deleteBlob {}", path );

        try( BlobStoreContext context = getContext( path ) ) {
            BlobStore blobStore = context.getBlobStore();
            blobStore.removeBlob( path.container, path.path );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    public boolean deleteContainerIfEmpty( String path ) {
        CloudURI pathURI = new CloudURI( path );

        return deleteContainerIfEmpty( pathURI );
    }

    public boolean deleteContainerIfEmpty( CloudURI path ) {
        log.debug( "deleteContainerIfEmpty {}", path );

        try( BlobStoreContext context = getContext( path ) ) {
            BlobStore blobStore = context.getBlobStore();
            return blobStore.deleteContainerIfEmpty( path.container );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    public void deleteContainer( String path ) {
        CloudURI pathURI = new CloudURI( path );

        deleteContainer( pathURI );
    }

    public void deleteContainer( CloudURI path ) {
        log.debug( "deleteContainer {}", path );

        try( BlobStoreContext context = getContext( path ) ) {
            BlobStore blobStore = context.getBlobStore();
            blobStore.deleteContainer( path.container );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    public boolean blobExists( String path ) {
        CloudURI pathURI = new CloudURI( path );

        return blobExists( pathURI );
    }

    public boolean blobExists( CloudURI path ) {
        log.debug( "blobExists {}", path );

        try( BlobStoreContext context = getContext( path ) ) {
            BlobStore blobStore = context.getBlobStore();
            return blobStore.blobExists( path.container, path.path );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    public boolean containerExists( String path ) {
        CloudURI pathURI = new CloudURI( path );

        return containerExists( pathURI );
    }

    public boolean containerExists( CloudURI path ) {
        log.debug( "containerExists {}", path );

        try( BlobStoreContext context = getContext( path ) ) {
            BlobStore blobStore = context.getBlobStore();
            return blobStore.containerExists( path.container );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    public boolean createContainer( CloudURI path ) {
        log.debug( "createContainer {}", path );

        try( BlobStoreContext context = getContext( path ) ) {
            BlobStore blobStore = context.getBlobStore();
            return blobStore.createContainerInLocation( null, path.container );
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    public CloudURI getDefaultURL( String path ) {
        log.debug( "getDefaultURL {}", path );

        return new CloudURI( fileSystemConfiguration.getDefaultScheme(),
            fileSystemConfiguration.getDefaultContainer(),
            FilenameUtils.separatorsToUnix( path )
        );
    }

    private BlobStoreContext getContext( CloudURI uri ) {
        Map<String, Object> containerConfiguration = fileSystemConfiguration.get( uri.scheme, uri.container );

        Properties overrides = new Properties();
        overrides.putAll( containerConfiguration );

        ContextBuilder contextBuilder = ContextBuilder
            .newBuilder( uri.getProvider() )
            .modules( List.of( new SLF4JLoggingModule() ) )
            .overrides( overrides );

        return contextBuilder.buildView( BlobStoreContext.class );
    }

    public CloudURI toLocalFilePath( Path path ) {
        log.debug( "toLocalFilePath {}", path );

        var baseDir = fileSystemConfiguration.getOrThrow( "file", "", "jclouds.filesystem.basedir" );

        return new CloudURI( FilenameUtils.separatorsToUnix( "file://" + Paths.get( baseDir.toString() ).relativize( path ) ) );
    }

    public File toFile( CloudURI cloudURI ) {
        Preconditions.checkArgument( "file".equals( cloudURI.scheme ) );

        String basedir = ( String ) fileSystemConfiguration.getOrThrow( "file", cloudURI.container, "jclouds.filesystem.basedir" );

        return Paths.get( basedir ).resolve( cloudURI.container ).resolve( cloudURI.path ).toFile();
    }
}
