/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.storage;

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.content.ContentWriter;
import oap.json.Binder;
import oap.storage.dynamo.client.DynamodbClient;
import oap.storage.dynamo.client.Key;
import oap.storage.dynamo.client.batch.WriteBatchOperationHelper;
import oap.storage.dynamo.client.crud.AbstractOperation;
import oap.storage.dynamo.client.crud.DeleteItemOperation;
import oap.storage.dynamo.client.crud.OperationType;
import oap.storage.dynamo.client.crud.UpdateItemOperation;
import oap.storage.dynamo.client.streams.DynamodbStreamsRecordProcessor;
import oap.util.Pair;
import oap.util.Stream;
import org.joda.time.DateTimeUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static oap.concurrent.Threads.synchronizedOn;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.util.Pair.__;

@Slf4j
public class DynamoPersistence<I, T> extends AbstractPersistance<I, T> implements Closeable, AutoCloseable {

    public static final Path DEFAULT_CRASH_DUMP_PATH = Path.of( "/tmp/dynamo-persistance-crash-dump" );
    public static final DateTimeFormatter CRASH_DUMP_PATH_FORMAT_MILLIS = DateTimeFormat
        .forPattern( "yyyy-MM-dd-HH-mm-ss-SSS" )
        .withZoneUTC();

    private final DynamodbClient dynamodbClient;
    private final DynamodbStreamsRecordProcessor streamProcessor;
    private final WriteBatchOperationHelper batchWriter;
    private final Function<Map<String, AttributeValue>, Metadata<T>> convertFromDynamoItem;
    private final Function<Metadata<T>, Map<String, Object>> convertToDynamoItem;

    public DynamoPersistence( DynamodbClient dynamodbClient,
                              String tableName,
                              long delay,
                              MemoryStorage<I, T> storage,
                              Function<Map<String, AttributeValue>, Metadata<T>> convertFromDynamoItem,
                              Function<Metadata<T>, Map<String, Object>> convertToDynamoItem ) {
        this( dynamodbClient, tableName, delay, storage, convertFromDynamoItem, convertToDynamoItem, DEFAULT_CRASH_DUMP_PATH );
    }

    public DynamoPersistence( DynamodbClient dynamodbClient,
                              String tableName, long delay,
                              MemoryStorage<I, T> storage,
                              Function<Map<String, AttributeValue>, Metadata<T>> convertFromDynamoItem,
                              Function<Metadata<T>, Map<String, Object>> convertToDynamoItem,
                              Path crashDumpPath ) {
        super( storage, tableName, delay, crashDumpPath );
        this.convertFromDynamoItem = convertFromDynamoItem;
        this.convertToDynamoItem = convertToDynamoItem;
        this.streamProcessor = DynamodbStreamsRecordProcessor.builder( dynamodbClient ).build();
        batchWriter = new WriteBatchOperationHelper( dynamodbClient );
        this.dynamodbClient = dynamodbClient;

        storage.addDataListener( new Storage.DataListener<I, T>() {
            @Override
            public void permanentlyDeleted( IdObject<I, T> object ) {
                dynamodbClient.delete( new Key( tableName, "id", object.id.toString() ), null );
            }
        } );
    }

    @Override
    protected void processRecords( CountDownLatch cdl ) {
        TableDescription table = dynamodbClient.describeTable( tableName, null );
        String streamArn = table.latestStreamArn();
        cdl.countDown();
        streamProcessor.processRecords( streamArn, record -> {
            log.trace( "dynamoDb notification: {} ", record );
            var key = record.dynamodb().keys().get( "id" );
            var op = record.eventName();

            if( key == null ) return;
            var id = key.s();
            if( id == null ) return;

            switch( op ) {
                case REMOVE -> deleteById( id );
                case INSERT, MODIFY -> refreshById( id );
            }
        } );
    }

    @Override
    protected void load() {
        log.debug( "loading data from {}", tableName );
        Consumer<Metadata<T>> cons = metadata -> storage.memory.put( storage.identifier.get( metadata.object ), metadata );
        log.info( "Loading documents from [{}] DynamoDB table", tableName );
        dynamodbClient.getRecordsByScan( tableName, null ).map( convertFromDynamoItem ).forEach( cons );
        log.info( storage.size() + " object(s) loaded." );
    }

    @Override
    public void fsync() {
        var time = DateTimeUtils.currentTimeMillis();
        synchronizedOn( lock, () -> {
            if( stopped ) return;
            log.trace( "fsyncing, last: {}, objects in storage: {}", lastExecuted, storage.size() );
            var list = new ArrayList<AbstractOperation>( batchSize );
            var deletedIds = new ArrayList<I>( batchSize );
            AtomicInteger updated = new AtomicInteger();
            storage.memory.selectUpdatedSince( lastExecuted ).forEach( ( id, m ) -> {
                updated.incrementAndGet();
                if( m.isDeleted() ) {
                    deletedIds.add( id );
                    list.add( new DeleteItemOperation( new Key( tableName, "id", id.toString() ) ) );
                } else {
                    list.add( new UpdateItemOperation( new Key( tableName, "id", id.toString() ), convertToDynamoItem.apply( m ) ) );
                }
                if( list.size() >= batchSize ) {
                    persist( deletedIds, list );
                }
            } );
            log.trace( "fsyncing, last: {}, updated objects in storage: {}, total in storage: {}", lastExecuted, updated.get(), storage.size() );
            persist( deletedIds, list );
            lastExecuted = time;
        } );
    }

    private void persist( List<I> deletedIds, List<AbstractOperation> list ) {
        if( stopped ) return;
        if( list.isEmpty() ) return;
        try {
            batchWriter.addOperations( list );
            batchWriter.write();
            deletedIds.forEach( storage.memory::removePermanently );
            list.clear();
            deletedIds.clear();
        } catch( Exception e ) {
            Path filename = crashDumpPath.resolve( CRASH_DUMP_PATH_FORMAT_MILLIS.print( DateTimeUtils.currentTimeMillis() ) + ".json.gz" );
            log.error( "cannot persist. Dumping to " + filename + "...", e );
            List<Pair<String, AbstractOperation>> dump = Stream.of( list )
                .filter( model -> model.getType() == OperationType.UPDATE )
                .map( model -> __( "replace", model ) )
                .toList();
            Files.write( filename, GZIP, Binder.json.marshal( dump ), ContentWriter.ofString() );
        }
    }

    private void refreshById( String dynamoId ) {
        var res = dynamodbClient.getRecord( new Key( tableName, "id", dynamoId ), null );
        if( res == null || !res.isSuccess() ) return;
        Metadata<T> m = convertFromDynamoItem.apply( res.getSuccessValue() );
        storage.lock.synchronizedOn( dynamoId, () -> {
            var id = storage.identifier.fromString( dynamoId );
            var old = storage.memory.get( id );
            if( old.isEmpty() || m.modified > old.get().modified ) {
                log.debug( "refresh from dynamo {}", dynamoId );
                storage.memory.put( id, m );
                if( old.isEmpty() ) storage.fireAdded( id, m.object );
                else storage.fireUpdated( id, m.object );
            } else log.debug( "[{}] m.modified <= oldM.modified", dynamoId );
        } );
    }
}
