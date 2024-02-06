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

package oap.storage.dynamo.client.batch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import oap.storage.dynamo.client.DynamodbClient;
import oap.storage.dynamo.client.Key;
import oap.storage.dynamo.client.crud.OperationType;
import oap.storage.dynamo.client.annotations.API;
import oap.storage.dynamo.client.convertors.DynamodbDatatype;
import oap.storage.dynamo.client.crud.AbstractOperation;
import oap.storage.dynamo.client.crud.DynamoDbHelper;
import oap.util.Result;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Splits any given list to not more than 25 items, reordering them to have updateItem extracted,
 * then recreates number of [BatchWriteItem(up to 25)|updateItem...|BatchWriteItem|updateItem...|...]
 * then calls appropriate method on client.
 *
 * Note: you cannot put and delete the same item in the same BatchWriteItem request (they must be in different OperationHolder chunks).
 * Note: There are more than 25 requests in the batch.
 * Note: Any individual item in a batch exceeds 400 KB.
 * Note: The total request size exceeds 16 MB.
 *
 * Usage:
 *  BatchOperationHelper helper = new BatchOperationHelper(...);
 *  helper.addOperation(...);
 *  helper.addOperation(...);
 *  helper.write();
 *
 */
@NotThreadSafe
@Slf4j
public class WriteBatchOperationHelper extends DynamoDbHelper implements DynamodbWriteBatch {
    public static final int WRITE_MAX_BATCH_SIZE = 25;
    private final DynamodbClient client;

    private int batchSize = WRITE_MAX_BATCH_SIZE;
    protected List<OperationsHolder> operations = new ArrayList<>();

    public WriteBatchOperationHelper( DynamodbClient client ) {
        this.client = client;
    }

    @API
    public void setBatchSize( int batchSize ) {
        if( batchSize <= 0 || batchSize > WRITE_MAX_BATCH_SIZE ) {
            return;
        }
        this.batchSize = batchSize;
    }

    @API
    public void addOperations( List<AbstractOperation> notArrangedOperations ) {
        if( notArrangedOperations == null || notArrangedOperations.isEmpty() ) return;
        notArrangedOperations.forEach( this::addOperation );
    }

    @API
    public void addOperation( AbstractOperation operation ) {
        if( operation.getType() == OperationType.UPDATE ) {
            operations.add( new OperationsHolder( Collections.singletonList( operation ), true ) );
            return;
        }
        OperationsHolder lastPiece = new OperationsHolder( new ArrayList<>(), false );
        if( !operations.isEmpty() ) {
            //last op - update?
            lastPiece = operations.remove( operations.size() - 1 );
            if ( needNewPiece( lastPiece.operations ) ) {
                operations.add( lastPiece );
                lastPiece = new OperationsHolder( new ArrayList<>(), false );
            }
        }
        operations.add( lastPiece );
        lastPiece.operations.add( operation );
    }

    private boolean needNewPiece( List<AbstractOperation> lastPiece ) {
        return lastPiece.get( lastPiece.size() - 1 ).getType() == OperationType.UPDATE
               || lastPiece.size() >= batchSize;
    }

    @API
    public boolean write() {
        Objects.requireNonNull( client, "You should pass valid DynamodbClient in constructor before calling" );
        operations = splitIfNeeded( operations, new AtomicBoolean( true ) );
        for( OperationsHolder batch : operations ) {
            if( batch.updateOperation ) {
                //updates are not in batch in DynamoDB
                batch.operations.forEach( operation -> {
                    operation.setState( DynamodbClient.State.SUCCESS );
                    Result<UpdateItemResponse, DynamodbClient.State> stateResult = client.update( operation.getKey(), operation.getBinNamesAndValues(), null );
                    if ( !stateResult.isSuccess() ) operation.setState( stateResult.failureValue );
                } );
                continue;
            }
            List<AbstractOperation> allOperations = new ArrayList<>( batch.getOperations() );
            //batch is allowed only for create/delete in DynamoDB
            //create a batch request
            Multimap<String, WriteRequest> writesByTables = ArrayListMultimap.create();
            batch.operations.forEach( operation -> {
                if ( operation.getType() == OperationType.READ ) {
                    throw new UnsupportedOperationException( "only CREATE,UPDATE,DELETE operations are supported" );
                }
                operation.setState( DynamodbClient.State.SUCCESS );
                String tableName = operation.getKey().getTableName();
                Map<String, AttributeValue> binNamesAndValues = new HashMap<>();
                //create an id
                Map<String, AttributeValue> keyAttribute = getKeyAttribute( operation.getKey() );
                binNamesAndValues.putAll( keyAttribute );

                WriteRequest.Builder writeRequest = WriteRequest.builder();
                if( operation.getType() == OperationType.CREATE ) {
                    operation.getBinNamesAndValues().forEach( ( binName, binValue ) -> {
                        binNamesAndValues.put( binName, DynamodbDatatype.createAttributeValueFromObject( binValue ) );
                    } );
                    operation.setPutRequest( PutRequest.builder().item( binNamesAndValues ).build() );
                    writeRequest.putRequest( operation.getPutRequest() );
                } else {
                    operation.setDeleteRequest( DeleteRequest.builder().key( keyAttribute ).build() );
                    writeRequest.deleteRequest( operation.getDeleteRequest() );
                }
                writesByTables.put( tableName, writeRequest.build() );
            } );
            BatchWriteItemResponse response = client.writeBatch( BatchWriteItemRequest.builder().requestItems( writesByTables.asMap() ), null );
            int maxErrorRetries = client.maxErrorRetries;
            while( maxErrorRetries > 0 && response.hasUnprocessedItems() && !response.unprocessedItems().isEmpty() ) {
                response = client.writeBatchUnprocessed( response.unprocessedItems() );
                log.debug( "Retrying to batch write: {}", batch );
            }
            if ( response.hasUnprocessedItems() && !response.unprocessedItems().isEmpty() ) {
                Set<PutRequest> failedPutRequests = new HashSet<>();
                Set<DeleteRequest> failedDeleteRequests = new HashSet<>();
                for ( Map.Entry<String, List<WriteRequest>> entry : response.unprocessedItems().entrySet() ) {
                    entry.getValue().stream().forEach( wr -> {
                        if ( wr.deleteRequest() != null ) failedDeleteRequests.add( wr.deleteRequest() );
                        if ( wr.putRequest() != null ) failedPutRequests.add( wr.putRequest() );
                    } );
                }
                Optional<AbstractOperation> op = allOperations.stream()
                        .filter( o -> failedDeleteRequests.contains( o.getDeleteRequest() ) || failedPutRequests.contains( o.getPutRequest() ) )
                        .findAny();
                if ( !op.isPresent() ) {
                    throw new IllegalStateException( "put/delete operation was not found" );
                }
                op.get().setState( DynamodbClient.State.ERROR );
            }
        }
        return true;
    }

    /**
     * The DynamoDB refuses to proceed batch command if it contains both CREATE and DELETE is single batch.
     * We needtosplit them im different batches.
     * @param operations
     */
    private List<OperationsHolder> splitIfNeeded( List<OperationsHolder> operations, AtomicBoolean wasSplit ) {
        if ( !wasSplit.get() ) return operations;
        wasSplit.set( false );
        List<OperationsHolder> result = new ArrayList<>( operations.size() );
        operations.forEach( oh -> {
            if ( oh.updateOperation ) {
                result.add( oh );
                return;
            }
            boolean atLeastOnceSplitted = false;
            Set<Key> keys = new LinkedHashSet<>();
            for( int i = 0; i < oh.operations.size(); i++ ) {
                AbstractOperation operation = oh.operations.get( i );
                if ( !keys.add( operation.getKey() ) ) {
                    //split
                    OperationsHolder ohLeft = new OperationsHolder( oh.operations.subList( 0, i ), false );
                    OperationsHolder ohRight = new OperationsHolder( oh.operations.subList( i, oh.operations.size() ), false );
                    result.add( ohLeft );
                    result.add( ohRight );
                    keys.clear();
                    atLeastOnceSplitted = true;
                }
            }
            if ( !atLeastOnceSplitted ) {
                result.add( oh );
            }
            wasSplit.set( wasSplit.get() | atLeastOnceSplitted );
        } );
        if ( wasSplit.get() ) return splitIfNeeded( result, wasSplit );
        return result;
    }

    @Override
    public String toString() {
        return operations.toString();
    }

    public List<OperationsHolder> getOperations() {
        return new ArrayList<>( operations );
    }
}
