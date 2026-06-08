package oap.storage.mqtt;

import oap.id.Id;
import oap.id.Identifier;
import oap.json.Binder;
import oap.message.mqtt.MosquittoFixture;
import oap.message.mqtt.MqttClient;
import oap.reflect.TypeRef;
import oap.storage.MemoryStorage;
import oap.storage.Metadata;
import oap.storage.Storage;
import oap.testng.AbstractFixture;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoStoragePublisherTest extends Fixtures {
    private final MosquittoFixture mosquittoFixture;

    public MongoStoragePublisherTest() {
        mosquittoFixture = fixture( new MosquittoFixture() ).withScope( AbstractFixture.Scope.CLASS );
    }

    @Test
    public void testPublisher() {
        MemoryStorage<String, TestMqttStoragePublisher> memoryStorage = new MemoryStorage<>( Identifier.forAnnotationFixed(), Storage.Lock.SERIALIZED, 1024 );

        try( MqttClient client = new MqttClient( "testPublisher", "localhost", mosquittoFixture.mqttPort ) ) {
            client.start();

            LinkedHashSet<String> sj1 = new LinkedHashSet<>();
            LinkedHashSet<String> sj2 = new LinkedHashSet<>();

            client.subscribe( "/tmp/t/#", ( t, d ) -> {
                if( d.length == 0 ) {
                    sj1.add( "DEL " + t );
                } else {
                    Metadata<TestMqttStoragePublisher> m = Binder.json.unmarshal( new TypeRef<>() {}, d );

                    sj1.add( m.object.id + "-" + m.object.data );
                }
            } );

            MongoStoragePublisher<String, TestMqttStoragePublisher> publisher = new MongoStoragePublisher<>( client, memoryStorage, "/tmp/t/" );

            memoryStorage.store( new TestMqttStoragePublisher( "id1", "data1" ) );
            memoryStorage.store( new TestMqttStoragePublisher( "id2", "data2" ) );

            memoryStorage.store( new TestMqttStoragePublisher( "id2", "data22" ) );

            memoryStorage.delete( "id1" );


            client.subscribe( "/tmp/t/#", ( c, d ) -> {
                Metadata<TestMqttStoragePublisher> m = Binder.json.unmarshal( new TypeRef<>() {}, d );

                sj2.add( m.object.id + "-" + m.object.data );
            } );


            assertThat( sj1 ).containsExactly( "id1-data1", "id2-data2", "id2-data22", "DEL /tmp/t/id1" );
            assertThat( sj2 ).containsExactly( "id2-data22" );
        }
    }

    public static class TestMqttStoragePublisher {
        @Id
        public String id;
        public String data;

        public TestMqttStoragePublisher( String id, String data ) {
            this.id = id;
            this.data = data;
        }
    }
}
