package oap.notification;

import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@ToString
public class NotificationPublish extends Notification {
    @Serial
    private static final long serialVersionUID = 8509736862218143643L;

    public final String topic;

    public NotificationPublish( String topic, Notification notification ) {
        super( notification );

        this.topic = topic;
    }

    public NotificationPublish( String topic, String sender, Serializable message ) {
        super( sender, message );

        this.topic = topic;
    }
}
