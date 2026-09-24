package oap.notification;

import oap.json.Binder;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Higher-level, typed wrapper around a {@link NotificationTransport} — wraps a plain {@link Serializable}
 * payload into a {@link Notification} on send, and hands the transport's config through to {@code subscribe}.
 */
public class NotificationService {
    public final NotificationTransport notificationTransport;

    public NotificationService( NotificationTransport notificationTransport ) {
        this.notificationTransport = notificationTransport;
    }

    /**
     * @see #sendNotification(String, Qos, boolean, Serializable)
     */
    public <TMessage extends Serializable> void sendNotification( String topic, Qos qos, TMessage message ) throws NotificationException {
        sendNotification( topic, qos, false, message );
    }

    /**
     * Wraps `message` in a {@link Notification} and publishes it.
     *
     * @see NotificationTransport#publish(String, Qos, boolean, Notification)
     */
    public <TMessage extends Serializable> void sendNotification( String topic, Qos qos, boolean retain, TMessage message ) throws NotificationException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Binder.json.marshal( message, outputStream );
        notificationTransport.publish( topic, qos, retain, new Notification( outputStream.toByteArray() ) );
    }

    /**
     * @see NotificationTransport#subscribe(List, Consumer)
     */
    public void subscribeToTopic( String topic, Consumer<NotificationPublish> notificationConsumer ) {
        notificationTransport.subscribe( topic, notificationConsumer );
    }

    /**
     * @see NotificationTransport#subscribe(List, Consumer)
     */
    public void subscribeToTopic( List<String> topics, Consumer<NotificationPublish> notificationConsumer ) {
        notificationTransport.subscribe( topics, notificationConsumer );
    }

    /**
     * @see NotificationTransport#subscribe(List, boolean, Consumer)
     */
    public void subscribeToTopic( String topic, boolean manualAcknowledgement, Consumer<NotificationPublish> notificationConsumer ) {
        notificationTransport.subscribe( List.of( topic ), manualAcknowledgement, notificationConsumer );
    }

    /**
     * @see NotificationTransport#subscribe(List, boolean, Consumer)
     */
    public void subscribeToTopic( List<String> topics, boolean manualAcknowledgement, Consumer<NotificationPublish> notificationConsumer ) {
        notificationTransport.subscribe( topics, manualAcknowledgement, notificationConsumer );
    }
}
