package oap.notification;

import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;

import java.util.List;
import java.util.function.Consumer;

/**
 * No-op {@link NotificationTransport} that only logs — never actually delivers anything, `subscribe`'s
 * `notificationConsumer` is never invoked. Useful as a placeholder/default transport in tests or configs
 * where notification delivery isn't under test.
 */
@Slf4j
public class MockNotificationTransport implements NotificationTransport {
    @Override
    public void publish( String topic, Qos qos, boolean retain, Notification notification ) {
        log.trace( "publish topic {} qos {} retain {} notification {}",
            topic, qos, retain, Binder.json.marshal( notification ) );
    }

    @Override
    public void subscribe( List<String> topics, boolean manualAcknowledgement, Consumer<NotificationPublish> notificationConsumer ) {
        log.trace( "subscribe topics {} manualAcknowledgement {}", topics, manualAcknowledgement );
    }
}
