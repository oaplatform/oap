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

package oap.storage.dynamo.client;


import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.storage.dynamo.client.atomic.AtomicUpdateFieldAndValue;
import oap.storage.dynamo.client.crud.DynamoDbTableModifier;
import oap.storage.dynamo.client.annotations.API;
import oap.storage.dynamo.client.atomic.AtomicUpdateRecordSupporter;
import oap.storage.dynamo.client.batch.DynamodbWriteBatch;
import oap.storage.dynamo.client.batch.WriteBatchOperationHelper;
import oap.storage.dynamo.client.convertors.DynamodbDatatype;
import oap.storage.dynamo.client.crud.DynamoDbReader;
import oap.storage.dynamo.client.crud.DynamoDbWriter;
import oap.storage.dynamo.client.crud.DynamodbEntityHelper;
import oap.storage.dynamo.client.crud.ItemsPage;
import oap.storage.dynamo.client.modifiers.AttributesModifier;
import oap.storage.dynamo.client.modifiers.BatchGetItemRequestModifier;
import oap.storage.dynamo.client.modifiers.BatchWriteItemRequestModifier;
import oap.storage.dynamo.client.modifiers.CreateGlobalSecondaryIndexActionModifier;
import oap.storage.dynamo.client.modifiers.CreateTableRequestModifier;
import oap.storage.dynamo.client.modifiers.DeleteItemRequestModifier;
import oap.storage.dynamo.client.modifiers.DescribeTableResponseModifier;
import oap.storage.dynamo.client.modifiers.DynamodbBinsModifier;
import oap.storage.dynamo.client.modifiers.GetItemRequestModifier;
import oap.storage.dynamo.client.modifiers.QueryRequestModifier;
import oap.storage.dynamo.client.modifiers.ScanRequestModifier;
import oap.storage.dynamo.client.modifiers.UpdateItemRequestModifier;
import oap.storage.dynamo.client.modifiers.UpdateTableRequestModifier;
import oap.util.Result;
import org.slf4j.event.Level;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dax.DaxClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static oap.util.Dates.s;


@Slf4j
@ThreadSafe
@API
public class DynamodbClient implements AutoCloseable, Closeable {

    /**
     * Any DynamoDB tables has its processing status: ACTIVE - is the only one which should not be synchronized.
     * All table oparations should be performed only with ACTIVE tables, otherwise we need to wait till status is active.
     */
    private static final ReentrantReadWriteLock tableSyncGuardian = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock.ReadLock readLock = tableSyncGuardian.readLock();
    private static final ReentrantReadWriteLock.WriteLock writeLock = tableSyncGuardian.writeLock();
    private final CountDownLatch connectionIsReady = new CountDownLatch( 1 );
    private final ExecutorService initService = Executors.newFixedThreadPool( 1 );

    public int apiCallTimeout = 1000;
    public int maxErrorRetries = 3;
    public int emptyReadLimit = 500;
    public int maxRowsPerSingleRead = 1000;

    /**
     * This client is used for DynamoDB manipulating.
     */
    @GuardedBy( "connectionIsReady" )
    private DynamoDbClient dynamoDbClient;

    @GuardedBy( "connectionIsReady" )
    private DynamoDbEnhancedClient enhancedClient;

    @GuardedBy( "connectionIsReady" )
    private DynamoDbStreamsClient streamClient;

    private DynamoDbReader reader;
    private DynamoDbWriter writer;
    private DynamoDbTableModifier tableModifier;
    private DynamodbEntityHelper entityHelper;

    public DynamodbClient( DynamoDbClient dynamoDbClient ) {
        this.dynamoDbClient = dynamoDbClient;
        log.info( "Creating DynamoDB enhanced client..." );
        enhancedClient = DynamoDbEnhancedClient
            .builder()
            .dynamoDbClient( dynamoDbClient )
            .build();
        log.info( "Creating reader..." );
        reader = new DynamoDbReader( dynamoDbClient, readLock );
        log.info( "Creating writer..." );
        writer = new DynamoDbWriter( dynamoDbClient, enhancedClient, readLock );
        log.info( "Creating helper..." );
        entityHelper = new DynamodbEntityHelper( reader, writer );
        log.info( "Creating table modifier..." );
        tableModifier = new DynamoDbTableModifier( dynamoDbClient, enhancedClient, readLock, writeLock );
        log.info( "Client is ready." );
        connectionIsReady.countDown();
    }

    @Override
    @API
    public void close() throws IOException {
        initService.shutdownNow();
        synchronized( connectionIsReady ) {
            if( dynamoDbClient != null ) dynamoDbClient.close();
        }
    }

    enum ClientSelector {
        DynamodbClientHttp( "http" ),
        DynamodbClientHttps( "https" ),
        DaxClient( "dax" );

        private final String protocol;

        ClientSelector( String protocol ) {
            this.protocol = protocol;
        }

        public String protocol() {
            return protocol;
        }
        static ClientSelector ofProtocol( String protocol ) {
            for ( ClientSelector value : values() ) {
                if ( value.protocol.equals( protocol ) ) return value;
            }
            throw new IllegalArgumentException( "no such protocol: " + protocol );
        }
    }

    /**
     * This creates a Dynamodb client to work with DynamoDB. Protocl should be 'http|https' or 'dax' for DAX cluster.
     * @param protocol for non-cluster; 'http' or 'https' or 'dax' for cluster
     * @param hosts hist name or ip address of dynamodb node or host name for DAX cluster
     * @param port by default 8000, for DAX cluster 8111
     * @param awsAccessKeyId aws AccessKeyId cred
     * @param awsSecretAccessKey aws SecretAccessKey
     * @param awsRegion aws Region name
     */
    @API
    public DynamodbClient( String protocol,
                           String hosts,
                           String port,
                           String awsAccessKeyId,
                           String awsSecretAccessKey,
                           String awsRegion ) {
        initService.submit( () -> {
            ClientSelector clientSelector = ClientSelector.ofProtocol( protocol );
            System.setProperty( "aws.accessKeyId", awsAccessKeyId );
            System.setProperty( "aws.secretAccessKey", awsSecretAccessKey );

            AwsCredentialsProvider provider = DefaultCredentialsProvider.create();
            if ( provider.resolveCredentials() == null ) {
                throw new RuntimeException( "AWS credentials to DynamoDB are not provided" );
            }
            Region region = Region.of( awsRegion );
            ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration
                .builder()
                .apiCallTimeout( Duration.ofMillis( apiCallTimeout ) )
                .retryPolicy( RetryPolicy.builder()
                    .numRetries( maxErrorRetries )
//                    .backoffStrategy( BackoffStrategy.defaultThrottlingStrategy() )
                    .retryCondition( RetryCondition.defaultRetryCondition()  )
                    .build() )
                .build();

            URI uri = createUri( clientSelector, hosts, port );
            log.debug( "Creating DynamoDB client..." );
            dynamoDbClient = createDynamoDbClient( uri, provider, region, overrideConfig );
            log.debug( "Creating DynamoDB enhanced client..." );
            enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient( dynamoDbClient ).build();
            log.debug( "Creating DynamoDB stream..." );
            streamClient = createStreamClient( uri, provider, region );
            reader = new DynamoDbReader( dynamoDbClient, readLock );
            writer = new DynamoDbWriter( dynamoDbClient, enhancedClient, readLock );
            entityHelper = new DynamodbEntityHelper( reader, writer );
            tableModifier = new DynamoDbTableModifier( dynamoDbClient, enhancedClient, readLock, writeLock );
            connectionIsReady.countDown();
            log.debug( "DynamoDB client is ready, connection established" );
        } );
        initService.shutdown();
    }

    public DynamoDbStreamsClient createStreamClient( URI uri, AwsCredentialsProvider provider, Region region ) {
        return DynamoDbStreamsClient.builder()
                .endpointOverride( uri )
                .credentialsProvider( provider )
                .region( region )
                .build();
    }

    private URI createUri( ClientSelector dynamodbClient, String hosts, String port ) {
        try {
            if ( dynamodbClient == ClientSelector.DaxClient ) {
                return new URL( dynamodbClient.protocol() + "://" + hosts ).toURI();
            }
            return new URL( dynamodbClient.protocol() + "://" + hosts + ":" + port + "/" ).toURI();
        } catch( Exception e ) {
            throw new RuntimeException( "Cannot create an URI for " + dynamodbClient, e );
        }
    }

    /**
     * Creates a DynamoDB client to work with database.
     * @param uri
     * @param provider
     * @param region
     * @param overrideConfig
     * @return
     */
    private DynamoDbClient createDynamoDbClient( URI uri, AwsCredentialsProvider provider, Region region, ClientOverrideConfiguration overrideConfig ) {
        return DynamoDbClient.builder()
            .endpointOverride( uri )
            .overrideConfiguration( overrideConfig )
            .credentialsProvider( provider )
            .region( region )
            .build();
    }

    /**
     * Creates a management tool for DAX cluster.
     *
     * @param host
     * @param port
     * @param provider
     * @param region
     * @param overrideConfig
     * @return
     */
    private DaxClient createDaxClient( String host, String port, AwsCredentialsProvider provider, Region region, ClientOverrideConfiguration overrideConfig ) {
        URI uri = createUri( ClientSelector.DaxClient, host, port );
        return DaxClient.builder()
            .endpointOverride( uri )
            .overrideConfiguration( overrideConfig )
            .credentialsProvider( provider )
            .region( region )
            .build();
    }

    public Result<List<String>, State> getTables( ) {
        ListTablesResponse resp = dynamoDbClient.listTables();
        if ( !resp.hasTableNames() ) return Result.failure( State.NOT_FOUND );
        return Result.success( resp.tableNames() );
    }

    @API
    public void waitConnectionEstablished() {
        try {
            if ( connectionIsReady.await( 1, TimeUnit.MINUTES ) ) {
                log.info( "DynamoDB clients are ready" );
                return;
            }
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
            LogConsolidated.log( log, Level.ERROR, s( 5 ), e.getMessage(), e );
            log.error( "Interruption detected", e );
        }
        throw new RuntimeException( "Cannot establish connection to DynamoDB within 1 min" );
    }

    @API
    public void start() {
    }

    @API
    public enum State {
        SUCCESS, NOT_FOUND, ERROR, VERSION_CHECK_FAILED
    }

    /**
     * This method is slightly faster than delete/create due to single lock getting.
     * @param tableName
     * @param keyName
     * @return
     */
    @API
    public Result<State, State> recreateTable( String tableName, String keyName ) {
        return tableModifier.recreateTable( tableName, keyName );
    }

    @API
    public Result<State, State> deleteTable( String tableName ) {
        return tableModifier.deleteTable( tableName );
    }

    @API
    public boolean deleteTableIfExists( String tableName ) {
        return tableModifier.deleteTableIfExists( tableName )._1();
    }

    @API
    public boolean createTableIfNotExist( String tableName, String key ) {
        return tableModifier.createTableIfNotExist( tableName, key );
    }

    @API
    public TableDescription describeTable( String tableName, DescribeTableResponseModifier modifier ) {
        return tableModifier.describeTable( tableName, modifier ).table();
    }

    @API
    public boolean createTable( String tableName,
                                long readCapacityUnits,
                                long writeCapacityUnits,
                                String partitionKeyName,
                                String partitionKeyType,
                                String sortKeyName,
                                String sortKeyType,
                                CreateTableRequestModifier modifier ) {
        return tableModifier.createTable( tableName, readCapacityUnits, writeCapacityUnits,
                partitionKeyName, partitionKeyType,
                sortKeyName, sortKeyType, modifier );
    }

    @API
    public boolean tableExists( String tableName ) {
        return tableModifier.tableExists( tableName );
    }

    /**
     * Gets an item bins for a given key. In order to get only some needed attributes (as it returns all of them) you may
     * use modifier like r->r.projectionExpression("bin1,bin2")
     * @param key
     * @param modifier
     * @return
     */
    @API
    public Result<Map<String, AttributeValue>, State> getRecord( Key key, GetItemRequestModifier modifier ) {
        return reader.getRecord( key, modifier );
    }

    /**
     * Gets list of items' bins for given keys. In order to get only some needed attributes (as it returns all of them) you may
     * use attributesToGet as a set of bin names.
     * @param tableName
     * @param keys
     * @param attributesToGet
     * @return
     */
    @API
    public Result<List<Map<String, AttributeValue>>, State> getRecord( String tableName, Set<Key> keys, Set<String> attributesToGet ) {
        return reader.getRecords( tableName, keys, attributesToGet );
    }

    @API
    public ItemsPage getRecord( String tableName, int pageSize, String keyName, String exclusiveStartItemId ) {
        return reader.getRecords( tableName, pageSize, keyName, exclusiveStartItemId, null );
    }

    /**
     * Gets list of items' bins for a given column name. In order to get only some needed attributes (as it returns all of them) you may
     * use modifier like r->r.projectionExpression("bin1,bin2")
     * @param tableName
     * @param pageSize
     * @param columnName
     * @param exclusiveStartItemId
     * @param modifier
     * @return
     */
    @API
    public ItemsPage getRecord( String tableName, int pageSize, String columnName, String exclusiveStartItemId, ScanRequestModifier modifier ) {
        return reader.getRecords( tableName, pageSize, columnName, exclusiveStartItemId, modifier );
    }

    /**
     * Returns a given entity read from dynamoDB and converted to a given class.
     * @param clazz
     * @param key
     * @param modifier
     * @return
     * @param <T>
     * @throws ReflectiveOperationException
     */
    @API
    public <T> Result<T, DynamodbClient.State> getItem( Class<T> clazz, Key key, GetItemRequestModifier modifier ) throws ReflectiveOperationException {
        return entityHelper.getItem( clazz, key, modifier );
    }

    @API
    public Stream<Map<String, AttributeValue>> getRecordsByScan( String tableName, ScanRequestModifier modifier ) {
        return reader.getRecordsByScan( tableName, modifier );
    }

    /**
     * Gets a stream of items by a given query. In order to get only some needed attributes (as it returns all of them) you may
     * use modifier like r->r.projectionExpression("bin1,bin2")
     * @param tableName
     * @param key
     * @param modifier
     * @return
     */
    @API
    public Stream<Map<String, AttributeValue>> getRecordsByQuery( String tableName, Key key, QueryRequestModifier modifier ) {
        return reader.getRecordsByQuery( tableName, key, modifier );
    }

    /**
     * Gets a stream of items by a given query using given index. In order to get only some needed attributes (as it returns all of them) you may
     * use modifier like r->r.projectionExpression("bin1,bin2")
     * See https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.KeyConditions.html how to query
     * @param tableName
     * @param key
     * @param modifier
     * @return
     */
    @API
    public Stream<Map<String, AttributeValue>> getRecordsByQuery( String tableName, String indexName, String filterExpression, QueryRequestModifier modifier ) {
        return reader.getRecordsByQuery( tableName, indexName, filterExpression, modifier );
    }

    @API
    public Result<UpdateItemResponse, State> update( Key key, Map<String, Object> binNamesAndValues, UpdateItemRequestModifier modifier ) {
        return writer.update( key, binNamesAndValues, modifier );
    }

    @API
    public Result<UpdateItemResponse, State> update( Key key,
                                                     String binName,
                                                     Object binValue ) {
        return writer.update( key, Collections.singletonMap( binName, binValue ), null );
    }

    @API
    public Result<UpdateItemResponse, DynamodbClient.State> updateRecord( Key key, Map<String, AttributeValue> binNamesAndValues, UpdateItemRequestModifier modifier ) {
        return writer.updateRecord( key, binNamesAndValues, modifier );
    }

    @API
    public Result<UpdateItemResponse, DynamodbClient.State> updateRecordAtomic( Key key, Map<String, AttributeValue> binNamesAndValues, UpdateItemRequestModifier modifier, AtomicUpdateFieldAndValue generation ) {
        Objects.requireNonNull( key );
        Objects.requireNonNull( binNamesAndValues );
        Objects.requireNonNull( generation );
        AtomicUpdateRecordSupporter supporter = new AtomicUpdateRecordSupporter();
        supporter.setAtomicUpdateFieldAndValue( generation );
        if ( modifier != null ) {
            supporter.andThen( modifier );
        }
        binNamesAndValues.forEach( supporter::addAtomicUpdateFor );
        return writer.updateRecord( key, binNamesAndValues, supporter, generation::onRetry );
    }

    @API
    public Result<UpdateItemResponse, DynamodbClient.State> updateRecordAtomicWithRetry( Key key, Set<String> binNames, AttributesModifier modifier, int retries, AtomicUpdateFieldAndValue generation ) {
        Objects.requireNonNull( key );
        Objects.requireNonNull( modifier );
        Objects.requireNonNull( generation );
        if ( retries < 1 ) throw new IllegalArgumentException( "retries number should be a positive number, but was " + retries );
        int retryCount = retries;
        Result<UpdateItemResponse, DynamodbClient.State> ret = null;
        do {
            Result<Map<String, AttributeValue>, DynamodbClient.State> foundResult = getRecord( key, m -> {
                if ( binNames != null && !binNames.isEmpty() ) {
                    binNames.remove( generation.getFieldName() );
                    m.projectionExpression( Joiner.on( "," ).skipNulls().join( binNames ) );
                }
            } );
            Map<String, AttributeValue> successValue;
            AttributeValue genValue = null;
            try {
                if ( foundResult.isSuccess() ) {
                    genValue = foundResult.getSuccessValue().get( generation.getFieldName() );
                    successValue = modifier.apply( foundResult.getSuccessValue() );
                } else {
                    successValue = modifier.apply( new HashMap<>() );
                }
            } catch ( UnsupportedOperationException ex ) {
                throw new IllegalArgumentException( "Cannot process attributes", ex );
            }
            //remove key from map - it's necessary
            successValue.remove( key.getColumnName() );
            long gen = genValue != null ? Long.parseLong( genValue.n() ) : generation.getValue();
            ret = updateRecordAtomic( key, successValue, null, new AtomicUpdateFieldAndValue( generation, gen ) );
        } while( retryCount-- > 0 && ret.failureValue == DynamodbClient.State.VERSION_CHECK_FAILED );
        if ( ret.failureValue == DynamodbClient.State.VERSION_CHECK_FAILED ) {
            generation.onExhaustedRetryAttempts();
        }
        return ret;
    }

    public Result<UpdateItemResponse, DynamodbClient.State> updateOrCreateItem( Key key, Object item, UpdateItemRequestModifier modifier ) throws Exception {
        return entityHelper.updateOrCreateItem( key, item, modifier );
    }

    public Result<Map<String, AttributeValue>, State> delete( Key key, DeleteItemRequestModifier modifier ) {
        return writer.delete( key, modifier );
    }

    public DynamodbWriteBatch createBatch( ) {
        return new WriteBatchOperationHelper( this );
    }

    public Map<String, Collection<Map<String, AttributeValue>>> readBatch( BatchGetItemRequest.Builder builder, BatchGetItemRequestModifier modifier ) {
        return reader.batchGetRecords( builder, modifier );
    }

    public BatchWriteItemResponse writeBatch( BatchWriteItemRequest.Builder builder, BatchWriteItemRequestModifier modifier ) {
        return writer.writeBatch( builder, modifier );
    }

    public BatchWriteItemResponse writeBatchUnprocessed( Map<String, List<WriteRequest>> unprocessed ) {
        return writer.writeBatchUnprocessed( unprocessed );
    }

    public DynamoDbStreamsClient getStreamClient() {
        return streamClient;
    }

    public Map<String, AttributeValue> findAndModify( Key key, DynamodbBinsModifier updateModifier, GetItemRequestModifier readModifier ) {
        Objects.requireNonNull( key );
        Objects.requireNonNull( updateModifier );
        try {
            readLock.lock();
            Result<Map<String, AttributeValue>, State> record = reader.getRecord( key, readModifier );
            if ( record.getSuccessValue() == null ) {
                if ( record.getFailureValue() == State.NOT_FOUND ) {
                    record = Result.success( Collections.emptyMap() );
                } else {
                    log.error( "Could not get item by key '{}'", key );
                    return null;
                }
            }
            Map<String, AttributeValue> binNamesAndValues = new HashMap<>( record.getSuccessValue() );
            updateModifier.accept( binNamesAndValues );
            Result<UpdateItemResponse, State> result = writer.updateRecord( key, binNamesAndValues, b -> b.returnValues( ReturnValue.ALL_NEW ) );
            if ( result.getSuccessValue() == null ) {
                log.error( "Could not write item by key '{}'", key );
                return null;
            }
            UpdateItemResponse response = result.getSuccessValue();
            return response.attributes();
        } finally {
            readLock.unlock();
        }
    }

    public UpdateTableResponse createIndex( KeyForSchema key,
                                            String indexName,
                                            String columnName,
                                            DynamodbDatatype datatype,
                                            CreateGlobalSecondaryIndexActionModifier indexModifier,
                                            UpdateTableRequestModifier updateTableModifier ) {
        return tableModifier.createIndex( key, indexName, columnName, datatype, indexModifier, updateTableModifier );
    }

    public UpdateTableResponse deleteIndex( String tableName, String indexName ) {
        return tableModifier.deleteIndex( tableName, indexName );
    }

    public DynamoDbEnhancedClient getEnhancedClient() {
        return enhancedClient;
    }

    public DynamoDbClient getDynamoDbClient() {
        return dynamoDbClient;
    }

    public void setStreamClient( DynamoDbStreamsClient streamClient ) {
        this.streamClient = streamClient;
    }
}
