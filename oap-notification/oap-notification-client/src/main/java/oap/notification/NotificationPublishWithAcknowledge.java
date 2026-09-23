package oap.notification;

import lombok.ToString;

import java.io.Serializable;

/**
 * A {@link NotificationPublish} delivered under manual acknowledgement (see
 * {@link NotificationTransport#subscribe(java.util.List, boolean, java.util.function.Consumer)}) — the
 * consumer must call {@link #acknowledge()} once it has finished processing the message, or the transport
 * (e.g. the MQTT broker, for QoS > {@code AT_MOST_ONCE}) will redeliver it.
 */
@ToString
public class NotificationPublishWithAcknowledge extends NotificationPublish {
    private Runnable acknowledge;

    public NotificationPublishWithAcknowledge( String topic, Notification notification, Runnable acknowledge ) {
        super( topic, notification );
        this.acknowledge = acknowledge;
    }

    /**
     * No acknowledge callback attached — {@link #acknowledge()} throws {@link NullPointerException} on an
     * instance built this way. Only useful for constructing a value to compare/inspect, not one to acknowledge.
     */
    public NotificationPublishWithAcknowledge( String topic, Serializable message ) {
        super( topic, message );
    }

    /** Confirms processing of this message to the transport; must be called exactly once. */
    @Override
    public void acknowledge() {
        acknowledge.run();
    }
}
