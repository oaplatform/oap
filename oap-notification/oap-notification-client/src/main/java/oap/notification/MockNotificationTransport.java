package oap.notification;

import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class MockNotificationTransport implements NotificationTransport {
    @Override
    public void publish( String topic, Qos qos, Notification notification ) {
        log.trace( "publish topic {} qos {} notification {}", topic, qos, Binder.json.marshal( notification ) );
    }

    @Override
    public void subscribe( List<String> topics, Consumer<NotificationPublish> notificationConsumer ) {
        log.trace( "subscribe topics {}", topics );
    }
}
