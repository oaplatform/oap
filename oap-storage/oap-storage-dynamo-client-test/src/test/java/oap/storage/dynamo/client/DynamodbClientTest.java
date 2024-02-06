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

import com.google.common.base.Strings;
import com.google.common.collect.MapDifference;
import oap.application.Kernel;
import oap.application.module.Module;
import oap.storage.dynamo.client.convertors.DynamodbDatatype;
import oap.storage.dynamo.client.creator.PojoBeanToDynamoCreator;
import oap.storage.dynamo.client.creator.samples.Autonomious;
import oap.storage.dynamo.client.creator.samples.AutonomiousDynamo;
import oap.storage.dynamo.client.creator.samples.BeanWithRestrictedField;
import oap.storage.dynamo.client.creator.samples.CompositeBean;
import oap.storage.dynamo.client.creator.samples.EmbeddedBean;
import oap.storage.dynamo.client.fixtures.AbstractDynamodbFixture;
import oap.storage.dynamo.client.fixtures.TestContainerDynamodbFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Sets;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static oap.testng.Asserts.pathOfResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testng.Assert.assertNotNull;

public class DynamodbClientTest extends Fixtures {
    private String tableName = "tableForTestClient";
    private final String keyName = "longId";
    private final String longId = Strings.repeat( "1", 8000 );
    private final AbstractDynamodbFixture fixture = new TestContainerDynamodbFixture();
    private static Kernel kernel;

    public DynamodbClientTest() {
        fixture( fixture );
    }

    @BeforeClass
    public void setUp() {
        kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() );
        kernel.start( pathOfResource( DynamodbAtomicUpdateTest.class, "/oap/storage/dynamo/client/test-application.conf" ) );
        System.setProperty( "TMP_PATH", TestDirectoryFixture.testDirectory().toAbsolutePath().toString().replace( '\\', '/' ) );
    }

    @AfterClass
    public void tearDown() {
        kernel.stop();
    }

    @Test
    public void testClient() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( tableName );
        client.createTable( tableName, 2, 1, keyName, "S", null, null, null );

        client.update( new Key( tableName, keyName, longId ), "v1", false );
        client.update( new Key( tableName, keyName, longId ), "v1", true );
        client.update( new Key( tableName, keyName, longId ), "v2", 1 );
        client.update( new Key( tableName, keyName, longId ), "v2", "str" );

        var ret = client.getRecord( new Key( tableName, keyName, longId ), r -> r.projectionExpression( "v1,v2" ) );

        assertThat( ret.successValue.get( "v1" ).bool() ).isTrue();
        assertThat( ret.successValue.get( "v2" ).s() ).isEqualTo( "str" );

        assertThat( client.tableExists( tableName ) ).isTrue();
        var records = client.getRecord( tableName, Sets.of(
            new Key( tableName, keyName, longId ),
            new Key( tableName, keyName, longId ) ), Sets.of( "v1" ) );
        assertThat( records.successValue.size() ).isEqualTo( 1 );
        assertThat( records.successValue.get( 0 ).get( "v1" ).bool() ).isTrue();
        assertThat( records.successValue.get( 0 ).get( "v1" ).bool() ).isTrue();

        var ret2 = client.getRecord( new Key( tableName, keyName, longId ), r -> r.projectionExpression( "v2,v3" ) );
        assertThat( ret2.successValue.get( "v1" ) ).isNull();
        assertThat( ret2.successValue.get( "v2" ).s() ).isEqualTo( "str" );
        assertThat( ret2.successValue.get( "v3" ) ).isNull();
        client.recreateTable( tableName, keyName );

        ret = client.getRecord( new Key( tableName, keyName, longId ), null );
        assertThat( ret.failureValue ).isEqualTo( DynamodbClient.State.NOT_FOUND );

        client.deleteTableIfExists( tableName );
    }

    @Test
    public void testStartWithoutDynamodb() {
        var kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath() );
        try {
            assertThatCode( () -> kernel.start( pathOfResource( getClass(), "/oap/storage/dynamo/client/test-application.conf" ) ) )
                .doesNotThrowAnyException();
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testGetSets() throws IOException {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();

        client.deleteTable( "setOne" );
        client.deleteTable( "setTwo" );

        client.createTable( "setOne", 2, 1, keyName, "S", null, null, null );
        client.createTable( "setTwo", 2, 1, keyName, "S", null, null, null );

        client.update( new Key( "setOne", "longId", "id1" ), "b1", "v1" );
        client.update( new Key( "setTwo", "longId", "id1" ), "b1", "v1" );
        assertThat( client.getTables().successValue ).contains( "setOne", "setTwo" );
    }

    @Test
    public void testGenerationBins() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.createTableIfNotExist( tableName, keyName );

        client.update( new Key( tableName, keyName, "id1" ), Map.of(
            "b1", "v1",
            "b2", "v2",
            "b3", "v3" ), null );

        var record = client.getRecord( new Key( tableName, keyName, "id1" ), r -> r.projectionExpression( "b1,b3" ) );

        assertThat( record.getSuccessValue().get( "b1" ).s() ).isEqualTo( "v1" );
        assertThat( record.getSuccessValue().get( "b2" ) ).isNull();
        assertThat( record.getSuccessValue().get( "b3" ).s() ).isEqualTo( "v3" );
    }

    @Test
    // "write with our implementation, read with AWS DynamoDB SDK"
    public void crossWriteTest() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( "crossTable" );
        client.createTableIfNotExist( "crossTable", "id" );

        Key key = new Key( "crossTable", "id", "123456" );
        Autonomious auto = new Autonomious( "finalVarValue" );
        auto.setNumberVar( Math.PI );
        auto.setBooleanVar( true );
        auto.setBytesVar( "abracadabra".getBytes( StandardCharsets.UTF_8 ) );
        auto.setDoubleVar( Math.E );
        auto.setFloatVar( ( float ) 1.123456789 );
        auto.setId( key.getColumnValue() );
        auto.setNumberVar( 987654321 );
        auto.setIntVar( 123 );
        auto.setLongVar( 1234567890L );
        auto.setListOfBinaries( Lists.of(
            "A".getBytes( StandardCharsets.UTF_8 ),
            "B".getBytes( StandardCharsets.UTF_8 ),
            "C".getBytes( StandardCharsets.UTF_8 ) ) );
        auto.setListOfIntegers( Lists.of( 1, 2, 3, 4, 5 ) );
        auto.setListOfStrings( Lists.of( "Audi", "BMW", "Renault", "Toyota" ) );
        auto.setMapOfObjects( Maps.of(
            new Pair<>( "One", 1 ),
            new Pair<>( "Two", "2" ),
            new Pair<>( "Three", "3".getBytes( StandardCharsets.UTF_8 ) ),
            new Pair<>( "Four", Lists.of( "4", "Four", "Fier" ) )
        ) );

        client.updateOrCreateItem( key, auto, null );

        var record = client.getRecord( key, r -> r.projectionExpression( "listOfBinaries,listOfStrings" ) );
        assertThat( record.getSuccessValue().size() ).isEqualTo( 2 );
        assertThat( record.getSuccessValue().get( "listOfStrings" ) ).isEqualTo( AttributeValue.fromSs( auto.getListOfStrings() ) );
    }

    @Test
    // "write with AWS DynamoDB SDK, read with our implementation"
    public void crossReadTest() throws Exception {
        var client = fixture.getDynamodbClient();
        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( "crossTable" );
        client.createTableIfNotExist( "crossTable", "id" );

        Key key = new Key( "crossTable", "id", "123456" );
        AutonomiousDynamo auto = new AutonomiousDynamo( null ); // AWS DynamoDB SDK DB does not support final variables
        auto.setNumberVar( Math.PI );
        auto.setBooleanVar( true );
        auto.setBytesVar( "abracadabra".getBytes( StandardCharsets.UTF_8 ) );
        auto.setDoubleVar( Math.E );
        auto.setFloatVar( ( float ) 1.123456789 );
        auto.setId( key.getColumnValue() );
        auto.setNumberVar( 987654321.0 );
        auto.setIntVar( 123 );
        auto.setLongVar( 1234567890L );
        auto.setListOfBinaries( Lists.of(
            "A".getBytes( StandardCharsets.UTF_8 ),
            "B".getBytes( StandardCharsets.UTF_8 ),
            "C".getBytes( StandardCharsets.UTF_8 ) ) );
        auto.setListOfIntegers( Lists.of( 1, 2, 3, 4, 5 ) );
        auto.setListOfStrings( Lists.of( "Audi", "BMW", "Renault", "Toyota" ) );
        auto.setMapOfObjects( Maps.of(
            new Pair<>( "One", Lists.of( "1" ) ),
            new Pair<>( "Two", Lists.of( "2" ) ),
            new Pair<>( "Three", Lists.of( "3" ) ),
            new Pair<>( "Four", Lists.of( "4", "Four", "Fier" ) )
        ) );

        Map<String, AttributeValue> attributes = new PojoBeanToDynamoCreator<>().fromDynamo( auto );

        client.updateRecord( key, attributes, null );

        var record = client.getItem( AutonomiousDynamo.class, key, null );
        assertThat( record.getSuccessValue() ).isNotNull();
        //compare maps
        MapDifference mapDifference = com.google.common.collect.Maps.difference( auto.getMapOfObjects(), record.getSuccessValue().getMapOfObjects() );
        assertThat( mapDifference.areEqual() ).isTrue();
        auto.setMapOfObjects( null );
        record.getSuccessValue().setMapOfObjects( null );
        //compare lists of bytes
        assertThat( auto.getListOfBinaries().get( 0 ) ).isEqualTo( record.getSuccessValue().getListOfBinaries().get( 0 ) );
        assertThat( auto.getListOfBinaries().get( 1 ) ).isEqualTo( record.getSuccessValue().getListOfBinaries().get( 1 ) );
        assertThat( auto.getListOfBinaries().get( 2 ) ).isEqualTo( record.getSuccessValue().getListOfBinaries().get( 2 ) );
        auto.setListOfBinaries( null );
        record.getSuccessValue().setListOfBinaries( null );
        //compare entities
        assertThat( record.getSuccessValue() ).isEqualTo( auto );
    }

    @Test
    public void testFindAndModify() throws IOException {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( tableName );
        client.createTableIfNotExist( tableName, keyName );

        Key key = new Key( tableName, keyName, "id1" );

        assertThat( modifyAndGet( client, key, "b1", 1L ).n() ).isEqualTo( "1" );

        var record = client.getRecord( key, null );
        assertNotNull( record );

        assertThat( client.findAndModify( key, r -> {
            AttributeValue currentValue = r.get( "b1" );
            assertThat( currentValue.n() ).isEqualTo( "1" );
            r.put( "b1", DynamodbDatatype.createAttributeValueFromObject( Long.parseLong( currentValue.n() ) + 1 ) );
        }, builder -> builder.projectionExpression( "b1,b2" ) ).get( "b1" ).n() ).isEqualTo( "2" );

        assertThat( client.findAndModify( key, r -> {
            AttributeValue currentValue = r.get( "b1" );
            assertThat( currentValue.n() ).isEqualTo( "2" );
            r.put( "b1", DynamodbDatatype.createAttributeValueFromObject( Long.parseLong( currentValue.n() ) + 1 ) );
        }, builder -> builder.projectionExpression( "b1,b2" ) ).get( "b1" ).n() ).isEqualTo( "3" );
    }

    @Test
    public void testQueryByScanWithoutIndex() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        tableName = "testScanWithoutIndex";
        client.deleteTableIfExists( tableName );
        client.createTableIfNotExist( tableName, keyName );

        Key key1 = new Key( tableName, keyName, "id1" );
        Key key2 = new Key( tableName, keyName, "id2" );
        Key key3 = new Key( tableName, keyName, "id3" );
        Key key4 = new Key( tableName, keyName, "id4" );
        Key key5 = new Key( tableName, keyName, "id5" );

        assertThat( client.update( key1, "test_bin", 2 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key2, "test_bin", 4 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key3, "test_bin", 1 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key4, "test_bin", 1 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key5, "test_bin", 3 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key3, "aaa", 1 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key4, "aaa", 2 ).getSuccessValue() ).isNotNull();

        var resultFuture = client.getRecordsByScan( tableName,
            r -> r
                .filterExpression( "test_bin = :var1Val" )
                .expressionAttributeValues( Maps.of(
                    new Pair<>( ":var1Val", AttributeValue.fromN( "1" ) )
                ) )
        );
        Map<String, Map<String, AttributeValue>> records = resultFuture.collect( Collectors.toMap( k -> k.get( "longId" ).s(), v -> v ) );
        assertThat( records.size() ).isEqualTo( 2 );
        assertThat( records.get( "id3" ).get( "test_bin" ).n() ).isEqualTo( "1" );
        assertThat( records.get( "id4" ).get( "test_bin" ).n() ).isEqualTo( "1" );
        assertThat( records.get( "id3" ).get( "aaa" ).n() ).isEqualTo( "1" );
        assertThat( records.get( "id4" ).get( "aaa" ).n() ).isEqualTo( "2" );
    }

    private AttributeValue modifyAndGet( DynamodbClient client, Key key, String binName, Long value ) {
        return client.findAndModify(
            key,
            r -> r.put( binName, DynamodbDatatype.createAttributeValueFromObject( value ) ),
            null ).get( binName );
    }

    @Test
    public void compositeBeanTest() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( "compositeTable" );
        client.createTableIfNotExist( "compositeTable", "id" );

        EmbeddedBean embeddedBean = new EmbeddedBean();
        embeddedBean.setEmbeddedFieldString( "emb string" );
        embeddedBean.setEmbeddedFieldInt( 987 );
        CompositeBean compositeBean = new CompositeBean();
        compositeBean.setCompositeFieldString( "comp string" );
        compositeBean.setCompositeFieldBoolean( true );
        compositeBean.setEmbeddedBean( embeddedBean );

        Key key = new Key( "compositeTable", "id", "1" );
        // Map<String, AttributeValue> attributes = new PojoBeanToDynamoCreator().fromDynamo( compositeBean );
        client.updateOrCreateItem( key, compositeBean, null );

        CompositeBean record = client.getItem( CompositeBean.class, key, null ).getSuccessValue();
        assertThat( record ).isEqualTo( compositeBean );
    }

    @Test
    public void restrictedFieldTest() throws Exception {
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( "restrictedFieldTable" );
        client.createTableIfNotExist( "restrictedFieldTable", "id" );

        BeanWithRestrictedField beanWithRestrictedField = new BeanWithRestrictedField( "id", "name" );
        beanWithRestrictedField.setC( 6 );


        Key key = new Key( "restrictedFieldTable", "id", "id" );

        client.updateOrCreateItem( key, beanWithRestrictedField, null );
    }

}
