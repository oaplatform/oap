package oap.storage.cloud;

import com.adobe.testing.s3mock.S3MockApplication;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.io.IoStreams;
import oap.io.IoStreams.Encoding;
import oap.io.content.ContentReader;
import oap.testng.AbstractFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Maps;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * variables:
 * <ul>
 *     <li>PORT</li>
 * </ul>
 */
@Slf4j
public class S3MockFixture extends AbstractFixture<S3MockFixture> {
    @Getter
    private final int port;
    private final TestDirectoryFixture testDirectoryFixture;
    private boolean debug = false;
    private String initialBuckets = "";
    private S3MockApplication s3MockApplication;

    public S3MockFixture( TestDirectoryFixture testDirectoryFixture ) {
        this.testDirectoryFixture = testDirectoryFixture;

        port = definePort( "PORT" );
    }

    @Override
    protected void before() {
        super.before();

        s3MockApplication = S3MockApplication.start( new LinkedHashMap<>( Map.of(
            S3MockApplication.PROP_HTTP_PORT, port,
            S3MockApplication.PROP_INITIAL_BUCKETS, initialBuckets,
            S3MockApplication.PROP_SILENT, !debug,
            S3MockApplication.PROP_ROOT_DIRECTORY, testDirectoryFixture.testDirectory().toString()
        ) ) );
    }

    public S3MockFixture withDebug( boolean debug ) {
        this.debug = debug;

        return this;
    }

    public S3MockFixture withInitialBuckets( String... bucketNames ) {
        initialBuckets = String.join( ",", bucketNames );

        return this;
    }

    @Override
    protected void after() {
        if( s3MockApplication != null ) {
            s3MockApplication.stop();
        }
        super.after();
    }

    /**
     * !!! S3Mock bug!!!! no urldecode is used for the header
     */
    public Map<String, String> readTags( String container, String path ) {
        final AmazonS3 s3 = getS3();

        return Lists.toLinkedHashMap( s3.getObjectTagging( new GetObjectTaggingRequest( container, path ) ).getTagSet(),
            t -> URLDecoder.decode( t.getKey(), UTF_8 ), t -> URLDecoder.decode( t.getValue(), UTF_8 ) );
    }

    public void uploadFile( String container, String name, Path file ) {
        uploadFile( container, name, file, Map.of() );
    }

    public void uploadFile( String container, String name, Path file, Map<String, String> tags ) {
        final AmazonS3 s3 = getS3();

        PutObjectRequest putObjectRequest = new PutObjectRequest( container, name, file.toFile() )
            .withTagging( new ObjectTagging( Maps.toList( tags, Tag::new ) ) );

        s3.putObject( putObjectRequest );
    }

    public <T> T readFile( String container, String name, ContentReader<T> contentReader ) {
        AmazonS3 s3 = getS3();

        try( S3Object s3Object = s3.getObject( container, name );
             S3ObjectInputStream objectContent = s3Object.getObjectContent() ) {

            return contentReader.read( IoStreams.in( objectContent, Encoding.from( name ) ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    private AmazonS3 getS3() {
        return AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration( new AwsClientBuilder.EndpointConfiguration( "http://localhost:" + port, "us-east-1" ) )
            .withPathStyleAccessEnabled( true )
            .build();
    }
}
