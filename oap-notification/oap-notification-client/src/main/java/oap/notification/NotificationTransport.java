package oap.notification;

import java.util.List;
import java.util.function.Consumer;

/**
 * Pluggable pub/sub transport for {@link Notification}s (see {@link NotificationService} for the higher-level
 * API). The only implementation provided is MQTT, via {@code HivemqNotificationTransport} (oap-notification-mqtt).
 */
public interface NotificationTransport {
    /**
     * @param topic         destination topic
     * @param qos           delivery guarantee (see {@link Qos})
     * @param retain        whether the broker should keep this as the topic's last known value and deliver it
     *                      immediately to future subscribers (MQTT "retained message" semantics)
     * @param notification  the message to send
     */
    void publish( String topic, Qos qos, boolean retain, Notification notification ) throws NotificationException;

    /** @see #subscribe(List, Consumer) */
    default void subscribe( String topic, Consumer<NotificationPublish> notificationConsumer ) {
        subscribe( List.of( topic ), notificationConsumer );
    }

    /**
     * Subscribes to `topics`, invoking `notificationConsumer` for each message received. The subscription and
     * callback registration happen asynchronously relative to the caller.
     */
    default void subscribe( List<String> topics, Consumer<NotificationPublish> notificationConsumer ) {
        subscribe(  topics, false, notificationConsumer );
    }

    /**
     * Subscribes to `topics`, invoking `notificationConsumer` for each message received.
     *
     * @param manualAcknowledgement when {@code true}, each delivered message is a {@link NotificationPublishWithAcknowledge}
     *                              and the transport waits for {@link NotificationPublishWithAcknowledge#acknowledge()}
     *                              before considering it delivered (e.g. MQTT manual acknowledgement — an
     *                              un-acknowledged message is redelivered for QoS above {@code AT_MOST_ONCE});
     *                              when {@code false}, every message is delivered as a plain {@link NotificationPublish}
     *                              and is considered acknowledged as soon as the transport hands it to `notificationConsumer`
     */
    void subscribe( List<String> topics, boolean manualAcknowledgement, Consumer<NotificationPublish> notificationConsumer );
}
