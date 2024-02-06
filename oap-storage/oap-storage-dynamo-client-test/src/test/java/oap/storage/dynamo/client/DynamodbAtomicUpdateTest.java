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
import oap.testng.Fixtures;
import oap.util.HashMaps;
import oap.util.Result;
import oap.util.Sets;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class DynamodbAtomicUpdateTest extends Fixtures {
    public static final String TABLE_NAME = "atomicUpdateTest";
    public static final String ID_COLUMN_NAME = "id";
    private static final AtomicUpdateFieldAndValue parsingHelper = new AtomicUpdateFieldAndValue( "serializedRecordVersion", Integer.MAX_VALUE );

    private final AbstractDynamodbFixture fixture = new TestContainerDynamodbFixture();

    public DynamodbAtomicUpdateTest() {
        fixture( fixture );
    }

    @Test
    public void atomicUpdateShouldInitializeField() {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( TABLE_NAME );
        client.createTableIfNotExist( TABLE_NAME, ID_COLUMN_NAME );
        Key key = new Key( TABLE_NAME, ID_COLUMN_NAME, "IFA" );
        client.updateRecordAtomic(
            key,
            Collections.singletonMap( "vvvv", AttributeValue.fromS( "vvv" ) ),
            null,
            new AtomicUpdateFieldAndValue( "recVersion", -1 ) );

        Result<Map<String, AttributeValue>, DynamodbClient.State> result = client.getRecord( key, null );
        assertThat( result.isSuccess() ).isTrue();
        //first update is 0 + 1 = 1
        assertThat( result.getSuccessValue().get( "recVersion" ).n() ).isEqualTo( "1" );
    }

    @Test
    public void atomicUpdateShouldIncrementItsField() {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( TABLE_NAME );
        client.createTableIfNotExist( TABLE_NAME, ID_COLUMN_NAME );
        Key key = new Key( TABLE_NAME, ID_COLUMN_NAME, "IFA" );

        AtomicUpdateFieldAndValue serializedRecordVersion = new AtomicUpdateFieldAndValue( parsingHelper, 20 );
        client.updateRecordAtomic(
            key,
            Collections.singletonMap( "vvvv", AttributeValue.fromS( "vvv" ) ),
            null,
            serializedRecordVersion );

        Result<Map<String, AttributeValue>, DynamodbClient.State> result = client.getRecord( key, null );
        assertThat( parsingHelper.getValueFromRecord( result ) ).isEqualTo( "1" );
        serializedRecordVersion = new AtomicUpdateFieldAndValue( parsingHelper, 1 );
        client.updateRecordAtomic(
            key,
            Collections.singletonMap( "vvvv", AttributeValue.fromS( "vvv" ) ),
            null,
            serializedRecordVersion );

        result = client.getRecord( key, null );
        assertThat( parsingHelper.getValueFromRecord( result ) ).isEqualTo( "2" );
    }

    @Test
    public void atomicUpdateShouldSkipUpdateIfVersionDoesNotFit() {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( TABLE_NAME );
        client.createTableIfNotExist( TABLE_NAME, ID_COLUMN_NAME );
        Key key = new Key( TABLE_NAME, ID_COLUMN_NAME, "IFA" );

        assertThat( client.updateRecordAtomic(
            key,
            HashMaps.of(
                "vvvv", AttributeValue.fromS( "v1" )
            ),
            null,
            new AtomicUpdateFieldAndValue( 39 ) ).isSuccess() ).isTrue(); //any number for first update

        Result<Map<String, AttributeValue>, DynamodbClient.State> result = client.getRecord( key, null );
        assertThat( result.getSuccessValue().get( AtomicUpdateFieldAndValue.DEFAULT_NAME ).n() ).isEqualTo( "1" );

        AtomicUpdateFieldAndValue generation = new AtomicUpdateFieldAndValue( 18 );
        Result<UpdateItemResponse, DynamodbClient.State> resultOfInvalidOperation = client.updateRecordAtomic(
            key,
            HashMaps.of(
                "vvvv", AttributeValue.fromS( "v2" )
            ),
            null,
            generation ); //number is not equal to actual 19, so update should not happen
        assertThat( resultOfInvalidOperation.isSuccess() ).isFalse();
        assertThat( resultOfInvalidOperation.getFailureValue() ).isEqualTo( DynamodbClient.State.VERSION_CHECK_FAILED );

        result = client.getRecord( key, null );
        assertThat( generation.getValueFromRecord( result ) ).isEqualTo( "1" );
        assertThat( result.getSuccessValue().get( "vvvv" ).s() ).isEqualTo( "v1" );
    }

    @Test
    public void atomicUpdatesShouldRetryIfConcurrent() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( TABLE_NAME );
        client.createTableIfNotExist( TABLE_NAME, ID_COLUMN_NAME );
        Key key = new Key( TABLE_NAME, ID_COLUMN_NAME, "IFA" );
        AtomicInteger counter = new AtomicInteger( 0 );
        AtomicInteger innerRetryCounter = new AtomicInteger( 0 );
        AtomicInteger exhaustedRetryCounter = new AtomicInteger( 0 );
        Consumer<Exception> onRetry = ex -> innerRetryCounter.incrementAndGet();
        Consumer<Exception> onExhaustedRetry = ex -> exhaustedRetryCounter.incrementAndGet();
        ExecutorService service = Executors.newFixedThreadPool( 20 );
        for( int i = 0; i < 1000; i++ ) {
            service.submit( () -> {
                AtomicUpdateFieldAndValue genaVal = new AtomicUpdateFieldAndValue( "genaVal", counter.get(), onRetry );
                genaVal.setOnExhaustedRetry( onExhaustedRetry );
                Result<UpdateItemResponse, DynamodbClient.State> result = client.updateRecordAtomicWithRetry(
                    key,
                    Sets.of( "version", "id" ),
                    valueMap -> {
                        Map<String, AttributeValue> res = new HashMap<>( valueMap );
                        res.put( "version", AttributeValue.fromS( "v" + counter.get() ) );
                        return res;
                    },
                    3,
                    genaVal );
                if( result.isSuccess() ) counter.incrementAndGet();
                Result<Map<String, AttributeValue>, DynamodbClient.State> record = client.getRecord( key, null );
                if( !result.isSuccess() )
                    System.err.println( "#" + counter.get() + "/" + innerRetryCounter.get()
                        + " -> " + result.isSuccess()
                        + " (" + record.getSuccessValue().get( "version" ) + "), gen: " + record.getSuccessValue().get( "genaVal" ) );
            } );
        }
        service.shutdown();
        assertThat( service.awaitTermination( 1, TimeUnit.MINUTES ) ).isTrue();
        //2556(2), 4566(5), 7061(10), 10395(20), 18685(100), 28557(500), 38019(2000)
        System.err.println( "Retries: " + innerRetryCounter.get() );
        //859(2), 761(5), 641(10), 495(20), 185(100), 57(500), 19(2000)
        System.err.println( "Exhausted attempts: " + exhaustedRetryCounter.get() );
        Result<Map<String, AttributeValue>, DynamodbClient.State> result = client.getRecord( key, null );
        AtomicUpdateFieldAndValue generation = new AtomicUpdateFieldAndValue( "genaVal", 0 );
        assertThat( Long.parseLong( generation.getValueFromRecord( result ) ) ).isEqualTo( counter.get() );

        client.updateRecordAtomic(
            key,
            Collections.singletonMap( "version", AttributeValue.fromS( "final" ) ),
            null,
            new AtomicUpdateFieldAndValue( counter.get() ) );

        result = client.getRecord( key, null );
        assertThat( Long.parseLong( generation.getValueFromRecord( result ) ) ).isGreaterThanOrEqualTo( counter.get() );
    }
}
