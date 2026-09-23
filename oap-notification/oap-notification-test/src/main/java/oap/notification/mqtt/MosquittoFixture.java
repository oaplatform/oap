package oap.notification.mqtt;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.testng.AbstractFixture;
import oap.util.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.khbd.interp4j.core.Interpolations.s;

@Slf4j
public class MosquittoFixture extends AbstractFixture<MosquittoFixture> {
    private static final String VERSION = "2.1.2-alpine";
    @Getter
    private final int port;
    private final Map<String, List<byte[]>> messages = new ConcurrentHashMap<>();
    private GenericContainer container;
    private Mqtt5AsyncClient client;

    public MosquittoFixture() {
        port = definePort( "MQTT_PORT" );
    }

    @Override
    protected void before() {
        super.before();

        PortBinding portBinding = new PortBinding(
            Ports.Binding.bindPort( port ),
            new ExposedPort( 1883 ) );

        container = new GenericContainer<>( DockerImageName.parse( s( "eclipse-mosquitto:${VERSION}" ) ) )
            .withExposedPorts( 1883 )
            .withCreateContainerCmdModifier( cmd -> cmd.getHostConfig().withPortBindings( portBinding ) )
            .withLogConsumer( new Slf4jLogConsumer( log ) );
        container.start();
    }

    @Override
    protected void after() {
        if( client != null ) {
            try {
                client.disconnectWith().send().join();
            } catch( Exception e ) {
                log.debug( "error disconnecting fixture mqtt client", e );
            }
        }

        if( container != null ) {
            container.stop();
        }

        clean();

        super.after();
    }

    /** Clears every captured message (all topics). */
    public void clean() {
        messages.clear();
    }

    private Mqtt5AsyncClient client() {
        if( client == null ) {
            client = MqttClient.builder()
                .useMqttVersion5()
                .identifier( "mosquitto-fixture-" + RandomStringUtils.insecure().nextAlphabetic( 5 ) )
                .serverHost( "127.0.0.1" )
                .serverPort( port )
                .buildAsync();
            client.connectWith().send().join();
        }
        return client;
    }

    /**
     * Subscribes this fixture's own MQTT client to `topic`, capturing every received payload (available via
     * {@link #receive(String)}/{@link #receive(String, Class)}) — independent of any {@code HivemqNotificationTransport}
     * under test, so tests can assert what the broker actually delivered.
     *
     * @param retain the MQTT5 {@code retainAsPublished} subscription option — whether the broker preserves the
     *               retain flag on messages forwarded to this subscription
     */
    public void subscribe( String topic, MqttQos qos, boolean retain ) {
        client().subscribeWith()
            .topicFilter( topic )
            .qos( qos )
            .retainAsPublished( retain )
            .callback( publish ->
                messages.computeIfAbsent( topic, k -> new CopyOnWriteArrayList<>() ).add( publish.getPayloadAsBytes() ) )
            .send()
            .join();
    }

    /** Raw payloads captured on `topic`, in arrival order. */
    public List<byte[]> receive( String topic ) {
        return messages.getOrDefault( topic, List.of() );
    }

    /** Raw payloads captured on every topic subscribed so far, flattened. */
    public List<byte[]> receive() {
        List<byte[]> all = new ArrayList<>();
        messages.values().forEach( all::addAll );
        return all;
    }

    /** Deserializes every captured message on `topic` to `clazz`; an empty MQTT payload becomes {@code null}. */
    public <T> List<T> receive( String topic, Class<T> clazz ) {
        return Lists.map( receive( topic ), raw -> raw.length == 0 ? null : Binder.json.unmarshal( clazz, raw ) );
    }

    /** Same as {@link #receive(String, Class)}, but across every topic subscribed so far, flattened. */
    public <T> List<T> receive( Class<T> clazz ) {
        return Lists.map( receive(), raw -> raw.length == 0 ? null : Binder.json.unmarshal( clazz, raw ) );
    }
}
