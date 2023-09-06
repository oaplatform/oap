package oap.hadoop;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.testng.AbstractEnvFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class S3MockFixture extends AbstractEnvFixture<S3MockFixture> {
    private static final String S3MOCK_VERSION = "3.1.0";
    @Getter
    private final int port;
    private GenericContainer<?> container;
    private boolean debug = false;
    private String initialBuckets = "";

    public S3MockFixture() {
        definePort( "S3MOCK_PORT" );
        port = portFor( "S3MOCK_PORT" );
    }

    @Override
    protected void before() {
        super.before();

        var portBinding = new PortBinding(
            Ports.Binding.bindPort( port ),
            new ExposedPort( 9090 ) );

        container = new GenericContainer<>( DockerImageName.parse( "adobe/s3mock:" + S3MOCK_VERSION ) );
        container
            .withLogConsumer( new Slf4jLogConsumer( log ) )
            .withEnv( "debug", String.valueOf( debug ) )
            .withCreateContainerCmdModifier( cmd -> cmd.getHostConfig().withPortBindings( portBinding ) );

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

    @Override
    protected void after() {
        if( container != null ) {
            container.stop();
        }
        super.after();
    }
}
