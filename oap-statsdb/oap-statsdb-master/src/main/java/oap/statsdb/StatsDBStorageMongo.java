package oap.statsdb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import lombok.extern.slf4j.Slf4j;
import oap.reflect.TypeRef;
import oap.storage.mongo.MongoClient;
import org.apache.commons.lang3.mutable.MutableInt;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.joda.time.DateTimeUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * Created by igor.petrenko on 26.03.2019.
 */
@Slf4j
public class StatsDBStorageMongo implements StatsDBStorage, Closeable {
    private static final ReplaceOptions REPLACE_OPTIONS_UPSERT = new ReplaceOptions().upsert( true );

    private final MongoCollection<MongoNode> collection;
    public int bulkSize = 1000;
    private long lastFsync = -1;

    public StatsDBStorageMongo( MongoClient mongoClient, String table ) {
        TypeRef<MongoNode> ref = new TypeRef<>() {
        };

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs( new JsonNodeCodec() ),
            mongoClient.getCodecRegistry()
        );

        this.collection = mongoClient
            .getCollection( table, ref.clazz() )
            .withCodecRegistry( codecRegistry );

    }

    @Override
    public Map<String, Node> load( NodeSchema schema ) {
        log.debug( "load {}", schema );
        final Map<String, Node> db = new HashMap<>();

        final Consumer<MongoNode> cons = node -> {
            assert node.n.db.isEmpty();

            Map<String, Node> cdb = db;
            for( int i = 0; i < node._id.size() - 1; i++ ) {
                NodeSchema.NodeConfiguration nc = schema.get( i );
                String key = node._id.get( nc.key );
                cdb = cdb.computeIfAbsent( key, k -> new Node( nc.newInstance() ) ).db;
            }

            String lastId = node._id.get( schema.get( node._id.size() - 1 ).key );
            Node lastNode = cdb.get( lastId );
            if( lastNode == null ) {
                cdb.put( lastId, node.n );
            } else {
                cdb.put( lastId, node.n );
                node.n.db.putAll( lastNode.db );
            }
        };

        collection.find().forEach( cons );

        lastFsync = DateTimeUtils.currentTimeMillis();

        return db;
    }

    @Override
    public void store( NodeSchema schema, Map<String, Node> db ) {
        log.debug( "store {}", schema );
        int count = 0;

        long now = DateTimeUtils.currentTimeMillis();

        ArrayList<WriteModel<MongoNode>> bulk = new ArrayList<WriteModel<MongoNode>>();
        count += store( schema, 0, new HashMap<>(), db, bulk );
        if( !bulk.isEmpty() ) {
            collection.bulkWrite( bulk );
            count += bulk.size();
        }

        lastFsync = now;

        log.debug( "[{}] fsync modified: {}", collection.getNamespace(), count );
    }

    private int store( NodeSchema schema, int index, Map<String, String> id,
                       Map<String, Node> db, ArrayList<WriteModel<MongoNode>> bulk ) {
        if( db.isEmpty() ) return 0;

        if( index < 0 || index >= schema.size() ) {
            throw new IllegalArgumentException( "index '" + index + "' is out of bounds [0.." + schema.size() + ")" );
        }

        MutableInt count = new MutableInt();

        db.forEach( ( key, value ) -> {
            HashMap<String, String> newId = new HashMap<>( id );
            newId.put( schema.get( index ).key, key );

            if( value.mt >= lastFsync ) {
                bulk.add( new ReplaceOneModel<>( eq( "_id", newId ), new MongoNode( newId, new Node( value.ct, value.mt, value.v ) ), REPLACE_OPTIONS_UPSERT ) );
                if( bulk.size() >= bulkSize ) {
                    collection.bulkWrite( bulk );
                    count.add( bulk.size() );
                    bulk.clear();
                }
            }

            count.add( store( schema, index + 1, newId, value.db, bulk ) );
        } );

        return count.intValue();
    }

    @Override
    public void removeAll() {
        collection.deleteMany( new BsonDocument() );
    }

    @Override
    public void permanentlyDelete( NodeSchema schema, String... keys ) {
        log.trace( "permanentlyDelete {}", Arrays.asList( keys ) );
        ArrayList<Bson> query = new ArrayList<>();

        for( int index = 0; index < keys.length; index++ ) {
            query.add( eq( "_id." + schema.get( index ).key, keys[index] ) );
        }

        log.trace( "deleteMany( eq( _id, {}))", and( query ) );

        DeleteResult deleteMany = collection.deleteMany( and( query ) );
        log.trace( "DeletedCount {}", deleteMany.getDeletedCount() );
    }

    @Override
    public void close() {
    }

    public void insertMany( List<MongoNode> stats ) {
        collection.insertMany( stats );
    }
}
