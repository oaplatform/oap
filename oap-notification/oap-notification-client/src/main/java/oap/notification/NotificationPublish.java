package oap.notification;

import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * A {@link Notification} as delivered to a subscriber, tagged with the topic it arrived on — the payload
 * a {@link NotificationTransport}'s {@code subscribe} callback hands to the consumer.
 */
@ToString
public class NotificationPublish extends Notification {
    @Serial
    private static final long serialVersionUID = 8509736862218143643L;

    /** The topic this notification was published/received on. */
    public final String topic;

    public NotificationPublish( String topic, Notification notification ) {
        super( notification );

        this.topic = topic;
    }

    public NotificationPublish( String topic, Serializable message ) {
        super( message );

        this.topic = topic;
    }

    /**
     * Confirms processing of this message to the transport. Only meaningful for a message delivered under
     * manual acknowledgement (see {@link NotificationTransport#subscribe(java.util.List, boolean, java.util.function.Consumer)})
     * — a plain {@code NotificationPublish} was already considered delivered as soon as the transport handed
     * it off, so there is nothing to acknowledge. Overridden by {@link NotificationPublishWithAcknowledge}.
     */
    public void acknowledge() {
        throw new NotificationException( "acknowledge() is not supported: " + topic
            + " was delivered without manual acknowledgement (see NotificationTransport#subscribe with manualAcknowledgement=true)" );
    }
}
