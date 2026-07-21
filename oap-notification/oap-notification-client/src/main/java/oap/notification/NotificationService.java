package oap.notification;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

public class NotificationService {
    public final String id;
    public final NotificationTransport notificationTransport;

    public NotificationService( String id, NotificationTransport notificationTransport ) {
        this.id = id;
        this.notificationTransport = notificationTransport;
    }

    public <TMessage extends Serializable> void sendNotification( String topic, Qos qos, TMessage message ) {
        notificationTransport.publish( topic, qos, new Notification( id, message ) );
    }

    public void subscribeToTopic( String topic, Consumer<NotificationPublish> notificationConsumer ) {
        notificationTransport.subscribe( topic, notificationConsumer );
    }

    public void subscribeToTopic( List<String> topics, Consumer<NotificationPublish> notificationConsumer ) {
        notificationTransport.subscribe( topics, notificationConsumer );
    }
}
