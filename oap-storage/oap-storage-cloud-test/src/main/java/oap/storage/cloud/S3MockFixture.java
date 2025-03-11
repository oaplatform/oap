package oap.storage.cloud;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.io.IoStreams;
import oap.io.IoStreams.Encoding;
import oap.io.content.ContentReader;
import oap.testng.AbstractFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Maps;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * https://github.com/adobe/S3Mock/blob/main/testsupport/testcontainers/src/main/java/com/adobe/testing/s3mock/testcontainers/S3MockContainer.java
 * variables:
 * <ul>
 *     <li>HTTP_PORT</li>
 *     <li>HTTPS_PORT</li>
 * </ul>
 */
@Slf4j
public class S3MockFixture extends AbstractFixture<S3MockFixture> {
    private static final String VERSION = "3.12.0";

    private static final int S3MOCK_DEFAULT_HTTP_PORT = 9090;

    @Getter
    private final int httpPort;
    private final TestDirectoryFixture testDirectoryFixture;
    private final LinkedHashSet<String> initialBuckets = new LinkedHashSet<>();
    private GenericContainer<?> container;
    private LocalStackContainer localStackContainer;

    public S3MockFixture( TestDirectoryFixture testDirectoryFixture ) {
        this.testDirectoryFixture = testDirectoryFixture;

        httpPort = definePort( "HTTP_PORT" );

        addChild( testDirectoryFixture );
    }

    public S3MockFixture() {
        this( new TestDirectoryFixture( "-s3mock" ) );
    }

    @Override
    protected void before() {
        super.before();

        PortBinding portBinding = new PortBinding(
            Ports.Binding.bindPort( httpPort ),
            new ExposedPort( 4566 ) );

        localStackContainer = new LocalStackContainer( DockerImageName.parse( "localstack/localstack:4.0.3" ) )
            .withServices( LocalStackContainer.Service.S3 )
            .withCreateContainerCmdModifier( cmd -> cmd.getHostConfig().withPortBindings( portBinding ) );
        localStackContainer.start();

        final S3Client s3 = getS3();

        for( String bucket : initialBuckets ) {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket( bucket ).build();

            s3.createBucket( createBucketRequest );
        }
    }

    public S3MockFixture withInitialBuckets( String... bucketNames ) {
        initialBuckets.addAll( List.of( bucketNames ) );

        return this;
    }

    @Override
    protected void after() {
        if( localStackContainer != null ) {
            localStackContainer.stop();
        }

        super.after();
    }

    public int getPort() {
        return getHttpPort();
    }

    /**
     * !!! S3Mock bug!!!! no urldecode is used for the header
     */
    public Map<String, String> readTags( String container, String path ) {
        S3Client s3 = getS3();

        return Lists.toLinkedHashMap( s3.getObjectTagging( GetObjectTaggingRequest.builder().bucket( container ).key( path ).build() )
            .tagSet(), k -> URLDecoder.decode( k.key(), UTF_8 ), v -> URLDecoder.decode( v.value(), UTF_8 ) );
    }

    public void uploadFile( String container, String name, Path file ) {
        uploadFile( container, name, file, Map.of() );
    }

    public void uploadFile( String container, String name, Path file, Map<String, String> tags ) {
        final S3Client s3 = getS3();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket( container ).key( name )
            .tagging( Tagging.builder().tagSet( Maps.toList( tags, ( k, v ) -> Tag.builder().key( k ).value( v ).build() ) ).build() )
            .build();

        s3.putObject( putObjectRequest, file );
    }

    public void uploadFile( String container, String name, String content, Map<String, String> tags ) {
        final S3Client s3 = getS3();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket( container ).key( name )
            .tagging( Tagging.builder().tagSet( Maps.toList( tags, ( k, v ) -> Tag.builder().key( k ).value( v ).build() ) ).build() )
            .build();

        s3.putObject( putObjectRequest, RequestBody.fromString( content ) );
    }

    public <T> T readFile( String container, String name, ContentReader<T> contentReader, Encoding encoding ) {
        S3Client s3 = getS3();

        try( ResponseInputStream<GetObjectResponse> s3Object = s3.getObject( GetObjectRequest.builder().bucket( container ).key( name ).build() ) ) {
            return contentReader.read( IoStreams.in( s3Object, encoding ) );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private S3Client getS3() {
        S3EndpointParams s3EndpointParams = S3EndpointParams.builder().endpoint( "http://localhost:" + httpPort ).region( Region.AWS_GLOBAL ).build();
        Endpoint s3Endpoint = new DefaultS3EndpointProvider().resolveEndpoint( s3EndpointParams ).join();

        AwsBasicCredentials credentials = AwsBasicCredentials.create( "accessKeyId", "secretAccessKey" );
        StaticCredentialsProvider provider = StaticCredentialsProvider.create( credentials );

        return S3Client.builder()
            .credentialsProvider( provider )
            .endpointOverride( s3Endpoint.url() )
            .region( Region.US_EAST_1 )
            .forcePathStyle( true )
            .build();
    }

    public void deleteAll() {
        S3Client s3 = getS3();

        ListBucketsResponse buckets = s3.listBuckets();
        for( Bucket bucket : buckets.buckets() ) {
            ListObjectsV2Iterable objectListing = s3.listObjectsV2Paginator( ListObjectsV2Request.builder().bucket( bucket.name() ).build() );

            objectListing.stream().forEach( response -> {
                for( S3Object s3Object : response.contents() ) {
                    log.trace( "delete object {}/{}", bucket.name(), s3Object.key() );
                    s3.deleteObject( DeleteObjectRequest.builder().bucket( bucket.name() ).key( s3Object.key() ).build() );
                }
            } );
        }

    }

    @NotNull
    public FileSystemConfiguration getFileSystemConfiguration( String container ) {
        return new FileSystemConfiguration( Map.of(
            "fs.s3.clouds.identity", "access_key",
            "fs.s3.clouds.credential", "access_secret",
            "fs.s3.clouds.region", Region.AWS_GLOBAL.id(),
            "fs.s3.clouds.s3.virtual-host-buckets", false,
            "fs.s3.clouds.endpoint", "http://localhost:" + getHttpPort(),
            "fs.s3.clouds.headers", "DEBUG",

            "fs.default.clouds.scheme", "s3",
            "fs.default.clouds.container", container
        ) );
    }

}
