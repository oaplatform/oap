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

package oap.storage.dynamo.client.convertors;

import oap.storage.dynamo.client.serializers.JsonSerializationHelper;
import oap.testng.Fixtures;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Sets;
import org.testng.annotations.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.BINARY;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.LIST;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.MAP;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.NULL;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_BINARIES;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_NUMBERS;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_STRINGS;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.createAttributeValue;
import static org.assertj.core.api.Assertions.assertThat;

public class DynamodbDatatypeTest extends Fixtures {

    @Test
    public void testConvertFromBooleanTrue() {
        AttributeValue expected = EnhancedAttributeValue.fromBoolean( true ).toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.BOOLEAN, true ) );
        assertEquals( 1, DynamodbDatatype.BOOLEAN.size( true ) );
    }

    @Test
    public void testConvertFromBooleanFalse() {
        AttributeValue expected = EnhancedAttributeValue.fromBoolean( false ).toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.BOOLEAN, false ) );
        assertEquals( 1, DynamodbDatatype.BOOLEAN.size( false ) );
    }

    @Test
    public void testConvertFromAttributeValueToBoolean() {
        AttributeValue expected = EnhancedAttributeValue.fromBoolean( true ).toAttributeValue();

        assertEquals( true, DynamodbDatatype.fromAttributeValue( DynamodbDatatype.BOOLEAN, expected, null ) );
    }

    @Test
    public void testConvertFromEmptyString() {
        AttributeValue expected = EnhancedAttributeValue.fromString( "" ).toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.STRING, "" ) );
        assertEquals( 0, DynamodbDatatype.STRING.size( "" ) );
    }

    @Test
    public void testConvertFromAttributeValueToEmptyString() {
        AttributeValue expected = EnhancedAttributeValue.fromString( "" ).toAttributeValue();

        assertEquals( "", DynamodbDatatype.fromAttributeValue( DynamodbDatatype.STRING, expected, null ) );
    }

    @Test
    public void testConvertFromString() {
        AttributeValue expected = EnhancedAttributeValue.fromString( "astra" ).toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.STRING, "astra" ) );
        assertEquals( 5, DynamodbDatatype.STRING.size( "astra" ) );
    }

    @Test
    public void testConvertFromAttributeValueToString() {
        AttributeValue expected = EnhancedAttributeValue.fromString( "astra" ).toAttributeValue();

        assertEquals( "astra", DynamodbDatatype.fromAttributeValue( DynamodbDatatype.STRING, expected, null ) );
    }

    @Test
    public void testConvertFromNumber() {
        AttributeValue expected = EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.NUMBER, 3.1415 ) );
        assertEquals( 6, DynamodbDatatype.NUMBER.size( 3.1415 ) );
    }

    @Test
    public void testConvertFromAttributeValueToNumber() {
        AttributeValue expected = EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue();

        assertEquals( Double.valueOf( "3.1415" ), DynamodbDatatype.fromAttributeValue( DynamodbDatatype.NUMBER, expected, null ) );
    }

    @Test
    public void testConvertFromBinary() {
        byte[] bytes = JsonSerializationHelper.base64DecodeString( "dGhpcyB0ZXh0IGlzIGJhc2U2NC1lbmNvZGVk" );
        AttributeValue expected = EnhancedAttributeValue.fromBytes( SdkBytes.fromByteArray( bytes ) ).toAttributeValue();

        assertEquals( expected, createAttributeValue( BINARY, bytes ) );
        assertEquals( bytes.length, BINARY.size( bytes ) );
    }

    @Test
    public void testConvertFromAttributeValueToBinary() {
        byte[] bytes = JsonSerializationHelper.base64DecodeString( "dGhpcyB0ZXh0IGlzIGJhc2U2NC1lbmNvZGVk" );
        AttributeValue expected = EnhancedAttributeValue.fromBytes( SdkBytes.fromByteArray( bytes ) ).toAttributeValue();

        assertThat( DynamodbDatatype.fromAttributeValue( BINARY, expected, null ) ).isEqualTo( bytes );
    }

    @Test
    public void testConvertFromNull() {
        AttributeValue expected = EnhancedAttributeValue.nullValue().toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.NULL, null ) );
        assertEquals( 0, DynamodbDatatype.NULL.size( null ) );
    }

    @Test
    public void testConvertFromAttributeValueToNull() {
        AttributeValue expected = EnhancedAttributeValue.nullValue().toAttributeValue();

        assertThat( DynamodbDatatype.fromAttributeValue( NULL, expected, null ) ).isEqualTo( true );
    }

    @Test
    public void testConvertFromAttributeValueToNotNull() {
        AttributeValue expected = EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue();

        assertThat( DynamodbDatatype.fromAttributeValue( NULL, expected, null ) ).isEqualTo( false );
    }

    @Test
    public void testConvertFromSetOfBinaries() {
        byte[] bytes1 = JsonSerializationHelper.base64DecodeString( "abracadabra" );
        byte[] bytes2 = JsonSerializationHelper.base64DecodeString( "chupacabra" );
        AttributeValue expected = EnhancedAttributeValue.fromSetOfBytes( SdkBytes.fromByteArray( bytes1 ), SdkBytes.fromByteArray( bytes2 ) ).toAttributeValue();

        assertEquals( expected, createAttributeValue( SET_OF_BINARIES, Lists.of( bytes1, bytes2 ) ) );
        assertEquals( bytes1.length + bytes2.length, SET_OF_BINARIES.size( Lists.of( bytes1, bytes2 ) ) );
    }

    @Test
    public void testConvertFromAttributeValueToSetOfBinaries() {
        byte[] bytes1 = JsonSerializationHelper.base64DecodeString( "abracadabra" );
        byte[] bytes2 = JsonSerializationHelper.base64DecodeString( "chupacabra" );
        AttributeValue expected = EnhancedAttributeValue.fromSetOfBytes( SdkBytes.fromByteArray( bytes1 ), SdkBytes.fromByteArray( bytes2 ) ).toAttributeValue();

        List<byte[]> actual = ( List<byte[]> ) DynamodbDatatype.fromAttributeValue( SET_OF_BINARIES, expected, null );

        assertThat( actual.size() ).isEqualTo( 2 );
        assertThat( actual.toArray()[0] ).isEqualTo( bytes1 );
        assertThat( actual.toArray()[1] ).isEqualTo( bytes2 );
    }

    @Test
    public void testConvertFromSetOfStrings() {
        AttributeValue expected = EnhancedAttributeValue.fromSetOfStrings( "1", "2", "3" ).toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.SET_OF_STRINGS, Sets.of( "1", "2", "3" ) ) );
        assertEquals( 3, DynamodbDatatype.SET_OF_STRINGS.size( Sets.of( "1", "2", "3" ) ) );
    }

    @Test
    public void testConvertFromAttributeValueToSetOfStrings() {
        AttributeValue expected = EnhancedAttributeValue.fromSetOfStrings( "1", "2", "3" ).toAttributeValue();

        List<String> actual = ( List<String> ) DynamodbDatatype.fromAttributeValue( SET_OF_STRINGS, expected, null );

        assertThat( actual ).isEqualTo( Lists.of( "1", "2", "3" ) );
    }

    @Test
    public void testConvertFromSetOfNumbers() {
        AttributeValue expected = EnhancedAttributeValue.fromSetOfNumbers( "3.1415", "2.7182" ).toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.SET_OF_NUMBERS, Sets.of( 3.1415, 2.7182 ) ) );
        assertEquals( 12, DynamodbDatatype.SET_OF_STRINGS.size( Sets.of( 3.1415, 2.7182 ) ) );
    }

    @Test
    public void testConvertFromAttributeValueToSetOfNumbers() {
        AttributeValue expected = EnhancedAttributeValue.fromSetOfNumbers( "3.1415", "2.7182" ).toAttributeValue();

        List<Number> actual = ( List<Number> ) DynamodbDatatype.fromAttributeValue( SET_OF_NUMBERS, expected, null );

        assertThat( actual ).isEqualTo( Lists.of( 3.1415, 2.7182 ) );
    }

    @Test
    public void testConvertFromList() {
        List<AttributeValue> list = Lists.of(
                EnhancedAttributeValue.fromString( "1" ).toAttributeValue(),
                EnhancedAttributeValue.fromBoolean( true ).toAttributeValue(),
                EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue() );
        AttributeValue expected = EnhancedAttributeValue.fromListOfAttributeValues( list ).toAttributeValue();

        assertEquals( expected, createAttributeValue( DynamodbDatatype.LIST, Lists.of( "1", true, 3.1415 ) ) );
        assertEquals( 11, DynamodbDatatype.LIST.size( Sets.of( "1", true, 3.1415 ) ) );
    }

    @Test
    public void testConvertFromAttributeValueToList() {
        List<AttributeValue> list = Lists.of(
                EnhancedAttributeValue.fromString( "1" ).toAttributeValue(),
                EnhancedAttributeValue.fromBoolean( true ).toAttributeValue(),
                EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue() );
        AttributeValue expected = EnhancedAttributeValue.fromListOfAttributeValues( list ).toAttributeValue();

        List<Object> actual = ( List<Object> ) DynamodbDatatype.fromAttributeValue( LIST, expected, null );

        assertEquals( Lists.of( "1", true, 3.1415 ), actual );
    }

    @Test
    public void testConvertFromMapOfStrings() {
        Map<String, AttributeValue> map = Maps.of(
                new Pair<>( "One",  EnhancedAttributeValue.fromString( "1" ).toAttributeValue() ),
                new Pair<>( "Two", EnhancedAttributeValue.fromBoolean( true ).toAttributeValue() ),
                new Pair<>( "Three", EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue() ) );
        AttributeValue expected = EnhancedAttributeValue.fromMap( map ).toAttributeValue();
        Map<String, Object> mapToConvert =  Maps.of(
                new Pair<>( "One", "1" ),
                new Pair<>( "Two", true ),
                new Pair<>( "Three", 3.1415 ) );

        assertEquals( expected, createAttributeValue( MAP, mapToConvert ) );
        assertEquals( 31, MAP.size( mapToConvert ) );
    }

    @Test
    public void testConvertFromMapOfLongs() {
        Map<String, AttributeValue> map = Maps.of(
                new Pair<>( "1",  EnhancedAttributeValue.fromString( "1" ).toAttributeValue() ),
                new Pair<>( "2", EnhancedAttributeValue.fromBoolean( true ).toAttributeValue() ),
                new Pair<>( "3", EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue() ) );
        AttributeValue expected = EnhancedAttributeValue.fromMap( map ).toAttributeValue();
        Map<Long, Object> mapToConvert =  Maps.of(
                new Pair<>( 1L, "1" ),
                new Pair<>( 2L, true ),
                new Pair<>( 3L, 3.1415 ) );

        assertEquals( expected, createAttributeValue( MAP, mapToConvert, v -> ( ( Long ) v ).toString() ) );
        assertEquals( 23, MAP.size( mapToConvert ) );
    }

    @Test
    public void testConvertFromAttributeValueToMapOfStrings() {
        Map<String, AttributeValue> map = Maps.of(
                new Pair<>( "One",  EnhancedAttributeValue.fromString( "1" ).toAttributeValue() ),
                new Pair<>( "Two", EnhancedAttributeValue.fromBoolean( true ).toAttributeValue() ),
                new Pair<>( "Three", EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue() ) );
        AttributeValue expected = EnhancedAttributeValue.fromMap( map ).toAttributeValue();
        Map<String, Object> mapToConvert =  Maps.of(
                new Pair<>( "One", "1" ),
                new Pair<>( "Two", true ),
                new Pair<>( "Three", 3.1415 ) );

        Map<String, Object> actual = ( Map<String, Object> ) DynamodbDatatype.fromAttributeValue( MAP, expected, null );

        assertEquals( mapToConvert, actual );
    }

    @Test
    public void testConvertFromAttributeValueToMapOfLongs() {
        Map<String, AttributeValue> map = Maps.of(
                new Pair<>( "1",  EnhancedAttributeValue.fromString( "1" ).toAttributeValue() ),
                new Pair<>( "2", EnhancedAttributeValue.fromBoolean( true ).toAttributeValue() ),
                new Pair<>( "3", EnhancedAttributeValue.fromNumber( "3.1415" ).toAttributeValue() ) );
        AttributeValue expected = EnhancedAttributeValue.fromMap( map ).toAttributeValue();
        Map<Long, Object> mapToConvert =  Maps.of(
                new Pair<>( 1L, "1" ),
                new Pair<>( 2L, true ),
                new Pair<>( 3L, 3.1415 ) );

        Map<Long, Object> actual = ( Map<Long, Object> ) DynamodbDatatype.fromAttributeValue( MAP, expected, v -> Long.parseLong( ( String ) v ) );

        assertEquals( mapToConvert, actual );
    }

}
