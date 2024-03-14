package oap.storage.cloud.awss3;

import oap.storage.cloud.CloudException;
import oap.storage.cloud.CloudURI;
import oap.storage.cloud.Tags;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.blobstore.S3BlobStore;
import org.jclouds.s3.blobstore.functions.BlobToObject;
import org.jclouds.s3.options.PutObjectOptions;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Collectors;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

public class AwsS3Tags implements Tags {
    private static final Field blob2ObjectField;
    private static final Field syncField;

    static {
        try {
            syncField = S3BlobStore.class.getDeclaredField( "sync" );
            syncField.setAccessible( true );

            blob2ObjectField = S3BlobStore.class.getDeclaredField( "blob2Object" );
            blob2ObjectField.setAccessible( true );
        } catch( NoSuchFieldException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void putBlob( BlobStore blobStore, Blob blob, CloudURI blobURI, Map<String, String> tags ) throws CloudException {
        try {
            S3Client s3Client = ( S3Client ) syncField.get( blobStore );

            BlobToObject blobToObject = ( BlobToObject ) blob2ObjectField.get( blobStore );

            String headerValue = tags.entrySet()
                .stream()
                .map( entry -> encode( entry.getKey(), UTF_8 ) + "=" + encode( entry.getValue(), UTF_8 ) )
                .collect( Collectors.joining( "&" ) );

            s3Client.putObject( blobURI.container, blobToObject.apply( blob ),
                new PutObjectOptionsWithHeaders().withHeader( "x-amz-tagging", headerValue ) );
        } catch( IllegalAccessException e ) {
            throw new CloudException( e );
        }

    }

    private static class PutObjectOptionsWithHeaders extends PutObjectOptions {
        public PutObjectOptionsWithHeaders withHeader( String name, String value ) {
            headers.put( name, value );

            return this;
        }
    }
}
