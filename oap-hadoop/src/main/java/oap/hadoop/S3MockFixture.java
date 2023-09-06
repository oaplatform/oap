package oap.hadoop;

import lombok.extern.slf4j.Slf4j;
import oap.testng.AbstractEnvFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class S3MockFixture extends AbstractEnvFixture<S3MockFixture> {
    private static final String S3MOCK_VERSION = "3.1.0";
    private GenericContainer<?> container;
    private boolean debug = false;
    private String initialBuckets = "";

    @Override
    protected void before() {
        super.before();

        container = new GenericContainer<>( DockerImageName.parse( "adobe/s3mock:" + S3MOCK_VERSION ) );
        container
            .withLogConsumer( new Slf4jLogConsumer( log ) )
            .withEnv( "debug", String.valueOf( debug ) )
            .withExposedPorts( 9090 );

        if( !initialBuckets.isEmpty() ) {
            container.withEnv( "initialBuckets", initialBuckets );
        }
        container.start();
    }

    public S3MockFixture withDebug( boolean debug ) {
        this.debug = debug;

        return this;
    }

    public S3MockFixture withInitialBuckets( String... buckets ) {
        initialBuckets = String.join( ",", buckets );

        return this;
    }

    public int getPort() {
        return container.getMappedPort( 9090 );
    }

    @Override
    protected void after() {
        if( container != null ) {
            container.stop();
        }
        super.after();
    }
}
