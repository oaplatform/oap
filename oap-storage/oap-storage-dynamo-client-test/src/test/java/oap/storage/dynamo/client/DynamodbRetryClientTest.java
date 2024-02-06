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


import oap.storage.dynamo.client.atomic.AtomicUpdateFieldAndValue;
import oap.storage.dynamo.client.fixtures.AbstractDynamodbFixture;
import oap.storage.dynamo.client.fixtures.TestContainerDynamodbFixture;
import oap.storage.dynamo.client.modifiers.GetItemRequestModifier;
import oap.storage.dynamo.client.modifiers.UpdateItemRequestModifier;
import oap.testng.Fixtures;
import oap.util.HashMaps;
import oap.util.Result;
import oap.util.Sets;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DynamodbRetryClientTest extends Fixtures {

    public static final String TABLE_NAME = "retryTest";
    public static final String ID_COLUMN_NAME = "id";

    private final AbstractDynamodbFixture fixture = new TestContainerDynamodbFixture();

    public DynamodbRetryClientTest() {
        fixture( fixture );
    }

    private AtomicInteger counter = new AtomicInteger();
    private Map<String, AttributeValue> attributeValueMap = HashMaps.of(
        AtomicUpdateFieldAndValue.DEFAULT_NAME, AttributeValue.fromN( "2" ),
        "bin1", AttributeValue.fromS( "Adam Smith" ),
        "bin2", AttributeValue.fromS( "Samuel Collins" )
    );

    @NotNull
    private DynamodbClient createClient() {
        return new DynamodbClient( fixture.getDynamodbClient().getDynamoDbClient() ) {
            public Result<Map<String, AttributeValue>, State> getRecord( Key key, GetItemRequestModifier modifier ) {
                return Result.success( attributeValueMap );
            }

            @Override
            public Result<UpdateItemResponse, DynamodbClient.State> updateRecordAtomic( Key key, Map<String, AttributeValue> binNamesAndValues, UpdateItemRequestModifier modifier, AtomicUpdateFieldAndValue generation ) {
                counter.incrementAndGet();
                if( counter.get() == 5 ) {
                    attributeValueMap.put( AtomicUpdateFieldAndValue.DEFAULT_NAME, AttributeValue.fromN( "2" ) );
                    attributeValueMap.put( "bin3", AttributeValue.fromS( "v2" ) );
                    return Result.success( UpdateItemResponse.builder().attributes( attributeValueMap ).build() );
                }
                return Result.failure( State.VERSION_CHECK_FAILED );
            }
        };
    }

    @Test
    public void atomicUpdateWithRetry() {
        var client = createClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( TABLE_NAME );
        client.createTableIfNotExist( TABLE_NAME, ID_COLUMN_NAME );
        Key key = new Key( TABLE_NAME, ID_COLUMN_NAME, "Palo Alto, CA" );
        //attempt to write v1, but there actually is v2, so we have to repeat 5 attempts
        Map<String, AttributeValue> attributes = Collections.singletonMap( "bin3", AttributeValue.fromS( "v1" ) );

        AtomicUpdateFieldAndValue generation = new AtomicUpdateFieldAndValue( 1 );
        Result<UpdateItemResponse, DynamodbClient.State> result = client.updateRecordAtomicWithRetry( key,
            Sets.of( "bin1", "bin2" ),
            attributeValueMap -> {
                Map<String, AttributeValue> map = new HashMap<>( attributeValueMap );
                map.putAll( attributes );
                return attributes;
            },
            5,
            generation );

        assertThat( counter.get() ).isEqualTo( 5 );
        assertThat( result.isSuccess() ).isTrue();
        assertThat( result.getSuccessValue().attributes().get( "bin3" ).s() ).isEqualTo( "v2" );
        assertThat( generation.getValueFromAtomicUpdate( result ) ).isEqualTo( "2" );
    }
}
