package oap.message.mqtt;

import com.google.common.base.Preconditions;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.unsuback.Mqtt5UnsubAck;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import oap.application.annotation.Start;

import javax.annotation.Nonnull;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class MqttClient implements AutoCloseable, MqttClientConnectedListener, MqttClientDisconnectedListener {
    public final CopyOnWriteArrayList<MqttClientListener> listeners = new CopyOnWriteArrayList<>();
    private final String name;
    private final String host;
    private final int port;
    private ExecutorService virtualThreadExecutor;
    private Mqtt5Client client;

    public MqttClient( @Nonnull String name, String host, int port ) {
        Preconditions.checkNotNull( name );

        this.name = name;
        this.host = host;
        this.port = port;
    }

    @Start
    public void start() {
        log.info( "MQTT client start name {} host {} port {}", name, host, port );

        virtualThreadExecutor = Executors.newCachedThreadPool();

        client = com.hivemq.client.mqtt.MqttClient.builder()
            .useMqttVersion5()
            .identifier( name )
            .serverHost( host )
            .serverPort( port )
            .automaticReconnect()
            .initialDelay( 200, TimeUnit.MILLISECONDS )
            .maxDelay( 1000, TimeUnit.MILLISECONDS )
            .applyAutomaticReconnect()
            .executorConfig()
            .applicationScheduler( Schedulers.from( Executors.newVirtualThreadPerTaskExecutor() ) )
            .applyExecutorConfig()
            .addConnectedListener( this )
            .addDisconnectedListener( this )
            .build();

        client.toBlocking().connect();
    }

    @Override
    public void close() {
        if( client != null ) {
            client.toBlocking().disconnect();
        }
        if( virtualThreadExecutor != null ) {
            try {
                virtualThreadExecutor.shutdown();
                virtualThreadExecutor.awaitTermination( 1, TimeUnit.MINUTES );
                virtualThreadExecutor.shutdownNow();
            } catch( InterruptedException e ) {
                throw new MqttClientException( e );
            }
        }
    }

    @Override
    public void onConnected( @Nonnull MqttClientConnectedContext context ) {
        listeners.forEach( l -> l.onConnected( name ) );
    }

    @Override
    public void onDisconnected( @Nonnull MqttClientDisconnectedContext context ) {
        listeners.forEach( l -> l.onDisconnected( name ) );
    }

    public void subscribe( String topic, BiConsumer<String, byte[]> cons ) {
        try {
            Mqtt5SubAck mqtt5SubAck = client
                .toAsync()
                .subscribeWith()
                .topicFilter( topic )
                .callback( callback -> {
                    String msgTopic = callback.getTopic().toString();
                    byte[] content = callback.getPayloadAsBytes();

                    cons.accept( msgTopic, content );
                } )
                .send()
                .get( 10, TimeUnit.SECONDS );

            log.info( "subscribe topic {} reason {} userProperties {}", topic, mqtt5SubAck.getReasonString(), mqtt5SubAck.getUserProperties() );
        } catch( InterruptedException | TimeoutException | ExecutionException e ) {
            throw new MqttClientException( e );
        }
    }

    public void unsubscribe( String topicFilter ) {
        try {
            Mqtt5UnsubAck mqtt5UnsubAck = client
                .toAsync()
                .unsubscribeWith()
                .topicFilter( topicFilter )
                .send()
                .get( 10, TimeUnit.SECONDS );

            log.info( "subscribe topic {} reason {} userProperties {}", topicFilter, mqtt5UnsubAck.getReasonString(), mqtt5UnsubAck.getUserProperties() );
        } catch( InterruptedException | TimeoutException | ExecutionException e ) {
            throw new MqttClientException( e );
        }
    }

    public void publish( String topic, byte[] bytes, long expire ) {
        log.trace( "publish topic {} data {}", topic, bytes != null ? new String( bytes, UTF_8 ) : "<NULL-DELETE>" );

        Mqtt5PublishBuilder.Send.Complete<Mqtt5PublishResult> builder = client
            .toBlocking()
            .publishWith()
            .topic( topic )
            .payload( bytes )
            .qos( MqttQos.AT_LEAST_ONCE )
            .retain( true );


        if( expire > 0 ) {
            builder = builder
                .messageExpiryInterval( Math.floorDiv( expire, 1000 ) );
        } else {
            builder = builder.noMessageExpiry();
        }

        Mqtt5PublishResult result = builder.send();


        result.getError().ifPresent( t -> {
            throw new MqttClientException( t );
        } );
    }
}
