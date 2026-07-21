package oap.notification;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

public class NotificationService {
    public final NotificationTransport notificationTransport;

    public NotificationService( NotificationTransport notificationTransport ) {
        this.notificationTransport = notificationTransport;
    }

    public <TMessage extends Serializable> void sendNotification( String topic, Qos qos, TMessage message ) throws NotificationException {
        notificationTransport.publish( topic, qos, new Notification( message ) );
    }

    public void subscribeToTopic( String topic, Consumer<NotificationPublish> notificationConsumer ) {
        notificationTransport.subscribe( topic, notificationConsumer );
    }

    public void subscribeToTopic( List<String> topics, Consumer<NotificationPublish> notificationConsumer ) {
        notificationTransport.subscribe( topics, notificationConsumer );
    }
}
