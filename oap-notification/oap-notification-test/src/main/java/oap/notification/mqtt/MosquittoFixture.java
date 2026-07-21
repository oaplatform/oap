package oap.notification.mqtt;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.testng.AbstractFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class MosquittoFixture extends AbstractFixture<MosquittoFixture> {
    private static final String VERSION = "2.1.2-alpine";
    @Getter
    private final int port;
    private GenericContainer container;

    public MosquittoFixture() {
        port = definePort( "MQTT_PORT" );
    }

    @Override
    protected void before() {
        super.before();

        PortBinding portBinding = new PortBinding(
            Ports.Binding.bindPort( port ),
            new ExposedPort( 1883 ) );

        container = new GenericContainer<>( DockerImageName.parse( "eclipse-mosquitto:" + VERSION ) )
            .withExposedPorts( 1883 )
            .withCreateContainerCmdModifier( cmd -> cmd.getHostConfig().withPortBindings( portBinding ) )
            .withLogConsumer( new Slf4jLogConsumer( log ) );
        container.start();
    }

    @Override
    protected void after() {
        if( container != null ) {
            container.stop();
        }

        super.after();
    }
}
