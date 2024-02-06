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

import oap.application.Kernel;
import oap.application.module.Module;
import oap.storage.dynamo.client.fixtures.AbstractDynamodbFixture;
import oap.storage.dynamo.client.DynamodbClient;
import oap.storage.dynamo.client.Key;
import oap.storage.dynamo.client.fixtures.TestContainerDynamodbFixture;
import oap.storage.dynamo.client.crud.AbstractOperation;
import oap.storage.dynamo.client.crud.CreateItemOperation;
import oap.storage.dynamo.client.crud.DeleteItemOperation;
import oap.storage.dynamo.client.crud.OperationType;
import oap.storage.dynamo.client.crud.ReadItemOperation;
import oap.storage.dynamo.client.crud.UpdateItemOperation;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Result;
import oap.util.Sets;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static oap.testng.Asserts.pathOfResource;
import static org.assertj.core.api.Assertions.assertThat;

public class DynamodbBatchOperationHelperTest extends Fixtures {
    private String tableName1 = "batchTable1";
    private String tableName2 = "batchTable2";
    private final String keyName = "longId";

    private final AbstractDynamodbFixture fixture = new TestContainerDynamodbFixture();

    public DynamodbBatchOperationHelperTest() {
        fixture( fixture );
        Kernel kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() );
        kernel.start( pathOfResource( getClass(), "/oap/storage/dynamo/client/test-application.conf" ) );
    }

    @BeforeMethod
    public void beforeMethod() {
        System.setProperty( "TMP_PATH", TestDirectoryFixture.testDirectory().toAbsolutePath().toString().replace( '\\', '/' ) );
    }

    @Test
    public void testBatchOperationCreateAndDeleteInOneBatch() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( tableName1 );

        WriteBatchOperationHelper helper = new WriteBatchOperationHelper( client );
        List<AbstractOperation> operations = Lists.of(
            new CreateItemOperation(
                new Key( tableName1, keyName, "id1" ),
                Maps.of(
                    new Pair<>( "StringColumn", "string value" )
                )
            ),
            new CreateItemOperation(
                new Key( tableName1, keyName, "id2" ),
                Maps.of(
                    new Pair<>( "StringColumn", "string value" )
                )
            ),
            new DeleteItemOperation( new Key( tableName1, keyName, "id2" ) ),
            new DeleteItemOperation( new Key( tableName1, keyName, "id1" ) )
        );
        helper.addOperations( operations );

        //create table
        client.createTable( tableName1, 10, 10, keyName, "S", null, null, null );

        helper.write();
    }

    @Test
    public void testBatchOperation() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( tableName1 );
        client.deleteTableIfExists( tableName2 );

        WriteBatchOperationHelper helper = new WriteBatchOperationHelper( client );
        addOperations( helper );

        //create table
        client.createTable( tableName1, 10, 10, keyName, "S", null, null, null );
        client.createTable( tableName2, 10, 10, keyName, "S", null, null, null );

        helper.write();

        Key key = new Key( tableName1, keyName, "id1" );
        Result<Map<String, AttributeValue>, DynamodbClient.State> id1 = client.getRecord( key, null );

        assertThat( id1.successValue.size() ).isEqualTo( 7 );
        assertThat( id1.successValue.get( "StringColumn" ).s() ).isEqualTo( "string value 1 updated" );
        assertThat( id1.successValue.get( "ByteColumn" ).b().asByteArray() ).isEqualTo( "新機能！かんたんスピード検索".getBytes( StandardCharsets.UTF_8 ) );
        assertThat( id1.successValue.get( "IntColumn" ).n() ).isEqualTo( "1" ); // the value returned as string
        assertThat( id1.successValue.get( "BoolColumn" ).bool() ).isTrue();
        assertThat( id1.successValue.get( "DoubleColumn" ).n() ).isEqualTo( String.valueOf( Math.PI ) );
        assertThat( id1.successValue.get( "StringColumnNull" ).nul() ).isEqualTo( true );

        Result<Map<String, AttributeValue>, DynamodbClient.State> id2 = client.getRecord( new Key( tableName1, keyName, "id2" ), null );
        assertThat( id2.successValue.size() ).isEqualTo( 5 );
        assertThat( id2.successValue.get( "StringColumn" ).s() ).isEqualTo( "string value 2 updated" );
        assertThat( id2.successValue.get( "IntColumn" ).n() ).isEqualTo( "2" ); // the value returned as string
        assertThat( id2.successValue.get( "BoolColumn" ).bool() ).isFalse();
        assertThat( id2.successValue.get( "DoubleColumn" ).n() ).isEqualTo( String.valueOf( Math.E ) );

        assertThat( client.getRecord( new Key( tableName1, keyName, "id3" ), null ).failureValue ).isEqualTo( DynamodbClient.State.NOT_FOUND );
    }

    private void addOperations( WriteBatchOperationHelper helper ) {
        List<AbstractOperation> operationsForTable1 = createOperationsForTable( tableName1, false );
        List<AbstractOperation> operationsForTable2 = createOperationsForTable( tableName2, true );
        addOperationsToHelper( operationsForTable1, operationsForTable2, helper );
    }

    private void addOperationsToHelper( List<AbstractOperation> operationsForTable1,
                                        List<AbstractOperation> operationsForTable2,
                                        WriteBatchOperationHelper helper ) {
        List<AbstractOperation> randomOperations1 = new ArrayList<>( operationsForTable1 );
        List<AbstractOperation> randomOperations2 = new ArrayList<>( operationsForTable2 );
        Random random = new Random();
        operationsForTable1.forEach( o -> {
            if( random.nextDouble() > 0.6 && !randomOperations1.isEmpty() ) {
                helper.addOperation( randomOperations1.remove( 0 ) );
            }
            if( random.nextDouble() > 0.6 && !randomOperations2.isEmpty() ) {
                helper.addOperation( randomOperations2.remove( 0 ) );
            }
        } );
        helper.addOperations( randomOperations1 );
        helper.addOperations( randomOperations2 );
    }

    @NotNull
    private List<AbstractOperation> createOperationsForTable( String tableName, boolean withoutUpdates ) {
        List<AbstractOperation> operations = Lists.of(
            new CreateItemOperation(
                new Key( tableName, keyName, "id1" ),
                Maps.of(
                    new Pair<>( "StringColumn", "string value 1" ),
                    new Pair<>( "StringColumnNull", "string value 1 to be overwritten with null" ),
                    new Pair<>( "IntColumn", 1 ),
                    new Pair<>( "BoolColumn", true ),
                    new Pair<>( "DoubleColumn", Math.PI ),
                    new Pair<>( "ByteColumn", "新機能！かんたんスピード検索".getBytes( StandardCharsets.UTF_8 ) )
                )
            ),
            new CreateItemOperation(
                new Key( tableName, keyName, "id2" ),
                Maps.of(
                    new Pair<>( "StringColumn", "string value 2" ),
                    new Pair<>( "IntColumn", 2 ),
                    new Pair<>( "BoolColumn", false ),
                    new Pair<>( "DoubleColumn", Math.E )
                )
            ),
            new UpdateItemOperation(
                new Key( tableName, keyName, "id1" ),
                Maps.of(
                    new Pair<>( "StringColumn", "string value 1 updated" ),
                    new Pair<>( "StringColumnNull", null )
                )
            ),
            new CreateItemOperation(
                new Key( tableName, keyName, "id3" ),
                Maps.of(
                    new Pair<>( "StringColumn", "to be deleted" )
                )
            ),
            new UpdateItemOperation(
                new Key( tableName, keyName, "id2" ),
                Maps.of(
                    new Pair<>( "StringColumn", "string value 2 updated" )
                )
            ),
            new DeleteItemOperation( new Key( tableName, keyName, "id3" ) )
        );
        if( withoutUpdates ) {
            return operations.stream().filter( operation -> operation.getType() != OperationType.UPDATE ).toList();
        }
        return operations;
    }

    @Test
    public void testBatchRead() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( tableName1 );
        client.deleteTableIfExists( tableName2 );
        client.createTableIfNotExist( tableName1, keyName );
        client.createTableIfNotExist( tableName2, keyName );

        Key id11 = new Key( tableName1, keyName, "id11" );
        Key id12 = new Key( tableName1, keyName, "id12" );
        Key id21 = new Key( tableName2, keyName, "id21" );
        Key id22 = new Key( tableName2, keyName, "id22" );
        Key id23 = new Key( tableName2, keyName, "id23" );
        Key id24 = new Key( tableName2, keyName, "id24" );
        client.update( id11, "record1bin1", "v1" ); //table1
        client.update( id12, "record1bin2", "v2" ); //table1
        client.update( id21, "record2bin1", "v3.1" ); //table2
        client.update( new Key( tableName2, keyName, "id997" ), "record2bin1", "v997" );
        client.update( id22, "record2bin2", "v3.2" ); //table2
        client.update( new Key( tableName2, keyName, "id998" ), "record2bin1", "v998" );
        client.update( id23, "record2bin3", "v3.3" ); //table2
        client.update( id24, "record2bin4", "v3.4" ); //table2
        client.update( new Key( tableName2, keyName, "id999" ), "record2bin1", "v999" );

        ReadBatchOperationHelper helper = new ReadBatchOperationHelper( client );
        helper.setBatchSize( 2 );
        helper.addOperation( new ReadItemOperation( id11, null ) );
        helper.addOperation( new ReadItemOperation( id12, null ) );
        helper.addOperation( new ReadItemOperation( id21, null ) );
        helper.addOperation( new ReadItemOperation( id22, null ) );
        helper.addOperation( new ReadItemOperation( id23, null ) );
        helper.addOperation( new ReadItemOperation( id24, null ) );

        Map<String, Collection<Map<String, AttributeValue>>> result = helper.read(
            r -> r.attributesToGet( Sets.of( keyName, "record1bin1", "record1bin2", "record2bin1", "record2bin2", "record2bin3" ) )
        );

        assertThat( result.size() ).isEqualTo( 2 );
        assertThat( result.get( tableName1 ).size() ).isEqualTo( 2 );
        assertThat( result.get( tableName2 ).size() ).isEqualTo( 4 );
        String rec1val = result.get( tableName1 ).stream().filter( r -> r.get( keyName ).s().equals( "id11" ) ).findAny().get().get( "record1bin1" ).s();
        String rec2val = result.get( tableName1 ).stream().filter( r -> r.get( keyName ).s().equals( "id12" ) ).findAny().get().get( "record1bin2" ).s();
        String rec3val = result.get( tableName2 ).stream().filter( r -> r.get( keyName ).s().equals( "id21" ) ).findAny().get().get( "record2bin1" ).s();
        String rec4val = result.get( tableName2 ).stream().filter( r -> r.get( keyName ).s().equals( "id22" ) ).findAny().get().get( "record2bin2" ).s();
        String rec5val = result.get( tableName2 ).stream().filter( r -> r.get( keyName ).s().equals( "id23" ) ).findAny().get().get( "record2bin3" ).s();
        AttributeValue rec6val = result.get( tableName2 ).stream().filter( r -> r.get( keyName ).s().equals( "id24" ) ).findAny().get().get( "record2bin4" );
        assertThat( rec1val ).isEqualTo( "v1" );
        assertThat( rec2val ).isEqualTo( "v2" );
        assertThat( rec3val ).isEqualTo( "v3.1" );
        assertThat( rec4val ).isEqualTo( "v3.2" );
        assertThat( rec5val ).isEqualTo( "v3.3" );
        assertThat( rec6val ).isNull(); //this item has ignoring bin called as 'record2bin4', so value for it was not read
    }
}
