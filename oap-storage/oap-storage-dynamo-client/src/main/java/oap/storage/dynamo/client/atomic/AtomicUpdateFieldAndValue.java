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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import oap.storage.dynamo.client.DynamodbClient;
import oap.storage.dynamo.client.annotations.API;
import oap.storage.dynamo.client.restrictions.ReservedWords;
import oap.util.Result;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@EqualsAndHashCode
@ToString
@API
public class AtomicUpdateFieldAndValue {
    public static final String DEFAULT_NAME = "generation";

    @Getter
    private final String fieldName;
    @Getter
    private final long value;
    /**
     * This function is called when retry happens.
     */
    @Setter
    private Consumer<Exception> onRetry = null;
    /**
     * This function is called when all retry attempts failed.
     */
    @Setter
    private Consumer<Exception> onExhaustedRetry = null;

    public AtomicUpdateFieldAndValue( long value ) {
        this( DEFAULT_NAME, value );
    }

    public AtomicUpdateFieldAndValue( AtomicUpdateFieldAndValue toCopy, long value ) {
        this( Objects.requireNonNull( toCopy ).fieldName, value, toCopy.onRetry );
        this.onExhaustedRetry = toCopy.onExhaustedRetry;
    }

    public AtomicUpdateFieldAndValue( String fieldName, long value ) {
        this( fieldName, value, null );
    }

    public AtomicUpdateFieldAndValue( String fieldName, long value, Consumer<Exception> onRetry ) {
        Objects.requireNonNull( fieldName );
        if( !ReservedWords.isAttributeNameAppropriate( fieldName ) ) {
          throw new IllegalArgumentException( "Column '" + fieldName + "' is prohibited in DynamoDB" );
        }
        this.fieldName = fieldName;
        this.value = Math.max( value, 0 );
        this.onRetry = onRetry;
    }

    public String getValueFromAtomicUpdate( Result<UpdateItemResponse, DynamodbClient.State> result ) {
        if ( result == null || !result.isSuccess() ) throw new IllegalArgumentException();
        return result.getSuccessValue().attributes().get( fieldName ).n();
    }

    public String getValueFromRecord( Result<Map<String, AttributeValue>, DynamodbClient.State> result ) {
        if ( result == null || !result.isSuccess() ) throw new IllegalArgumentException();
        return result.getSuccessValue().get( fieldName ).n();
    }

    public Void onRetry( Exception ex ) {
        if ( onRetry == null ) return null;
        onRetry.accept( ex );
        return null;
    }

    public Void onExhaustedRetryAttempts() {
        if ( onExhaustedRetry == null ) return null;
        onExhaustedRetry.accept( null );
        return null;
    }
}
