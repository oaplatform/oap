package oap.mail.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import oap.mail.MailQueuePersistence;
import oap.mail.Message;
import oap.reflect.TypeRef;
import oap.storage.mongo.JsonCodec;
import oap.storage.mongo.MongoClient;
import oap.util.Cuid;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Iterator;

public class MailQueuePersistenceMongo implements MailQueuePersistence {
    private final MongoCollection<MessageData> collection;

    public MailQueuePersistenceMongo( MongoClient mongoClient, String collectionName ) {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs( new JsonCodec<>( new TypeRef<MessageData>() {}, md -> md.id, id -> id ) ),
            mongoClient.getCodecRegistry()
        );
        this.collection = mongoClient
            .getCollection( collectionName, MessageData.class )
            .withCodecRegistry( codecRegistry );
    }

    @Override
    public void add( Message message ) {
        collection.insertOne( new MessageData( Cuid.UNIQUE.next(), message ) );
    }

    @Override
    public int size() {
        return ( int ) collection.countDocuments();
    }

    @Override
    public Iterator<Message> iterator() {
        MongoCursor<MessageData> iterator = collection.find().iterator();
        return new Iterator<>() {
            MessageData messageData;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Message next() {
                messageData = iterator.next();
                return messageData.message;
            }

            @Override
            public void remove() {
                collection.deleteOne( Filters.eq( "_id", messageData.id ) );
            }
        };
    }
}
