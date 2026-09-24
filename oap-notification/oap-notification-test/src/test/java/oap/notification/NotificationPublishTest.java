package oap.notification;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NotificationPublishTest {
    @Test
    public void testAcknowledgeThrowsForPlainNotificationPublish() {
        NotificationPublish notificationPublish = new NotificationPublish( "topic", Qos.AT_LEAST_ONCE, false, "val" );

        assertThatThrownBy( notificationPublish::acknowledge )
            .isInstanceOf( NotificationException.class );
    }
}
