package oap.storage.cloud.awss3;

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import oap.concurrent.Executors;
import oap.concurrent.ThreadPoolExecutor;
import oap.io.Closeables;
import oap.storage.cloud.BlobData;
import oap.storage.cloud.CloudException;
import oap.storage.cloud.CloudURI;
import oap.storage.cloud.FileSystem;
import oap.storage.cloud.FileSystemCloudApi;
import oap.storage.cloud.FileSystemConfiguration;
import oap.storage.cloud.ListOptions;
import oap.storage.cloud.PageSet;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Throwables;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class FileSystemCloudApiS3 implements FileSystemCloudApi {
    private final S3AsyncClient s3Client;
    private volatile S3TransferManager s3TransferManager;

    public FileSystemCloudApiS3( FileSystemConfiguration fileSystemConfiguration, String bucketName ) {
        S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder();

        Object regionObj = fileSystemConfiguration.get( "s3", bucketName, "jclouds.region" );
        if( regionObj == null ) {
            regionObj = System.getenv( "AWS_REGION" );
        }
        Region region = regionObj != null ? Region.of( regionObj.toString() ) : Region.AWS_GLOBAL;

        Object endpoint = fileSystemConfiguration.get( "s3", bucketName, "jclouds.endpoint" );
        if( endpoint != null ) {
            S3EndpointParams s3EndpointParams = S3EndpointParams.builder().endpoint( endpoint.toString() )
                .region( region )
                .build();
            Endpoint s3Endpoint = new DefaultS3EndpointProvider().resolveEndpoint( s3EndpointParams ).join();
            builder = builder.endpointOverride( s3Endpoint.url() ).forcePathStyle( true );
        }

        Object accessKey = fileSystemConfiguration.get( "s3", bucketName, "jclouds.identity" );
        Object accessSecret = fileSystemConfiguration.get( "s3", bucketName, "jclouds.credential" );

        if( accessKey != null && accessSecret != null ) {
            builder = builder.credentialsProvider( StaticCredentialsProvider.create( AwsBasicCredentials.create( accessKey.toString(), accessSecret.toString() ) ) );
        }

        s3Client = builder
            .region( region )
            .build();
    }

    private static Tagging getTagging( Map<String, String> tags ) {
        Tagging.Builder builder = Tagging.builder();
        if( tags != null ) {
            builder.tagSet( Maps.toList( tags, ( k, v ) -> Tag.builder().key( k ).value( v ).build() ) );
        }
        return builder.build();
    }

    private static DateTime instantToDateTime( Instant obj ) {
        return new DateTime( obj.getEpochSecond() * 1000, DateTimeZone.UTC );
    }

    @Override
    public CompletableFuture<Boolean> blobExistsAsync( CloudURI path ) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket( path.container ).key( path.path ).build();

        return s3Client
            .headObject( headObjectRequest )
            .thenApply( _ -> true )
            .exceptionallyCompose( ex -> {
                if( ex.getCause() instanceof NoSuchBucketException || ex.getCause() instanceof NoSuchKeyException ) {
                    return CompletableFuture.completedFuture( false );
                }

                return CompletableFuture.failedFuture( propagate( ex ) );
            } );
    }

    @Override
    public CompletableFuture<Boolean> containerExistsAsync( CloudURI path ) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
            .bucket( path.container )
            .build();

        return s3Client.headBucket( headBucketRequest )
            .thenApply( _ -> true )
            .exceptionallyCompose( e -> {
                if( e.getCause() instanceof NoSuchBucketException ) {
                    return CompletableFuture.completedFuture( false );
                }

                return CompletableFuture.failedFuture( propagate( e ) );
            } );
    }

    @Override
    public CompletableFuture<Void> deleteBlobAsync( CloudURI path ) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket( path.container ).key( path.path ).build();

        return s3Client.deleteObject( deleteRequest )
            .thenAccept( _ -> {} )
            .exceptionallyCompose( e -> CompletableFuture.failedFuture( propagate( e ) ) );
    }

    @Override
    public CompletableFuture<Void> deleteContainerAsync( CloudURI path ) {
        return s3Client.listObjectsV2( ListObjectsV2Request.builder().bucket( path.container ).build() )
            .thenCompose( listResponse -> {
                List<S3Object> listObjects = listResponse.contents();

                ArrayList<ObjectIdentifier> objectsToDelete = new ArrayList<>();
                for( S3Object s3Object : listObjects ) {
                    objectsToDelete.add( ObjectIdentifier.builder().key( s3Object.key() ).build() );
                }

                DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket( path.container )
                    .delete( Delete.builder().objects( objectsToDelete ).build() )
                    .build();

                return s3Client.deleteObjects( deleteObjectsRequest ).thenCompose( deleteObjectsResponse -> {
                    DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket( path.container ).build();

                    return s3Client.deleteBucket( deleteBucketRequest ).thenAccept( _ -> {} );
                } );
            } )
            .exceptionallyCompose( e -> CompletableFuture.failedFuture( propagate( e ) ) );
    }

    @Override
    public CompletableFuture<Boolean> createContainerAsync( CloudURI path ) {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket( path.container ).build();
        return s3Client.createBucket( createBucketRequest )
            .thenApply( _ -> true )
            .exceptionallyCompose( e -> {
                if( e.getCause() instanceof BucketAlreadyExistsException ) {
                    return CompletableFuture.completedFuture( false );
                }
                return CompletableFuture.failedFuture( propagate( e ) );
            } );
    }

    @Override
    public CompletableFuture<Boolean> deleteContainerIfEmptyAsync( CloudURI path ) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket( path.container ).build();

        return s3Client.deleteBucket( deleteBucketRequest )
            .thenApply( _ -> true )
            .exceptionallyCompose( e -> {
                if( e.getMessage().contains( "The bucket you tried to delete is not empty" ) ) {
                    return CompletableFuture.completedFuture( false );
                }

                return CompletableFuture.failedFuture( propagate( e ) );
            } );
    }

    @Override
    public CompletableFuture<? extends FileSystem.StorageItem> getMetadataAsync( CloudURI path ) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket( path.container ).key( path.path ).build();

        return s3Client.headObject( headObjectRequest )
            .thenApply( headObjectResponse -> {
                return new FileSystem.StorageItem() {
                    @Override
                    public String getName() {
                        return path.toString();
                    }

                    @Override
                    public URI getUri() {
                        try {
                            return s3Client.utilities().getUrl( builder -> builder.bucket( path.container ).key( path.path ).build() ).toURI();
                        } catch( URISyntaxException e ) {
                            throw new CloudException( e );
                        }
                    }

                    @Override
                    public String getETag() {
                        return headObjectResponse.eTag();
                    }

                    @Override
                    public DateTime getLastModified() {
                        return instantToDateTime( headObjectResponse.lastModified() );
                    }

                    @Override
                    public Long getSize() {
                        return headObjectResponse.contentLength();
                    }
                };
            } )
            .exceptionallyCompose( e -> {
                if( e.getCause() instanceof NoSuchKeyException ) {
                    return CompletableFuture.completedFuture( null );
                }

                return CompletableFuture.failedFuture( propagate( e ) );
            } );
    }

    @Override
    public CompletableFuture<Void> downloadFileAsync( CloudURI source, Path destination ) throws CloudException {
        S3TransferManager s3TransferManager = getS3TransferManager();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket( source.container ).key( source.path ).build();
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder().getObjectRequest( getObjectRequest ).destination( destination ).build();
        FileDownload fileDownload = s3TransferManager.downloadFile( downloadFileRequest );
        return fileDownload.completionFuture()
            .thenAccept( future -> {} )
            .exceptionallyCompose( e -> CompletableFuture.failedFuture( propagate( e ) ) );
    }

    private S3TransferManager getS3TransferManager() {
        if( s3TransferManager == null ) {
            synchronized( this ) {
                if( s3TransferManager == null ) {
                    s3TransferManager = S3TransferManager.builder().s3Client( s3Client ).build();
                }
            }
        }
        return s3TransferManager;
    }

    @Override
    public CompletableFuture<Void> copyAsync( CloudURI source, CloudURI destination ) {
        S3TransferManager s3TransferManager = getS3TransferManager();
        CopyObjectRequest build = CopyObjectRequest.builder()
            .sourceBucket( source.container )
            .destinationBucket( destination.container )
            .sourceKey( source.path )
            .destinationKey( destination.path )
            .build();
        Copy copy = s3TransferManager.copy( CopyRequest.builder().copyObjectRequest( build ).build() );
        return copy.completionFuture()
            .thenAccept( _ -> {} )
            .exceptionallyCompose( e -> CompletableFuture.failedFuture( propagate( e ) ) );
    }

    @Override
    public CompletableFuture<? extends InputStream> getInputStreamAsync( CloudURI path ) {
        return s3Client
            .getObject( GetObjectRequest.builder().bucket( path.container ).key( path.path ).build(), AsyncResponseTransformer.toBlockingInputStream() )
            .exceptionallyCompose( e -> CompletableFuture.failedFuture( propagate( e ) ) );
    }

    @Override
    public OutputStream getOutputStream( CloudURI cloudURI, Map<String, String> tags ) {
        ThreadPoolExecutor threadPoolExecutor = null;
        CompletableFuture<CompletedUpload> completedUploadCompletableFuture = null;

        try {
            S3TransferManager s3TransferManager = getS3TransferManager();
            BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream( null );

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket( cloudURI.container ).key( cloudURI.path )
                .tagging( getTagging( tags ) )
                .build();
            UploadRequest uploadRequest = UploadRequest.builder()
                .putObjectRequest( putObjectRequest )
                .requestBody( body )
                .build();

            Upload upload = s3TransferManager.upload( uploadRequest );
            completedUploadCompletableFuture = upload.completionFuture();

            PipedOutputStream pipedOutputStream = new PipedOutputStream();
            PipedInputStream pingedInputStream = new PipedInputStream( pipedOutputStream );

            threadPoolExecutor = Executors.newFixedBlockingThreadPool( 1, new NamedThreadFactory( "fs-write-" + cloudURI ) );
            Future<?> future;
            future = threadPoolExecutor.submit( () -> {
                body.writeInputStream( pingedInputStream );
            } );

            return new CloudOutputStream( s3TransferManager, completedUploadCompletableFuture, pipedOutputStream, future, threadPoolExecutor );
        } catch( Exception e ) {
            log.error( e.getMessage(), e );

            if( completedUploadCompletableFuture != null ) {
                completedUploadCompletableFuture.completeExceptionally( e );
            }
            Closeables.close( threadPoolExecutor );
            Closeables.close( s3TransferManager );
            throw Throwables.propagate( e );
        }
    }

    @Override
    public CompletableFuture<Void> uploadAsync( CloudURI cloudURI, BlobData blobData ) {
        Preconditions.checkNotNull( blobData.content );

        S3TransferManager s3TransferManager = getS3TransferManager();
        AsyncRequestBody body = switch( blobData.content ) {
            case InputStream _ -> AsyncRequestBody.forBlockingInputStream( null );
            case String str -> AsyncRequestBody.fromString( str, UTF_8 );
            case byte[] bytes -> AsyncRequestBody.fromBytes( bytes );
            case ByteBuffer byteBuffer -> AsyncRequestBody.fromByteBuffer( byteBuffer );
            case File file -> AsyncRequestBody.fromFile( file );
            case Path path -> AsyncRequestBody.fromFile( path );
            case null, default -> throw new CloudException( "Unknown content type " + blobData.content.getClass() );
        };


        PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
            .bucket( cloudURI.container )
            .key( cloudURI.path )
            .tagging( getTagging( blobData.tags ) );

        if( blobData.contentType != null ) {
            putObjectRequestBuilder.contentType( blobData.contentType );
        }
        if( blobData.contentLength != null ) {
            putObjectRequestBuilder.contentLength( blobData.contentLength );
        }

        UploadRequest uploadRequest = UploadRequest.builder()
            .requestBody( body )
            .putObjectRequest( putObjectRequestBuilder.build() )
            .build();

        Upload upload = s3TransferManager.upload( uploadRequest );

        if( blobData.content instanceof InputStream is ) {
            ( ( BlockingInputStreamAsyncRequestBody ) body ).writeInputStream( is );
        }

        return upload.completionFuture()
            .thenAccept( _ -> {} )
            .exceptionallyCompose( e -> CompletableFuture.failedFuture( propagate( e ) ) );
    }

    @Override
    public CompletableFuture<? extends PageSet<? extends FileSystem.StorageItem>> listAsync( CloudURI path, ListOptions listOptions ) {
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket( path.container );
        if( !path.path.isEmpty() ) {
            builder.prefix( path.path );
        }
        if( listOptions.continuationToken != null ) {
            builder.continuationToken( listOptions.continuationToken );
        }
        if( listOptions.maxKeys != null ) {
            builder.maxKeys( listOptions.maxKeys );
        }
        return s3Client.listObjectsV2( builder.build() )
            .thenApply( listObjectsV2Response -> {
                log.trace( " response {}", listObjectsV2Response );
                return new PageSet<>( listObjectsV2Response.nextContinuationToken(), Lists.map( listObjectsV2Response.contents(), obj -> new FileSystem.StorageItem() {
                    @Override
                    public String getName() {
                        return obj.key();
                    }

                    @Override
                    public URI getUri() {
                        return s3Client.utilities().parseUri( URI.create( new CloudURI( path.scheme, path.container, obj.key() ).toString() ) ).uri();
                    }

                    @Override
                    public String getETag() {
                        return obj.eTag();
                    }

                    @Override
                    public DateTime getLastModified() {
                        return instantToDateTime( obj.lastModified() );
                    }

                    @Override
                    public Long getSize() {
                        return obj.size();
                    }
                } ) );
            } )
            .exceptionallyCompose( e -> CompletableFuture.failedFuture( propagate( e ) ) );
    }

    @Override
    public void close() {
        Closeables.close( s3TransferManager );
        Closeables.close( s3Client );
    }

    public static class CloudOutputStream extends OutputStream {
        private final S3TransferManager s3TransferManager;
        private final CompletableFuture<CompletedUpload> completedUploadCompletableFuture;
        private final PipedOutputStream pipedOutputStream;
        private final Future<?> future;
        private final ThreadPoolExecutor threadPoolExecutor;

        public CloudOutputStream( S3TransferManager s3TransferManager,
                                  CompletableFuture<CompletedUpload> completedUploadCompletableFuture,
                                  PipedOutputStream pipedOutputStream, Future<?> future, ThreadPoolExecutor threadPoolExecutor ) {
            this.s3TransferManager = s3TransferManager;
            this.completedUploadCompletableFuture = completedUploadCompletableFuture;
            this.pipedOutputStream = pipedOutputStream;
            this.future = future;
            this.threadPoolExecutor = threadPoolExecutor;
        }

        @Override
        public void write( int b ) throws IOException {
            pipedOutputStream.write( b );
        }

        @Override
        public void write( byte[] b ) throws IOException {
            pipedOutputStream.write( b );
        }

        @Override
        public void write( byte[] b, int off, int len ) throws IOException {
            pipedOutputStream.write( b, off, len );
        }

        @Override
        public void flush() throws IOException {
            pipedOutputStream.flush();
        }

        @Override
        public void close() {
            Closeables.close( pipedOutputStream );

            try {
                future.get();
            } catch( InterruptedException | ExecutionException e ) {
                log.error( e.getMessage(), e );
            }
            try {
                completedUploadCompletableFuture.get();
            } catch( InterruptedException | ExecutionException e ) {
                log.error( e.getMessage(), e );
            }
            Closeables.close( threadPoolExecutor );
            Closeables.close( s3TransferManager );
        }
    }
}
