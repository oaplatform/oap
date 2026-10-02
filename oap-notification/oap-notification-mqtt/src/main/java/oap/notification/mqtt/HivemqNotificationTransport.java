package oap.notification.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.exceptions.MqttSessionExpiredException;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.application.annotation.Start;
import oap.application.annotation.Stop;
import oap.json.Binder;
import oap.notification.Notification;
import oap.notification.NotificationException;
import oap.notification.NotificationPublish;
import oap.notification.NotificationPublishWithAcknowledge;
import oap.notification.NotificationTransport;
import oap.notification.Qos;
import oap.util.Dates;
import oap.util.Lists;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static dev.khbd.interp4j.core.Interpolations.s;

@Slf4j
public class HivemqNotificationTransport implements NotificationTransport, AutoCloseable {
    @Getter
    private final String identifier;
    private final String host;
    private final int port;
    public long connectTimeout = Dates.s( 10 );
    public long publishTimeout = Dates.s( 1 );
    /** Seconds (not millis, unlike the timeouts above — the MQTT5 CONNECT property is wire-encoded in seconds).
     *  MQTT5 defaults this to 0 (session dies the instant the connection drops). Only raise this if the broker
     *  is known to preserve sessions across reconnects — if it doesn't (e.g. a restart, or a non-sticky
     *  load-balanced/clustered broker with no shared session state), a nonzero value makes every automatic
     *  reconnect throw {@code MqttSessionExpiredException} (broker's CONNACK won't have the session-present flag
     *  the client now expects), which is worse than the plain-0 behavior this guards against. The actual
     *  safety net for a publish landing in a disconnect/reconnect gap is the retry in {@link #publish}, which
     *  works regardless of this value. */
    public long sessionExpiryInterval = 0;
    private Mqtt5AsyncClient client;

    /**
     * @param identifier MQTT client identifier; {@code %rnd%} is replaced with 5 random letters
     *                   so multiple instances (e.g. across replicas) don't collide on the broker
     */
    public HivemqNotificationTransport( String identifier, String host, int port ) {
        this.identifier = identifier.replace( "%rnd%", RandomStringUtils.insecure().nextAlphabetic( 5 ) );
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
            .sessionExpiryInterval( sessionExpiryInterval )
            .send()
            .orTimeout( connectTimeout, TimeUnit.MILLISECONDS )
            .join();

        log.debug( "[{}] Connected to MQTT server at {}:{} response {}", identifier, host, port, ack );
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
    public void publish( String topic, Qos qos, boolean retain, Notification notification ) throws NotificationException {
        publish( topic, qos, retain, notification, true );
    }

    private void publish( String topic, Qos qos, boolean retain, Notification notification, boolean retryOnSessionExpired ) throws NotificationException {
        try {
            log.trace( "[{}] publish topic {} qos {} retain {} notification {}", identifier, topic, qos, retain, Binder.json.marshal( notification ) );

            Mqtt5PublishResult result = client
                .publishWith()
                .topic( topic )
                .qos( convertQos( qos ) )
                .retain( retain )
                .payload( Binder.json.marshal( notification ).getBytes() )
                .send()
                .orTimeout( publishTimeout, TimeUnit.MILLISECONDS )
                .join();

            log.trace( "[{}] publish topic {} qos {} result {}", identifier, topic, qos, result );
        } catch( CompletionException e ) {
            if( retryOnSessionExpired && e.getCause() instanceof MqttSessionExpiredException ) {
                log.trace( "[{}] MQTT session expired mid-publish ({}), waiting for automatic reconnect and retrying once in background",
                    identifier, e.getCause().getMessage() );
                Thread.ofVirtual().name( s( "notification-retry-${identifier}" ) ).start( () -> {
                    try {
                        awaitReconnect();
                        publish( topic, qos, retain, notification, false );
                    } catch( NotificationException retryException ) {
                        log.error( "[{}] retry after session-expired publish failed: {}", identifier, retryException.getMessage(), retryException );
                    }
                } );
                return;
            }
            throw new NotificationException( e.getCause() );
        }
    }

    private void awaitReconnect() {
        long deadline = System.currentTimeMillis() + connectTimeout;
        while( !client.getState().isConnected() && System.currentTimeMillis() < deadline ) {
            try {
                Thread.sleep( 100 );
            } catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public void subscribe( List<String> topics, boolean manualAcknowledgement, Consumer<NotificationPublish> notificationConsumer ) {
        Mqtt5SubAck ack = client
            .subscribeWith()
            .addSubscriptions( Lists.map( topics, topic -> Mqtt5Subscription.builder().topicFilter( topic ).build() ) )
            .callback( mqtt5Publish -> {
                byte[] payloadAsBytes = mqtt5Publish.getPayloadAsBytes();

                log.trace( "[{}] topic {} payload {}", identifier, mqtt5Publish.getTopic(),
                    payloadAsBytes.length > 0 ? new String( payloadAsBytes ) : "<EMPTY>" );

                Notification notification = Binder.json.unmarshal( Notification.class, payloadAsBytes );
                String topic = mqtt5Publish.getTopic().toString();
                Qos qos = convertQos( mqtt5Publish.getQos() );
                boolean retain = mqtt5Publish.isRetain();
                notificationConsumer.accept( manualAcknowledgement
                    ? new NotificationPublishWithAcknowledge( topic, qos, retain, notification, mqtt5Publish::acknowledge )
                    : new NotificationPublish( topic, qos, retain, notification ) );
            } )
            .manualAcknowledgement( manualAcknowledgement )
            .send()
            .orTimeout( publishTimeout, TimeUnit.MILLISECONDS )
            .join();

        log.trace( "[{}] publish topics {} manualAcknowledgement {} result {}", identifier, topics, manualAcknowledgement, ack );
    }

    private MqttQos convertQos( Qos qos ) {
        return switch( qos ) {
            case AT_MOST_ONCE -> MqttQos.AT_MOST_ONCE;
            case EXACTLY_ONCE -> MqttQos.EXACTLY_ONCE;
            case AT_LEAST_ONCE -> MqttQos.AT_LEAST_ONCE;
        };
    }

    private Qos convertQos( MqttQos qos ) {
        return switch( qos ) {
            case AT_MOST_ONCE -> Qos.AT_MOST_ONCE;
            case EXACTLY_ONCE -> Qos.EXACTLY_ONCE;
            case AT_LEAST_ONCE -> Qos.AT_LEAST_ONCE;
        };
    }
}
