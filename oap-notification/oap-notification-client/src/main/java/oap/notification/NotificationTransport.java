package oap.notification;

import java.util.function.Consumer;

public interface NotificationTransport {
    void publish( String topic, Qos qos, Notification notification );

    void subscribe( String topic, Consumer<Notification> notificationConsumer );
}
