package oap.notification.mqtt;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HivemqNotificationTransportTest {
    @Test
    public void testRndPatternReplacedWithRandomLetters() {
        HivemqNotificationTransport transport = new HivemqNotificationTransport( "client-%rnd%", "127.0.0.1", 1883 );

        assertThat( transport.getIdentifier() )
            .doesNotContain( "%rnd%" )
            .matches( "client-[a-zA-Z]{5}" );
    }

    @Test
    public void testRndPatternProducesDifferentIdentifiersEachTime() {
        HivemqNotificationTransport transport1 = new HivemqNotificationTransport( "client-%rnd%", "127.0.0.1", 1883 );
        HivemqNotificationTransport transport2 = new HivemqNotificationTransport( "client-%rnd%", "127.0.0.1", 1883 );

        assertThat( transport1.getIdentifier() ).isNotEqualTo( transport2.getIdentifier() );
    }

    @Test
    public void testIdentifierWithoutRndPatternIsUnchanged() {
        HivemqNotificationTransport transport = new HivemqNotificationTransport( "client1", "127.0.0.1", 1883 );

        assertThat( transport.getIdentifier() ).isEqualTo( "client1" );
    }
}
