package oap.notification.mqtt;

import oap.notification.NotificationService;
import oap.notification.Qos;
import oap.notification.TestNotificationMessage;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.StringJoiner;

import static org.assertj.core.api.Assertions.assertThat;

public class MosquittoNotificationServiceTest extends Fixtures {
    private final MosquittoFixture mosquittoFixture;

    public MosquittoNotificationServiceTest() {
        mosquittoFixture = fixture( new MosquittoFixture() );
    }

    @Test
    public void testMessages() {
        StringJoiner msg = new StringJoiner( " / " );

        try( HivemqNotificationTransport notificationTransportClient1 = new HivemqNotificationTransport( "client1", "127.0.0.1", mosquittoFixture.getPort() );
             HivemqNotificationTransport notificationTransportClient2 = new HivemqNotificationTransport( "client2", "127.0.0.1", mosquittoFixture.getPort() ) ) {

            notificationTransportClient1.start();
            notificationTransportClient2.start();

            NotificationService notificationService1 = new NotificationService( "c1", notificationTransportClient1 );
            NotificationService notificationService2 = new NotificationService( "c2", notificationTransportClient2 );

            notificationService1.sendNotification( "/test", Qos.AT_LEAST_ONCE, new TestNotificationMessage( "val1" ) );

            notificationService2.subscribeToTopic( "/test", notification -> {
                TestNotificationMessage notificationMessage = ( TestNotificationMessage ) notification.message;
                msg.add( notificationMessage.value );
            } );

            notificationService1.sendNotification( "/test", Qos.AT_LEAST_ONCE, new TestNotificationMessage( "val2" ) );

            assertThat( msg ).hasToString( "val2" );
        }
    }
}
