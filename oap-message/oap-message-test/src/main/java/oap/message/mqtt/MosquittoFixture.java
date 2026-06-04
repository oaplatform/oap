package oap.message.mqtt;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.extern.slf4j.Slf4j;
import oap.testng.AbstractFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class MosquittoFixture extends AbstractFixture<MosquittoFixture> {
    private static final String VERSION = "2.1.2-alpine";
    public final int mqttPort;
    private GenericContainer<?> container;

    public MosquittoFixture() {
        mqttPort = definePort( "MQTT_PORT" );
    }

    @Override
    protected void before() {
        PortBinding portBinding = new PortBinding(
            Ports.Binding.bindPort( mqttPort ),
            new ExposedPort( 1883 ) );

        container = new GenericContainer<>( DockerImageName.parse( "eclipse-mosquitto:" + VERSION ) )
            .withLogConsumer( new Slf4jLogConsumer( log ) )
            .withCreateContainerCmdModifier( cmd -> cmd.getHostConfig().withPortBindings( portBinding ) )
            .withExposedPorts( 1883 );
        container.start();
    }

    @Override
    protected void after() {
        if( container != null ) {
            container.stop();
        }
    }
}
