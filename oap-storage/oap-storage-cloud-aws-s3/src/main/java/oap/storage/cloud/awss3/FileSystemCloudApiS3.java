package oap.storage.cloud.awss3;

import lombok.extern.slf4j.Slf4j;
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
import org.apache.commons.lang3.NotImplementedException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class FileSystemCloudApiS3 implements FileSystemCloudApi {
    private static final int PART_SIZE = 5 * 1024 * 1024;

    private final S3Client s3Client;

    public FileSystemCloudApiS3( FileSystemConfiguration fileSystemConfiguration, String bucketName ) {
        S3ClientBuilder builder = S3Client.builder()
            .httpClientBuilder( Apache5HttpClient.builder() );

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
    public boolean blobExists( CloudURI path ) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket( path.container ).key( path.path ).build();

        try {
            s3Client.headObject( headObjectRequest );
            return true;
        } catch( NoSuchBucketException | NoSuchKeyException e ) {
            return false;
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public boolean containerExists( CloudURI path ) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
            .bucket( path.container )
            .build();

        try {
            s3Client.headBucket( headBucketRequest );
            return true;
        } catch( NoSuchBucketException e ) {
            return false;
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void deleteBlob( CloudURI path ) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket( path.container ).key( path.path ).build();

        try {
            s3Client.deleteObject( deleteRequest );
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void deleteContainer( CloudURI path ) {
        try {
            ListObjectsV2Response listResponse = s3Client.listObjectsV2( ListObjectsV2Request.builder().bucket( path.container ).build() );

            ArrayList<ObjectIdentifier> objectsToDelete = new ArrayList<>();
            for( S3Object s3Object : listResponse.contents() ) {
                objectsToDelete.add( ObjectIdentifier.builder().key( s3Object.key() ).build() );
            }

            if( !objectsToDelete.isEmpty() ) {
                DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket( path.container )
                    .delete( Delete.builder().objects( objectsToDelete ).build() )
                    .build();

                s3Client.deleteObjects( deleteObjectsRequest );
            }

            s3Client.deleteBucket( DeleteBucketRequest.builder().bucket( path.container ).build() );
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public boolean createContainer( CloudURI path ) {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket( path.container ).build();

        try {
            s3Client.createBucket( createBucketRequest );
            return true;
        } catch( BucketAlreadyExistsException e ) {
            return false;
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public boolean deleteContainerIfEmpty( CloudURI path ) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket( path.container ).build();

        try {
            s3Client.deleteBucket( deleteBucketRequest );
            return true;
        } catch( SdkException e ) {
            if( e.getMessage() != null && e.getMessage().contains( "The bucket you tried to delete is not empty" ) ) {
                return false;
            }
            throw new CloudException( e );
        }
    }

    @Override
    public FileSystem.StorageItem getMetadata( CloudURI path ) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket( path.container ).key( path.path ).build();

        try {
            HeadObjectResponse headObjectResponse = s3Client.headObject( headObjectRequest );

            return new FileSystem.StorageItem() {
                @Override
                public String getName() {
                    return path.toString();
                }

                @Override
                public URI getUri() {
                    try {
                        return s3Client.utilities().getUrl( b -> b.bucket( path.container ).key( path.path ).build() ).toURI();
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

                @Override
                public String getContentType() {
                    return headObjectResponse.contentType();
                }
            };
        } catch( NoSuchKeyException e ) {
            return null;
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void downloadFile( CloudURI source, Path destination ) throws CloudException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket( source.container ).key( source.path ).build();

        try {
            oap.io.Files.ensureFile( destination );

            try( ResponseInputStream<GetObjectResponse> in = s3Client.getObject( getObjectRequest ) ) {
                Files.copy( in, destination, StandardCopyOption.REPLACE_EXISTING );
            }
        } catch( IOException e ) {
            throw new CloudException( e );
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void copy( CloudURI source, CloudURI destination ) {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
            .sourceBucket( source.container )
            .destinationBucket( destination.container )
            .sourceKey( source.path )
            .destinationKey( destination.path )
            .build();

        try {
            s3Client.copyObject( copyObjectRequest );
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public InputStream getInputStream( CloudURI path ) {
        try {
            return s3Client.getObject( GetObjectRequest.builder().bucket( path.container ).key( path.path ).build() );
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public OutputStream getOutputStream( CloudURI cloudURI, Map<String, String> tags ) {
        return new MultipartUploadOutputStream( cloudURI, tags, null );
    }

    @Override
    public void upload( CloudURI cloudURI, BlobData blobData ) {
        try {
            switch( blobData.content ) {
                case InputStream is -> uploadStream( cloudURI, is, blobData.tags, blobData.contentType );
                case String str -> putObject( cloudURI, blobData, RequestBody.fromString( str, UTF_8 ) );
                case byte[] bytes -> putObject( cloudURI, blobData, RequestBody.fromBytes( bytes ) );
                case ByteBuffer byteBuffer -> putObject( cloudURI, blobData, RequestBody.fromByteBuffer( byteBuffer ) );
                case File file -> putObject( cloudURI, blobData, RequestBody.fromFile( file ) );
                case Path path -> putObject( cloudURI, blobData, RequestBody.fromFile( path ) );
                case null -> putObject( cloudURI, blobData, RequestBody.empty() ); // "folder"
                default -> throw new CloudException( "Unknown content type " + blobData.content.getClass() );
            }
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    private void putObject( CloudURI cloudURI, BlobData blobData, RequestBody requestBody ) {
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

        s3Client.putObject( putObjectRequestBuilder.build(), requestBody );
    }

    private void uploadStream( CloudURI cloudURI, InputStream inputStream, Map<String, String> tags, String contentType ) {
        try( OutputStream out = new MultipartUploadOutputStream( cloudURI, tags, contentType ) ) {
            inputStream.transferTo( out );
        } catch( IOException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public PageSet<? extends FileSystem.StorageItem> list( CloudURI path, ListOptions listOptions ) {
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

        try {
            ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2( builder.build() );
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

                @Override
                public String getContentType() {
                    throw new NotImplementedException();
                }
            } ) );
        } catch( SdkException e ) {
            throw new CloudException( e );
        }
    }

    @Override
    public void close() {
        Closeables.close( s3Client );
    }

    private final class MultipartUploadOutputStream extends OutputStream {
        private final CloudURI cloudURI;
        private final Map<String, String> tags;
        private final String contentType;
        private final List<CompletedPart> completedParts = new ArrayList<>();
        private final byte[] buffer = new byte[ PART_SIZE ];

        private int position = 0;
        private int partNumber = 1;
        private String uploadId;
        private boolean closed = false;

        MultipartUploadOutputStream( CloudURI cloudURI, Map<String, String> tags, String contentType ) {
            this.cloudURI = cloudURI;
            this.tags = tags;
            this.contentType = contentType;
        }

        @Override
        public void write( int b ) {
            if( position == buffer.length ) {
                flushPart();
            }
            buffer[ position++ ] = ( byte ) b;
        }

        @Override
        public void write( byte[] b, int off, int len ) {
            int written = 0;
            while( written < len ) {
                if( position == buffer.length ) {
                    flushPart();
                }
                int toCopy = Math.min( len - written, buffer.length - position );
                System.arraycopy( b, off + written, buffer, position, toCopy );
                position += toCopy;
                written += toCopy;
            }
        }

        private void flushPart() {
            try {
                if( uploadId == null ) {
                    uploadId = createMultipartUpload();
                }
                uploadCurrentBuffer();
                position = 0;
            } catch( SdkException e ) {
                abortQuietly();
                throw new CloudException( e );
            }
        }

        private String createMultipartUpload() {
            CreateMultipartUploadRequest.Builder builder = CreateMultipartUploadRequest.builder()
                .bucket( cloudURI.container )
                .key( cloudURI.path )
                .tagging( getTagging( tags ) );
            if( contentType != null ) {
                builder.contentType( contentType );
            }
            return s3Client.createMultipartUpload( builder.build() ).uploadId();
        }

        private void uploadCurrentBuffer() {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket( cloudURI.container )
                .key( cloudURI.path )
                .uploadId( uploadId )
                .partNumber( partNumber )
                .build();

            UploadPartResponse response = s3Client.uploadPart( uploadPartRequest, RequestBody.fromByteBuffer( ByteBuffer.wrap( buffer, 0, position ) ) );

            completedParts.add( CompletedPart.builder().partNumber( partNumber ).eTag( response.eTag() ).build() );
            partNumber++;
        }

        private void abortQuietly() {
            if( uploadId == null ) {
                return;
            }
            try {
                s3Client.abortMultipartUpload( AbortMultipartUploadRequest.builder()
                    .bucket( cloudURI.container )
                    .key( cloudURI.path )
                    .uploadId( uploadId )
                    .build() );
            } catch( SdkException e ) {
                log.error( e.getMessage(), e );
            }
        }

        @Override
        public void close() {
            if( closed ) {
                return;
            }
            closed = true;

            try {
                if( uploadId == null ) {
                    PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
                        .bucket( cloudURI.container )
                        .key( cloudURI.path )
                        .tagging( getTagging( tags ) );
                    if( contentType != null ) {
                        putObjectRequestBuilder.contentType( contentType );
                    }
                    s3Client.putObject( putObjectRequestBuilder.build(), RequestBody.fromByteBuffer( ByteBuffer.wrap( buffer, 0, position ) ) );
                    return;
                }

                if( position > 0 ) {
                    uploadCurrentBuffer();
                }

                s3Client.completeMultipartUpload( CompleteMultipartUploadRequest.builder()
                    .bucket( cloudURI.container )
                    .key( cloudURI.path )
                    .uploadId( uploadId )
                    .multipartUpload( CompletedMultipartUpload.builder().parts( completedParts ).build() )
                    .build() );
            } catch( SdkException e ) {
                abortQuietly();
                throw new CloudException( e );
            }
        }
    }
}
