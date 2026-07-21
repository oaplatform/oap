package oap.notification;

import java.util.List;
import java.util.function.Consumer;

public interface NotificationTransport {
    void publish( String topic, Qos qos, Notification notification );

    default void subscribe( String topic, Consumer<NotificationPublish> notificationConsumer ) {
        subscribe( List.of( topic ), notificationConsumer );
    }

    void subscribe( List<String> topics, Consumer<NotificationPublish> notificationConsumer );
}
