package oap.storage.mqtt;

import com.google.common.base.Preconditions;
import oap.json.Binder;
import oap.message.mqtt.MqttClient;
import oap.storage.MemoryStorage;
import oap.storage.Storage;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MongoStoragePublisher<I, T> implements Storage.DataListener<I, T> {
    private final MqttClient mqttClient;
    private final MemoryStorage<I, T> storage;
    private final String topicPrefix;
    public long expired = -1;

    public MongoStoragePublisher( MqttClient mqttClient, MemoryStorage<I, T> storage, String topicPrefix ) {
        this.mqttClient = mqttClient;
        this.storage = storage;
        this.topicPrefix = topicPrefix;

        this.storage.addDataListener( this );

        Preconditions.checkArgument( topicPrefix.endsWith( "/" ), "topic prefix must end with '/'" );
    }

    @Override
    public void changed( List<IdObject<I, T>> added, List<IdObject<I, T>> updated, List<IdObject<I, T>> deleted ) {
        for( IdObject<I, T> a : added ) {
            mqttClient.publish( topicPrefix + a.id, Binder.json.marshal( a.metadata ).getBytes( UTF_8 ), expired );
        }
        for( IdObject<I, T> u : updated ) {
            mqttClient.publish( topicPrefix + u.id, Binder.json.marshal( u.metadata ).getBytes( UTF_8 ), expired );
        }
        for( IdObject<I, T> d : deleted ) {
            mqttClient.publish( topicPrefix + d.id, null, -1 );
        }
    }
}
