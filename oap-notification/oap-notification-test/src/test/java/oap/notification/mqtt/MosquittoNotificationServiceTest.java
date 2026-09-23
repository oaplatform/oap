package oap.notification.mqtt;

import oap.notification.NotificationPublish;
import oap.notification.NotificationPublishWithAcknowledge;
import oap.notification.NotificationService;
import oap.notification.Qos;
import oap.notification.TestNotificationMessage;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.StringJoiner;

import static oap.testng.Asserts.assertEventually;
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

            NotificationService notificationService1 = new NotificationService( notificationTransportClient1 );
            NotificationService notificationService2 = new NotificationService( notificationTransportClient2 );

            notificationService1.sendNotification( "/test", Qos.AT_LEAST_ONCE, false, new TestNotificationMessage( "val1" ) );

            notificationService2.subscribeToTopic( "/test", notification -> {
                TestNotificationMessage notificationMessage = ( TestNotificationMessage ) notification.message;
                msg.add( notificationMessage.value );
            } );

            notificationService1.sendNotification( "/test", Qos.AT_LEAST_ONCE, false, new TestNotificationMessage( "val2" ) );

            assertEventually( 100, 20, () -> {
                assertThat( msg ).hasToString( "val2" );
            } );
        }
    }

    @Test
    public void testDefaultSubscribeDeliversPlainNotificationPublish() {
        StringJoiner msg = new StringJoiner( " / " );

        try( HivemqNotificationTransport notificationTransportClient1 = new HivemqNotificationTransport( "client1-plain", "127.0.0.1", mosquittoFixture.getPort() );
             HivemqNotificationTransport notificationTransportClient2 = new HivemqNotificationTransport( "client2-plain", "127.0.0.1", mosquittoFixture.getPort() ) ) {

            notificationTransportClient1.start();
            notificationTransportClient2.start();

            NotificationService notificationService1 = new NotificationService( notificationTransportClient1 );
            NotificationService notificationService2 = new NotificationService( notificationTransportClient2 );

            notificationService2.subscribeToTopic( "/test-plain", notification -> {
                assertThat( notification ).isExactlyInstanceOf( NotificationPublish.class );

                TestNotificationMessage notificationMessage = ( TestNotificationMessage ) notification.message;
                msg.add( notificationMessage.value );
            } );

            notificationService1.sendNotification( "/test-plain", Qos.AT_LEAST_ONCE, false, new TestNotificationMessage( "plain-val" ) );

            assertEventually( 100, 20, () -> {
                assertThat( msg ).hasToString( "plain-val" );
            } );
        }
    }

    @Test
    public void testManualAcknowledgement() {
        StringJoiner msg = new StringJoiner( " / " );

        try( HivemqNotificationTransport notificationTransportClient1 = new HivemqNotificationTransport( "client1-ack", "127.0.0.1", mosquittoFixture.getPort() );
             HivemqNotificationTransport notificationTransportClient2 = new HivemqNotificationTransport( "client2-ack", "127.0.0.1", mosquittoFixture.getPort() ) ) {

            notificationTransportClient1.start();
            notificationTransportClient2.start();

            NotificationService notificationService1 = new NotificationService( notificationTransportClient1 );
            NotificationService notificationService2 = new NotificationService( notificationTransportClient2 );

            notificationService2.subscribeToTopic( "/test-ack", true, notification -> {
                assertThat( notification ).isExactlyInstanceOf( NotificationPublishWithAcknowledge.class );

                TestNotificationMessage notificationMessage = ( TestNotificationMessage ) notification.message;
                msg.add( notificationMessage.value );

                notification.acknowledge();
            } );

            notificationService1.sendNotification( "/test-ack", Qos.AT_LEAST_ONCE, false, new TestNotificationMessage( "ack-val" ) );

            assertEventually( 100, 20, () -> {
                assertThat( msg ).hasToString( "ack-val" );
            } );
        }
    }
}
