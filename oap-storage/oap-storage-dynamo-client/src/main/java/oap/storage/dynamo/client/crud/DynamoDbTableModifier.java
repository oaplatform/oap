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

package oap.storage.dynamo.client.crud;

import lombok.extern.slf4j.Slf4j;
import oap.LogConsolidated;
import oap.storage.dynamo.client.KeyForSchema;
import oap.storage.dynamo.client.DynamodbClient;
import oap.storage.dynamo.client.Key;
import oap.storage.dynamo.client.Record;
import oap.storage.dynamo.client.annotations.API;
import oap.storage.dynamo.client.convertors.DynamodbDatatype;
import oap.storage.dynamo.client.modifiers.CreateGlobalSecondaryIndexActionModifier;
import oap.storage.dynamo.client.modifiers.CreateTableRequestModifier;
import oap.storage.dynamo.client.modifiers.DescribeTableResponseModifier;
import oap.storage.dynamo.client.modifiers.TableSchemaModifier;
import oap.storage.dynamo.client.modifiers.UpdateTableRequestModifier;
import oap.storage.dynamo.client.exceptions.ReservedNameException;
import oap.storage.dynamo.client.restrictions.ReservedWords;
import oap.util.Pair;
import oap.util.Result;
import org.slf4j.event.Level;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateGlobalSecondaryIndexAction;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteGlobalSecondaryIndexAction;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexUpdate;
import software.amazon.awssdk.services.dynamodb.model.IndexStatus;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.StreamSpecification;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableResponse;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static oap.util.Dates.s;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

@Slf4j
@ThreadSafe
public class DynamoDbTableModifier {

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClient enhancedClient;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;

    @API
    public DynamoDbTableModifier( DynamoDbClient dynamoDbClient, DynamoDbEnhancedClient enhancedClient, ReentrantReadWriteLock.ReadLock readLock, ReentrantReadWriteLock.WriteLock writeLock ) {
        this.dynamoDbClient = dynamoDbClient;
        this.enhancedClient = enhancedClient;
        this.readLock = readLock;
        this.writeLock = writeLock;
    }

    private TableSchema<Record> createSchemaForRecord( TableSchemaModifier<Record> modifier ) {
        StaticTableSchema.Builder<Record> builder = StaticTableSchema.builder( Record.class )
                .newItemSupplier( Record::new )
                .addAttribute( String.class, a -> a.name( "id" )
                        .getter( Record::getId )
                        .setter( Record::setId )
                        .tags( primaryPartitionKey() ) )
                .addAttribute( String.class, a -> a.name( "sortKey" )
                        .getter( Record::getSortKey )
                        .setter( Record::setSortKey )
                        .tags( primarySortKey() ) );
        if ( modifier != null ) modifier.accept( builder );
        return builder.build();
    }

    /**
     * Creates a table for using with putItem, getItem, deleteItem, query methods.
     * Note: processing MUST be wrapped into readLock.
     * @param tableName
     * @param read
     * @param write
     * @param indexName
     * @param modifier
     * @return
     */
    @API
    public DynamoDbTable<Record> createTableWithSchema( String tableName, long read, long write, String indexName, TableSchemaModifier<Record> modifier ) {
        Objects.requireNonNull( tableName );
        Objects.requireNonNull( indexName );
        if ( !ReservedWords.isTableNameOrIndexAppropriate( tableName ) ) {
            throw new ReservedNameException( "Table '" + tableName + "' is forbidden in DynamoDB" );
        }
        if ( !ReservedWords.isTableNameOrIndexAppropriate( indexName ) ) {
            throw new ReservedNameException( "Table '" + tableName + "' is forbidden in DynamoDB" );
        }
        DynamoDbTable<Record> mappedTable = enhancedClient.table( tableName, createSchemaForRecord( modifier ) );
        mappedTable
                .createTable( r -> r.provisionedThroughput( createProvisionedThroughput( read, write ) )
                .globalSecondaryIndices( EnhancedGlobalSecondaryIndex.builder()
                                .indexName( indexName )
                                .projection( p -> p.projectionType( ProjectionType.KEYS_ONLY ) )
                                .provisionedThroughput( createProvisionedThroughput( read, write ) )
                                .build() ) );
        writeLock.lock();
        try {
            dynamoDbClient.waiter().waitUntilTableExists( r -> r.tableName( tableName ) );
        } finally {
            writeLock.unlock();
        }
        return mappedTable;
    }

    private ProvisionedThroughput createProvisionedThroughput( long read, long write ) {
       return ProvisionedThroughput.builder()
                .readCapacityUnits( read )
                .writeCapacityUnits( write )
                .build();
    }

    @API
    public Result<DynamodbClient.State, DynamodbClient.State> recreateTable( String tableName, String keyName ) {
        Objects.requireNonNull( tableName );
        try {
            writeLock.lock();
            TableDescription description = deleteTableIfExists( tableName )._2();
            if ( description != null ) {
                createTable( tableName,
                        description.provisionedThroughput().readCapacityUnits(), description.provisionedThroughput().writeCapacityUnits(),
                        keyName, "S", null, null,
                        x -> x.streamSpecification( StreamSpecification.builder()
                                .streamEnabled( true )
                                .streamViewType( StreamViewType.NEW_AND_OLD_IMAGES )
                                .build() )
                            .attributeDefinitions( description.attributeDefinitions() )
                            .keySchema( description.keySchema() )
                );
            } else {
                createTableIfNotExist( tableName, keyName );
            }
            log.info( "table recreated: " + tableName );
            LogConsolidated.log( log, Level.INFO, s( 5 ), "table recreated: " + tableName, null );
        } finally {
            writeLock.unlock();
        }
        return Result.success( DynamodbClient.State.SUCCESS );
    }

    @API
    public Pair<Boolean, TableDescription> deleteTableIfExists( String tableName ) {
        Objects.requireNonNull( tableName );
        if( !tableExists( tableName ) ) {
            return new Pair<>( true, null );
        }
        long time = System.currentTimeMillis();
        try {
            writeLock.lock();
            try {
                DeleteTableResponse response = dynamoDbClient.deleteTable( createDeleteTableRequest( tableName ) );
                // Call and wait until the Amazon DynamoDB table is deleted
                WaiterResponse<DescribeTableResponse> waiterResponse =
                        dynamoDbClient.waiter().waitUntilTableNotExists( createDescribeTableRequest( tableName, null ) );
                log.info( "Table '{}' is deleted in {} ms", tableName, System.currentTimeMillis() - time );
                return new Pair<>( !waiterResponse.matched().response().isPresent(), response.tableDescription() );
            } catch( ResourceNotFoundException ex ) {
                return new Pair<>( true, null );
            } catch( DynamoDbException e ) {
                log.error( "Could not create a table '{}'", tableName, e );
                LogConsolidated.log( log, Level.ERROR, s( 5 ), e.getMessage(), e );
                return new Pair<>( false, null );
            }
        } finally {
            writeLock.unlock();
        }
    }

    @API
    public boolean createTableIfNotExist( String tableName, String key ) {
        Objects.requireNonNull( tableName );
        if( tableExists( tableName ) ) {
            return true;
        }
        return createTable( tableName, 10, 10, key, "S", null, null, null );
    }

    @API
    public boolean tableExists( String tableName ) {
        Objects.requireNonNull( tableName );
        try {
            readLock.lock();
            return dynamoDbClient.listTables().tableNames().stream().filter( name -> name.equals( tableName ) ).findAny().isPresent();
        } finally {
            readLock.unlock();
        }
    }

    @API
    public boolean createTable( String tableName, long readCapacityUnits, long writeCapacityUnits,
                                String partitionKeyName, String partitionKeyType,
                                String sortKeyName, String sortKeyType,
                                CreateTableRequestModifier modifier ) {
        Objects.requireNonNull( tableName );
        if ( !ReservedWords.isTableNameOrIndexAppropriate( tableName ) ) {
            throw new ReservedNameException( "Table '" + tableName + "' is forbidden in DynamoDB" );
        }
        long time = System.currentTimeMillis();
        try {
            List<KeySchemaElement> keySchema = new ArrayList<>();
            keySchema.add( KeySchemaElement.builder().attributeName( partitionKeyName ).keyType( KeyType.HASH ).build() ); // Partition key

            List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
            attributeDefinitions.add( AttributeDefinition.builder().attributeName( partitionKeyName ).attributeType( partitionKeyType ).build() );
            if ( sortKeyName != null ) {
                keySchema.add( KeySchemaElement.builder().attributeName( sortKeyName ).keyType( KeyType.RANGE ).build() ); // Sort key
                attributeDefinitions.add( AttributeDefinition.builder().attributeName( sortKeyName ).attributeType( sortKeyType ).build() );
            }
            CreateTableRequest.Builder builder = CreateTableRequest.builder()
                    .tableName( tableName )
                    .keySchema( keySchema )
                    .attributeDefinitions( attributeDefinitions )
                    .billingMode( BillingMode.PROVISIONED )
                    .provisionedThroughput( createProvisionedThroughput( readCapacityUnits, writeCapacityUnits ) );
            if ( modifier != null ) {
                modifier.accept( builder );
            }
            CreateTableRequest request = builder.build();
            boolean tableCreated = false;
            boolean streamEnabled = false;
            StreamSpecification streamSpecification = null;
            try {
                writeLock.lock();
                CreateTableResponse createTableResponse = dynamoDbClient.createTable( request );
                WaiterResponse<DescribeTableResponse> response =
                        dynamoDbClient.waiter().waitUntilTableExists( createDescribeTableRequest( tableName, null ) );
                tableCreated = response.matched().response().isPresent();
                streamSpecification = createTableResponse.tableDescription().streamSpecification();
                streamEnabled = streamSpecification != null && streamSpecification.streamEnabled();
                log.info( "Table '{}' is created & active in {} ms", tableName, System.currentTimeMillis() - time );
            } finally {
                writeLock.unlock();
            }
            time = System.currentTimeMillis();
            String fictiveRecordId = null;
            if ( modifier != null && streamEnabled ) {
                try {
                    readLock.lock();
                    // put/delete fictive item to table
                    DynamoDbWriter writer = new DynamoDbWriter( dynamoDbClient, enhancedClient, readLock );
                    fictiveRecordId = "fictiveRecordForTable:" + tableName + ":" + UUID.randomUUID().toString();
                    Key key = new Key( tableName, partitionKeyName, fictiveRecordId );
                    writer.update( key, Collections.singletonMap( "fictiveBin", String.valueOf( System.currentTimeMillis() ) ), null );
                    writer.delete( key, null );
                } finally {
                    readLock.unlock();
                }
                log.info( "Stream for table '{}' is ready in {} ms", tableName, System.currentTimeMillis() - time );
            }
            return tableCreated;
        } catch ( Exception e ) {
            log.error( "Could not create a table '{}'", tableName, e );
            LogConsolidated.log( log, Level.ERROR, s( 5 ), e.getMessage(), e );
        }
        return false;
    }

    @API
    public Result<DynamodbClient.State, DynamodbClient.State> deleteTable( String tableName ) {
        Objects.requireNonNull( tableName );
        deleteTableIfExists( tableName );
        return Result.success( DynamodbClient.State.SUCCESS );
    }

    public DescribeTableResponse describeTable( String tableName, DescribeTableResponseModifier modifier ) {
        Objects.requireNonNull( tableName );
        DescribeTableRequest request = createDescribeTableRequest( tableName, modifier );
        try {
            readLock.lock();
            return dynamoDbClient.describeTable( request );
        } finally {
            readLock.unlock();
        }
    }

    @API
    public UpdateTableResponse createIndex( KeyForSchema key,
                                            String indexName,
                                            String columnNameForIndex,
                                            DynamodbDatatype datatype,
                                            CreateGlobalSecondaryIndexActionModifier indexModifier,
                                            UpdateTableRequestModifier tableUpdateModifier ) {
        Objects.requireNonNull( indexName );
        Objects.requireNonNull( key.getTableName() );
        Objects.requireNonNull( columnNameForIndex );
        Objects.requireNonNull( datatype );
        if ( datatype != DynamodbDatatype.NUMBER
            && datatype != DynamodbDatatype.STRING
            && datatype != DynamodbDatatype.BINARY ) {
            //see https://stackoverflow.com/questions/33557804/dynamodb-global-secondary-index-on-set-items
            throw new IllegalArgumentException( "Only [ String | Binary | Number ] are supported as SGI in DynamoDB" );
        }
        if ( !ReservedWords.isTableNameOrIndexAppropriate( indexName ) ) {
            throw new ReservedNameException( "Index '" + indexName + "' is forbidden in DynamoDB" );
        }

        CreateGlobalSecondaryIndexAction.Builder indexUpdareRequestBuilder = CreateGlobalSecondaryIndexAction.builder()
            .indexName( indexName )
            .keySchema(
                    KeySchemaElement.builder()
                            .attributeName( columnNameForIndex )
                            .keyType( KeyType.HASH )
                            .build()
            )
            .projection( Projection.builder().projectionType( ProjectionType.ALL ).build() )
            .provisionedThroughput( createProvisionedThroughput( 1, 1 ) );
        if ( indexModifier != null ) {
            indexModifier.accept( indexUpdareRequestBuilder );
        }
        UpdateTableRequest.Builder tableUpdateRequest = UpdateTableRequest.builder()
                .tableName( key.getTableName() )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                            .attributeName( columnNameForIndex )
                            .attributeType( datatype.getScalarAttributeType() )
                            .build(),
                        AttributeDefinition.builder()
                                .attributeName( key.getColumnName() )
                                .attributeType( DynamodbDatatype.STRING.getScalarAttributeType() )
                                .build()
                )
                .globalSecondaryIndexUpdates( Collections.singletonList(
                        GlobalSecondaryIndexUpdate.builder().create( indexUpdareRequestBuilder.build() ).build() )
                );
        if ( tableUpdateModifier != null ) {
            tableUpdateModifier.accept( tableUpdateRequest );
        }
        long time = System.currentTimeMillis();
        try {
            writeLock.lock();
            UpdateTableResponse updateTableResponse = dynamoDbClient.updateTable( tableUpdateRequest.build() );
            do {
                WaiterResponse<DescribeTableResponse> waiterResponse =
                        dynamoDbClient.waiter().waitUntilTableExists( createDescribeTableRequest( key.getTableName(), null ) );
                Optional<DescribeTableResponse> response = waiterResponse.matched().response();
                if ( response.isPresent() && response.get().table().hasGlobalSecondaryIndexes() ) {
                    Optional<GlobalSecondaryIndexDescription> idxDescription = response.get().table().globalSecondaryIndexes()
                            .stream()
                            .filter( idxDesc -> idxDesc.indexName().equals( indexName ) && idxDesc.indexStatus() == IndexStatus.ACTIVE )
                            .findFirst();
                    if ( idxDescription.isPresent() ) {
                        log.info( "Index '{}' on table '{}' is created & active in {} ms", indexName, key.getTableName(), System.currentTimeMillis() - time );
                        break;
                    }
                    log.debug( "Index '{}' on table '{}' is being created...", indexName, key.getTableName() );
                    try {
                        TimeUnit.MILLISECONDS.sleep( 100 );
                    } catch ( InterruptedException e ) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException( "Interruption detected" );
                    }
                }
            } while ( true );
            return updateTableResponse;
        } finally {
            writeLock.unlock();
        }
    }

    @API
    public UpdateTableResponse deleteIndex( String tableName, String indexName ) {
        Objects.requireNonNull( tableName );
        Objects.requireNonNull( indexName );

        GlobalSecondaryIndexUpdate.Builder indexUpdateRequest = GlobalSecondaryIndexUpdate.builder();
        indexUpdateRequest.delete( DeleteGlobalSecondaryIndexAction.builder().indexName( indexName ).build() );
        UpdateTableRequest tableUpdateRequest = UpdateTableRequest.builder()
                .tableName( tableName )
                .globalSecondaryIndexUpdates( Collections.singletonList( indexUpdateRequest.build() ) )
                .build();
        long time = System.currentTimeMillis();
        try {
            writeLock.lock();
            UpdateTableResponse updateTableResponse = dynamoDbClient.updateTable( tableUpdateRequest );
            do {
                WaiterResponse<DescribeTableResponse> waiterResponse =
                        dynamoDbClient.waiter().waitUntilTableExists( createDescribeTableRequest( tableName, null ) );
                Optional<DescribeTableResponse> response = waiterResponse.matched().response();
                if( response.isPresent() ) {
                    if ( !response.get().table().hasGlobalSecondaryIndexes() ) {
                        log.info( "Index '{}' on table '{}' is deleted in {} ms", indexName, tableName, System.currentTimeMillis() - time );
                        break;
                    }
                    Optional<GlobalSecondaryIndexDescription> idxDescription = response.get().table().globalSecondaryIndexes()
                            .stream()
                            .filter( idxDesc -> idxDesc.indexName().equals( indexName ) )
                            .findFirst();
                    if ( idxDescription.isEmpty() ) {
                        log.info( "Index '{}' on table '{}' is deleted in {} ms", indexName, tableName, System.currentTimeMillis() - time );
                        break;
                    }
                    log.debug( "Index '{}' on table '{}' is being deleted...", indexName, tableName );
                    try {
                        TimeUnit.MILLISECONDS.sleep( 100 );
                    } catch ( InterruptedException e ) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException( "Interruption detected" );
                    }
                }
            } while ( true );
            return updateTableResponse;
        } finally {
            writeLock.unlock();
        }
    }

    private static DescribeTableRequest createDescribeTableRequest( String tableName, DescribeTableResponseModifier modifier ) {
        Objects.requireNonNull( tableName );
        DescribeTableRequest.Builder request = DescribeTableRequest.builder().tableName( tableName );
        if ( modifier != null ) modifier.accept( request );
        return request.build();
    }

    private static DeleteTableRequest createDeleteTableRequest( String tableName ) {
        Objects.requireNonNull( tableName );
        return DeleteTableRequest.builder()
            .tableName( tableName )
            .build();
    }

}
