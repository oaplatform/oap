package oap.storage.state;

import oap.storage.cloud.S3MockFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class CloudStateTest extends Fixtures {
    private static final S3MockFixture s3MockFixture;

    static {
        TestDirectoryFixture testDirectoryFixture = suiteFixture( new TestDirectoryFixture() );
        s3MockFixture = suiteFixture( new S3MockFixture( testDirectoryFixture ) ).withInitialBuckets( "test-bucket" );
    }

    @Test
    public void reload() {
        try( CloudState cloudState = new CloudState( s3MockFixture.getFileSystemConfiguration( "test-bucket" ), "test-bucket" ) ) {
            cloudState.save( List.of( "123".getBytes( UTF_8 ), "fff".getBytes( UTF_8 ) ) );
        }

        ArrayList<String> data = new ArrayList<>();

        try( CloudState cloudState = new CloudState( s3MockFixture.getFileSystemConfiguration( "test-bucket" ), "test-bucket" ) ) {
            cloudState.load( bytes -> data.add( new String( bytes, UTF_8 ) ) );
        }

        assertThat( data ).containsExactly( "123", "fff" );
    }
}
