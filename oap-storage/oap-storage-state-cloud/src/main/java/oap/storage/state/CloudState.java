package oap.storage.state;

import lombok.extern.slf4j.Slf4j;
import oap.io.Closeables;
import oap.storage.cloud.BlobData;
import oap.storage.cloud.CloudURI;
import oap.storage.cloud.FileSystem;
import oap.storage.cloud.FileSystemConfiguration;
import oap.storage.cloud.ListOptions;
import oap.storage.cloud.PageSet;
import oap.util.Throwables;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

@Slf4j
public class CloudState implements AutoCloseable {
    public final boolean concurrent;
    public final FileSystem fileSystem;
    public final String container;

    public CloudState( FileSystemConfiguration fileSystemConfiguration, String container ) {
        this( false, fileSystemConfiguration, container );
    }

    public CloudState( boolean concurrent, FileSystemConfiguration fileSystemConfiguration, String container ) {
        this.concurrent = concurrent;

        fileSystem = new FileSystem( fileSystemConfiguration );
        this.container = container;
    }

    public void save( List<byte[]> items ) {
        ArrayList<CompletableFuture<Void>> completableFutures = new ArrayList<>();

        for( byte[] item : items ) {
            String md5 = DigestUtils.md5Hex( item ).toUpperCase();

            CloudURI uri = new CloudURI( fileSystem.fileSystemConfiguration.getDefaultScheme(), container, md5 + ".data" );

            log.info( "uploading " + uri + "..." );

            CompletableFuture<Void> completableFuture = fileSystem.uploadAsync( uri,
                BlobData.builder().content( item ).build() );

            completableFutures.add( completableFuture );
        }

        CompletableFuture.allOf( completableFutures.toArray( new CompletableFuture[0] ) ).join();
    }

    public void load( Consumer<byte[]> found ) {
        try {
            ArrayList<CompletableFuture<Void>> completableFutures = new ArrayList<>();

            PageSet<? extends FileSystem.StorageItem> list;

            do {
                list = fileSystem.list( new CloudURI( fileSystem.fileSystemConfiguration.getDefaultScheme(), container, "/" ), ListOptions.builder().maxKeys( 1000 ).build() );

                for( int i = 0; i < list.size(); i++ ) {
                    FileSystem.StorageItem storageItem = list.get( i );
                    CloudURI path = new CloudURI( fileSystem.fileSystemConfiguration.getDefaultScheme(), container, storageItem.getName() );
                    CompletableFuture<Void> completableFuture = fileSystem
                        .getInputStreamAsync( path )
                        .thenCompose( inputStream -> {
                            log.info( "downloading " + path + "..." );

                            try {
                                byte[] bytes = inputStream.readAllBytes();
                                found.accept( bytes );

                                return fileSystem.deleteBlobAsync( path );
                            } catch( IOException e ) {
                                throw Throwables.propagate( e );
                            }
                        } );

                    completableFutures.add( completableFuture );
                }
            } while( list.nextContinuationToken != null );

            CompletableFuture.allOf( completableFutures.toArray( new CompletableFuture[0] ) ).join();
        } catch( CompletionException e ) {
            throw Throwables.propagate( e.getCause() );
        }
    }

    @Override
    public void close() {
        Closeables.close( fileSystem );
    }
}
