package oap.message.mqtt;

import oap.testng.Fixtures;
import oap.util.Dates;
import org.apache.commons.lang3.mutable.MutableObject;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.testng.Asserts.assertEventually;
import static org.assertj.core.api.Assertions.assertThat;

public class MqttClientTest extends Fixtures {
    private final MosquittoFixture mosquittoFixture;

    public MqttClientTest() {
        mosquittoFixture = fixture( new MosquittoFixture() );
    }

    @Test
    public void testPublishSubscribe() {
        MutableObject<String> m = new MutableObject<>();
        AtomicInteger count1 = new AtomicInteger();
        AtomicInteger count2 = new AtomicInteger();

        try( MqttClient mqttPublisher = new MqttClient( "p", "localhost", mosquittoFixture.mqttPort );
             MqttClient mqttSubscriber1 = new MqttClient( "s1", "localhost", mosquittoFixture.mqttPort );
             MqttClient mqttSubscriber2 = new MqttClient( "s2", "localhost", mosquittoFixture.mqttPort ) ) {

            mqttPublisher.start();
            mqttSubscriber1.start();
            mqttSubscriber2.start();

            mqttSubscriber1.subscribe( "/test/topic/#", ( _, msg ) -> {
                assertThat( new String( msg, UTF_8 ) ).isEqualTo( m.get() );
                count1.incrementAndGet();
            } );

            mqttSubscriber2.subscribe( "/test/topic/#", ( _, msg ) -> {
                assertThat( new String( msg, UTF_8 ) ).isEqualTo( m.get() );
                count2.incrementAndGet();
            } );

            m.setValue( "msg1" );
            mqttPublisher.publish( "/test/topic/id1", m.get().getBytes( UTF_8 ), Dates.m( 10 ) );

            assertEventually( 100, 40, () -> {
                assertThat( count1 ).hasValue( 1 );
                assertThat( count2 ).hasValue( 1 );
            } );

            m.setValue( "msg2" );
            mqttPublisher.publish( "/test/topic/id2", m.get().getBytes( UTF_8 ), Dates.m( 10 ) );

            assertEventually( 100, 40, () -> {
                assertThat( count1 ).hasValue( 2 );
                assertThat( count2 ).hasValue( 2 );
            } );
        }
    }

    @Test
    public void testReconnect() {
        MutableObject<String> m = new MutableObject<>();
        AtomicInteger count1 = new AtomicInteger();
        AtomicInteger count2 = new AtomicInteger();

        try( MqttClient mqttPublisher = new MqttClient( "p", "localhost", mosquittoFixture.mqttPort );
             MqttClient mqttSubscriber1 = new MqttClient( "s1", "localhost", mosquittoFixture.mqttPort ) ) {

            mqttPublisher.start();
            mqttSubscriber1.start();

            mqttSubscriber1.subscribe( "/test/topic/#", ( _, msg ) -> {
                assertThat( new String( msg, UTF_8 ) ).isEqualTo( m.get() );
                count1.incrementAndGet();
            } );


            m.setValue( "msg1" );
            mqttPublisher.publish( "/test/topic/id1", m.get().getBytes( UTF_8 ), Dates.m( 10 ) );

            assertEventually( 100, 40, () -> {
                assertThat( count1 ).hasValue( 1 );
            } );

            m.setValue( "msg2" );
            mqttPublisher.publish( "/test/topic/id2", m.get().getBytes( UTF_8 ), Dates.m( 10 ) );

            assertEventually( 100, 40, () -> {
                assertThat( count1 ).hasValue( 2 );
            } );

            try( MqttClient mqttSubscriber2 = new MqttClient( "s2", "localhost", mosquittoFixture.mqttPort ) ) {
                mqttSubscriber2.start();

                mqttSubscriber2.subscribe( "/test/topic/#", ( _, msg ) -> {
                    assertThat( new String( msg, UTF_8 ) ).satisfiesAnyOf(
                        m1 -> assertThat( m1 ).isEqualTo( "msg1" ),
                        m2 -> assertThat( m2 ).isEqualTo( "msg2" )
                    );
                    count2.incrementAndGet();
                } );

                assertEventually( 100, 40, () -> {
                    assertThat( count2 ).hasValue( 2 );
                } );
            }
        }

    }
}
