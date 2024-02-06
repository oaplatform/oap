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
import lombok.Setter;
import oap.storage.dynamo.client.DynamodbClient;
import oap.storage.dynamo.client.crud.OperationType;
import oap.storage.dynamo.client.crud.AbstractOperation;
import oap.storage.dynamo.client.crud.DynamoDbHelper;
import oap.storage.dynamo.client.modifiers.KeysAndAttributesModifier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadBatchOperationHelper extends DynamoDbHelper implements DynamodbReadBatch {
    public static final int READ_MAX_BATCH_SIZE = 100;
    private final DynamodbClient client;

    @Setter
    private int batchSize = READ_MAX_BATCH_SIZE;
    protected List<OperationsHolder> operations = new ArrayList<>();


    public ReadBatchOperationHelper( DynamodbClient client ) {
        this.client = client;
    }

    @Override
    public void addOperations( List<AbstractOperation> operationsToAdd ) {
        operationsToAdd.forEach( this::addOperation );
    }

    @Override
    public void addOperation( AbstractOperation operation ) {
        List<AbstractOperation> toBeAdded = new ArrayList<>();
        if ( operations.isEmpty() ) {
            operations.add( new OperationsHolder( toBeAdded, false ) );
        } else {
            toBeAdded = operations.get( operations.size() - 1 ).getOperations();
        }
        if ( toBeAdded.size() >= batchSize ) {
            toBeAdded = new ArrayList<>();
            operations.add( new OperationsHolder( toBeAdded, false ) );
        }
        toBeAdded.add( operation );
    }


    public Map<String, Collection<Map<String, AttributeValue>>> read( KeysAndAttributesModifier modifier ) {
        Multimap<String, Map<String, AttributeValue>> result = ArrayListMultimap.create();
        operations.forEach( oh -> {
            final Multimap<String, Map<String, AttributeValue>> keysByTables = ArrayListMultimap.create();
            oh.operations.forEach( operation -> {
                if ( operation.getType() != OperationType.READ ) {
                    throw new UnsupportedOperationException( "only READ operation is supported" );
                }
                operation.setState( DynamodbClient.State.SUCCESS );
                String tableName = operation.getKey().getTableName();
                keysByTables.put( tableName, getKeyAttribute( operation.getKey() ) );
            } );

            Map<String, KeysAndAttributes> requestItems = new HashMap<>();
            keysByTables.asMap().forEach( ( tn, keys ) -> {
                KeysAndAttributes.Builder builder = KeysAndAttributes.builder().keys( keys );
                if ( modifier != null ) {
                    modifier.accept( builder );
                }
                requestItems.put( tn, builder.build() );
            } );
            BatchGetItemRequest.Builder batchGetItemRequest = BatchGetItemRequest.builder().requestItems( requestItems );
            client.readBatch( batchGetItemRequest, null ).forEach( result::putAll );
        } );
        return result.asMap();
    }
}
