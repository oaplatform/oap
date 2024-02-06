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

package oap.storage.dynamo.client.atomic;

import oap.storage.dynamo.client.annotations.API;
import oap.storage.dynamo.client.modifiers.UpdateItemRequestModifier;
import oap.util.HashMaps;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@API
public class AtomicUpdateRecordSupporter implements UpdateItemRequestModifier {

    private AtomicUpdateFieldAndValue atomicUpdateFieldAndValue = new AtomicUpdateFieldAndValue( 0 );
    private final Map<String, AttributeValue> atomicUpdates = new LinkedHashMap<>();

    public AtomicUpdateRecordSupporter() {
    }

    public void addAtomicUpdateFor( String binName, AttributeValue binValue ) {
        Objects.requireNonNull( binName );
        Objects.requireNonNull( binValue );
        atomicUpdates.put( binName, binValue );
    }

    /**
     * Prepares UpdateItemRequest for updating a record in DynamoDB. It also adds a feature to support read/check/write
     * functionality with #recordVersionColumnName field (a.k.a. generation or version). If a record in DynamoDB
     * has such field, the given value ('generation') should be equal to DynamoDB field value, otherwise
     * update will fail with ConditionalCheckFailedException
     * @param builder an UpdateItemRequest.Builder to modify
     * Note: generation version number of a record to be equal in order to make update happen
     */
    @Override
    public void accept( UpdateItemRequest.Builder builder ) {
        Objects.requireNonNull( builder );
        StringBuilder toSetExpression = new StringBuilder();
        Map<String, String> expressionAttributeNames = HashMaps.of( "#gen", atomicUpdateFieldAndValue.getFieldName() );
        Map<String, AttributeValue> expressionAttributeValues = HashMaps.of(
                ":inc", AttributeValue.fromN( "1" ),
                ":null", AttributeValue.fromNul( true ),
                ":gen", AttributeValue.fromN( String.valueOf( atomicUpdateFieldAndValue.getValue() ) ) );

        int counter = 0;
        for ( Map.Entry<String, AttributeValue> atomicUpdate :  atomicUpdates.entrySet() ) {
            toSetExpression.append( "#var" ).append( counter ).append( " = :var" ).append( counter ).append( ", " );
            expressionAttributeNames.put( "#var" + counter, atomicUpdate.getKey() );
            expressionAttributeValues.put( ":var" + counter, atomicUpdate.getValue() );
            counter++;
        }
        if ( toSetExpression.length() == 0 ) {
            throw new IllegalArgumentException( "You have not added any atomic update instruction via #addAtomicUpdateFor method" );
        }
        toSetExpression.setLength( toSetExpression.length() - ", ".length() );
        builder
                .attributeUpdates( null ) //these (old, obsolete) updates are not compatible with given pairs
                .conditionExpression( "attribute_not_exists(#gen) OR (attribute_exists(#gen) AND (#gen = :gen OR #gen = :null))" )
                .updateExpression( "SET " + toSetExpression + " ADD #gen :inc" )
                .expressionAttributeNames( expressionAttributeNames )
                .expressionAttributeValues( expressionAttributeValues );
    }

    public void setAtomicUpdateFieldAndValue( AtomicUpdateFieldAndValue fieldAndValue ) {
        Objects.requireNonNull( fieldAndValue );
        this.atomicUpdateFieldAndValue = fieldAndValue;
    }
}
