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

import lombok.Data;
import oap.storage.dynamo.client.convertors.DynamodbDatatype;
import oap.storage.dynamo.client.fixtures.AbstractDynamodbFixture;
import oap.storage.dynamo.client.fixtures.TestContainerDynamodbFixture;
import oap.storage.dynamo.client.modifiers.TableSchemaModifier;
import oap.storage.dynamo.client.modifiers.UpdateTableRequestModifier;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Maps;
import oap.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;

public class DynamodbIndexingTest extends Fixtures {

    private final String keyName = "longId";
    private final AbstractDynamodbFixture fixture = new TestContainerDynamodbFixture();

    public DynamodbIndexingTest() {
        fixture( fixture );
    }

    @BeforeMethod
    public void beforeMethod() {
        System.setProperty( "TMP_PATH", TestDirectoryFixture.testDirectory().toAbsolutePath().toString().replace( '\\', '/' ) );
    }

    @Test
    public void testQueryByScanWithIndex() throws Exception {
        String tableName = "testScanWithIndex";
        String indexName = "index_for_test_bin";
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( tableName );
        client.createTableIfNotExist( tableName, keyName );

        Key key1 = new Key( tableName, keyName, "id1" );
        Key key2 = new Key( tableName, keyName, "id2" );
        Key key3 = new Key( tableName, keyName, "id3" );
        Key key4 = new Key( tableName, keyName, "id4" );
        Key key5 = new Key( tableName, keyName, "id5" );

        assertThat( client.update( key1, "test_bin", 2 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key2, "test_bin", 4 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key3, "test_bin", -1 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key4, "test_bin", -1 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key5, "test_bin", 3 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key3, "aaa", 1 ).getSuccessValue() ).isNotNull();
        assertThat( client.update( key4, "aaa", 2 ).getSuccessValue() ).isNotNull();

        client.createIndex(
            new KeyForSchema( tableName, keyName ),
            indexName,
            "test_bin",
            DynamodbDatatype.NUMBER,
            null,
            null );

        var resultFuture = defineScanTableStream( client, tableName, indexName, "-1" );
        Map<String, Map<String, AttributeValue>> records = resultFuture.collect( Collectors.toMap( k -> k.get( "longId" ).s(), v -> v ) );
        assertThat( records.size() ).isEqualTo( 2 );
        assertThat( records.get( "id3" ).get( "test_bin" ).n() ).isEqualTo( "-1" );
        assertThat( records.get( "id4" ).get( "test_bin" ).n() ).isEqualTo( "-1" );
        assertThat( records.get( "id3" ).get( "aaa" ).n() ).isEqualTo( "1" );
        assertThat( records.get( "id4" ).get( "aaa" ).n() ).isEqualTo( "2" );
    }

    @Data
    static class IndexedRecord {
        private String longId;
        private Long testBin;
        private Long aaa;
    }

    private TableSchema<IndexedRecord> createSchemaForRecord( TableSchemaModifier<IndexedRecord> modifier ) {
        StaticTableSchema.Builder<IndexedRecord> builder = StaticTableSchema.builder( IndexedRecord.class )
            .newItemSupplier( IndexedRecord::new )
            .addAttribute( String.class, a -> a.name( "longId" )
                .getter( IndexedRecord::getLongId )
                .setter( IndexedRecord::setLongId )
                .tags( primaryPartitionKey() ) );
        if( modifier != null ) {
            modifier.accept( builder );
        }
        return builder.build();
    }

    @Test
    public void testIndexedScan() throws Exception {
        String tableName = "testScanWithIndexPerformance";
        String indexColumnName = "test_bin";
        String indexName = "index_for_test_bin";
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( tableName );
        client.createTableIfNotExist( tableName, keyName );

        client.createIndex(
            new KeyForSchema( tableName, keyName ),
            indexName,
            indexColumnName,
            DynamodbDatatype.NUMBER,
            m -> m
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName( indexColumnName )
                        .keyType( KeyType.HASH )
                        .build()
                )
                .projection( Projection.builder().projectionType( ProjectionType.ALL ).build() ),
            m -> m
                .attributeDefinitions(
                    AttributeDefinition.builder()
                        .attributeName( keyName )
                        .attributeType( DynamodbDatatype.STRING.getScalarAttributeType() )
                        .build(),
                    AttributeDefinition.builder()
                        .attributeName( indexColumnName )
                        .attributeType( DynamodbDatatype.NUMBER.getScalarAttributeType() )
                        .build(),
                    AttributeDefinition.builder()
                        .attributeName( "aaa" )
                        .attributeType( DynamodbDatatype.NUMBER.getScalarAttributeType() )
                        .build()
                ) );
        insert100Rows( client, tableName, keyName );

        DynamoDbTable<IndexedRecord> table = client.getEnhancedClient().table( tableName,
            createSchemaForRecord( m -> m //add index attribute
                .addAttribute( Long.class, a -> a.name( indexColumnName )
                    .getter( IndexedRecord::getTestBin )
                    .setter( IndexedRecord::setTestBin )
                    .tags( secondaryPartitionKey( indexName ) ) )
                //this is necessary to get attribute value
                .addAttribute( Long.class, a -> a.name( "aaa" )
                    .getter( IndexedRecord::getAaa )
                    .setter( IndexedRecord::setAaa ) ) )
        );

        List<IndexedRecord> records = scanTableUsingIndex( table, indexName, 3L );
        assertThat( records.size() ).isEqualTo( 20 );
        //make sure all 3 attributes are fetched if projection = ALL
        assertThat( records.get( 0 ).aaa ).isNotNull();
        assertThat( records.get( 0 ).longId ).isNotNull();
        assertThat( records.get( 0 ).testBin ).isNotNull();

        client.deleteIndex( tableName, indexName );
    }

    @Test
    public void testIndexedScanWith2Indexes() throws Exception {
        String tableName = "testScanWithIndices";
        String index1ColumnName = "test_bin";
        String index2ColumnName = "aaa";
        String index1Name = "index_for_test_bin";
        String index2Name = "index_for_aaa";
        var client = fixture.getDynamodbClient();

        client.start();
        client.waitConnectionEstablished();
        client.deleteTableIfExists( tableName );
        client.createTableIfNotExist( tableName, keyName );

        //index for testBin+aaa
        client.createIndex(
            new KeyForSchema( tableName, keyName ),
            index1Name,
            index1ColumnName,
            DynamodbDatatype.NUMBER,
            m -> m
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName( index1ColumnName )
                        .keyType( KeyType.HASH )
                        .build(),
                    KeySchemaElement.builder()
                        .attributeName( index2ColumnName )
                        .keyType( KeyType.RANGE )
                        .build()
                )
                .projection( Projection.builder().projectionType( ProjectionType.KEYS_ONLY ).build() ),
            defineAttributes( keyName, DynamodbDatatype.STRING,
                index1ColumnName, DynamodbDatatype.NUMBER,
                index2ColumnName, DynamodbDatatype.NUMBER ) );
        //index for aaa+testBin
        client.createIndex(
            new KeyForSchema( tableName, keyName ),
            index2Name,
            index2ColumnName,
            DynamodbDatatype.NUMBER,
            m -> m
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName( index2ColumnName )
                        .keyType( KeyType.HASH )
                        .build(),
                    KeySchemaElement.builder()
                        .attributeName( index1ColumnName )
                        .keyType( KeyType.RANGE )
                        .build()
                )
                .projection( Projection.builder().projectionType( ProjectionType.KEYS_ONLY ).build() ),
            defineAttributes( keyName, DynamodbDatatype.STRING,
                index1ColumnName, DynamodbDatatype.NUMBER,
                index2ColumnName, DynamodbDatatype.NUMBER ) );
        insert100Rows( client, tableName, keyName );

        DynamoDbTable<IndexedRecord> table1 = client.getEnhancedClient().table( tableName,
            createSchemaForRecord( m -> m //add index attribute along with key
                .addAttribute( Long.class, a -> a.name( index1ColumnName )
                    .getter( IndexedRecord::getTestBin )
                    .setter( IndexedRecord::setTestBin )
                    .tags( secondaryPartitionKey( index1Name ) ) ) )
        );
        DynamoDbTable<IndexedRecord> table2 = client.getEnhancedClient().table( tableName,
            createSchemaForRecord( m -> m //add index attribute along with key
                .addAttribute( Long.class, a -> a.name( index2ColumnName )
                    .getter( IndexedRecord::getAaa )
                    .setter( IndexedRecord::setAaa )
                    .tags( secondaryPartitionKey( index2Name ) ) ) )
        );

        List<IndexedRecord> records1 = scanTableUsingIndex( table1, index1Name, 1L );
        assertThat( records1.size() ).isEqualTo( 20 );
        List<IndexedRecord> records2 = scanTableUsingIndex( table2, index2Name, 18L );
        assertThat( records2.size() ).isEqualTo( 1 );

        client.deleteIndex( tableName, index1Name );
        client.deleteIndex( tableName, index2Name );
    }

    @NotNull
    private UpdateTableRequestModifier defineAttributes( String keyName, DynamodbDatatype string, String index1ColumnName, DynamodbDatatype string1, String index2ColumnName, DynamodbDatatype number ) {
        return m -> m
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName( keyName )
                    .attributeType( string.getScalarAttributeType() )
                    .build(),
                AttributeDefinition.builder()
                    .attributeName( index1ColumnName )
                    .attributeType( number.getScalarAttributeType() )
                    .build(),
                AttributeDefinition.builder()
                    .attributeName( index2ColumnName )
                    .attributeType( string1.getScalarAttributeType() )
                    .build()
            );
    }

    private List<IndexedRecord> scanTableUsingIndex( DynamoDbTable<IndexedRecord> dynamoDbTable, String indexName, Long valueToSearch ) {
        var resultFuture = ( Stream<Page<IndexedRecord>> ) dynamoDbTable
            .index( indexName )
            .query( QueryEnhancedRequest.builder()
                .queryConditional( QueryConditional.keyEqualTo( k -> k.partitionValue( valueToSearch ) ) )
                .build() )
            .stream();
        List<IndexedRecord> records = new ArrayList<>();
        resultFuture.forEach( p -> records.addAll( p.items() ) );
        return records;
    }

    private void insert100Rows( DynamodbClient client, String tableName, String columnName ) {
        for( int i = 0; i < 100; i++ ) {
            Key key = new Key( tableName, columnName, "id" + i );
            client.update( key, "test_bin", i % 5 );
            client.update( key, "aaa", i );
        }
    }

    private Stream<Map<String, AttributeValue>> defineScanTableStream( DynamodbClient client, String tableName, String indexName, String valueToSelect ) {
        return client.getRecordsByScan( tableName,
            r -> {
                if( indexName != null ) r.indexName( indexName );
                r.filterExpression( "test_bin = :var1Val" )
                    .expressionAttributeValues(
                        Maps.of( new Pair<>( ":var1Val", AttributeValue.fromN( valueToSelect ) ) )
                    );
            }
        );
    }

}
