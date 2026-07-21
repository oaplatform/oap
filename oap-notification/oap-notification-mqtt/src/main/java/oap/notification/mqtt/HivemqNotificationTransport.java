package oap.notification.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import lombok.extern.slf4j.Slf4j;
import oap.application.annotation.Start;
import oap.application.annotation.Stop;
import oap.json.Binder;
import oap.notification.Notification;
import oap.notification.NotificationException;
import oap.notification.NotificationPublish;
import oap.notification.NotificationTransport;
import oap.notification.Qos;
import oap.util.Dates;
import oap.util.Lists;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class HivemqNotificationTransport implements NotificationTransport, AutoCloseable {
    private final String identifier;
    private final String host;
    private final int port;
    public long connectTimeout = Dates.s( 10 );
    public long publishTimeout = Dates.s( 1 );
    private Mqtt5AsyncClient client;

    public HivemqNotificationTransport( String identifier, String host, int port ) {
        this.identifier = identifier;
        this.host = host;
        this.port = port;
    }

    @Start
    public void start() {
        client = MqttClient
            .builder()
            .useMqttVersion5()
            .identifier( identifier )
            .serverHost( host )
            .serverPort( port )

            .automaticReconnect()
            .applyAutomaticReconnect()

            .buildAsync();

        Mqtt5ConnAck ack = client
            .connectWith()
            .send()
            .orTimeout( connectTimeout, TimeUnit.MILLISECONDS )
            .join();

        log.debug( "Connected to MQTT server at {}:{} response {}", host, port, ack );
    }

    @Stop
    public void close() {
        if( client != null && client.getState().isConnectedOrReconnect() ) {
            try {
                client
                    .disconnectWith()
                    .send()
                    .orTimeout( connectTimeout, TimeUnit.MILLISECONDS )
                    .join();
            } catch( CompletionException e ) {
                log.error( e.getCause().getMessage() );
            }
        }
    }

    @Override
    public void publish( String topic, Qos qos, Notification notification ) throws NotificationException {
        try {
            log.trace( "publish topic {} qos {} notification {}", topic, qos, Binder.json.marshal( notification ) );

            Mqtt5PublishResult result = client
                .publishWith()
                .topic( topic )
                .qos( convertQos( qos ) )
                .payload( Binder.json.marshal( notification ).getBytes() )
                .send()
                .orTimeout( publishTimeout, TimeUnit.MILLISECONDS )
                .join();

            log.trace( "publish topic {} qos {} result {}", topic, qos, result );
        } catch( CompletionException e ) {
            throw new NotificationException( e.getCause() );
        }
    }

    @Override
    public void subscribe( List<String> topics, Consumer<NotificationPublish> notificationConsumer ) {
        Mqtt5SubAck ack = client
            .subscribeWith()
            .addSubscriptions( Lists.map( topics, topic -> Mqtt5Subscription.builder().topicFilter( topic ).build() ) )
            .callback( mqtt5Publish -> {
                byte[] payloadAsBytes = mqtt5Publish.getPayloadAsBytes();

                log.trace( "topic {} payload {}", mqtt5Publish.getTopic(),
                    payloadAsBytes.length > 0 ? new String( payloadAsBytes ) : "<EMPTY>" );

                notificationConsumer.accept( new NotificationPublish( mqtt5Publish.getTopic().toString(), Binder.json.unmarshal( Notification.class, payloadAsBytes ) ) );
            } )
            .send()
            .orTimeout( publishTimeout, TimeUnit.MILLISECONDS )
            .join();

        log.trace( "publish topics {} result {}", topics, ack );
    }

    private MqttQos convertQos( Qos qos ) {
        return switch( qos ) {
            case AT_MOST_ONCE -> MqttQos.AT_MOST_ONCE;
            case EXACTLY_ONCE -> MqttQos.EXACTLY_ONCE;
            case AT_LEAST_ONCE -> MqttQos.AT_LEAST_ONCE;
        };
    }
}
