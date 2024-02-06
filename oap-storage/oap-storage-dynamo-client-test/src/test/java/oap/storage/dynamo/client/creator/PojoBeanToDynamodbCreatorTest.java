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

package oap.storage.dynamo.client.creator;

import com.google.common.collect.MapDifference;
import oap.storage.dynamo.client.creator.samples.TestClassWithSetOfIntegers;
import oap.storage.dynamo.client.creator.samples.Autonomious;
import oap.storage.dynamo.client.creator.samples.AutonomiousDynamo;
import oap.storage.dynamo.client.creator.samples.PrimitivesHolder;
import oap.storage.dynamo.client.creator.samples.TestClassWithListOfDoubles;
import oap.storage.dynamo.client.creator.samples.TestClassWithListOfFloats;
import oap.storage.dynamo.client.creator.samples.TestClassWithListOfIntegers;
import oap.storage.dynamo.client.creator.samples.TestClassWithListOfLongs;
import oap.storage.dynamo.client.creator.samples.TestClassWithSetOfDoubles;
import oap.storage.dynamo.client.creator.samples.TestClassWithSetOfFloats;
import oap.storage.dynamo.client.creator.samples.TestClassWithSetOfLongs;
import oap.storage.dynamo.client.creator.samples.UsingIgnoreFieldAnno;
import oap.testng.Fixtures;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Sets;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.BINARY;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.BOOLEAN;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.MAP;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.NULL;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.NUMBER;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_BINARIES;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_NUMBERS;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_STRINGS;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.STRING;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.createAttributeValue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class PojoBeanToDynamodbCreatorTest extends Fixtures {

    @Test
    public void testPrimitiveBoolean() throws ReflectiveOperationException {
        PrimitivesHolder auto = new PrimitivesHolder();
        auto.setBooleanVar( true );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<>().createFromBean( auto );

        assertEquals( createAttributeValue( BOOLEAN, true ), actual.get( "booleanVar" ) );
    }

    @Test
    public void testPrimitiveInt() throws ReflectiveOperationException {
        PrimitivesHolder auto = new PrimitivesHolder();
        auto.setIntVar( 123 );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<>().createFromBean( auto );

        assertEquals( createAttributeValue( NUMBER, 123 ), actual.get( "intVar" ) );
    }

    @Test
    public void testPrimitiveLong() throws ReflectiveOperationException {
        PrimitivesHolder auto = new PrimitivesHolder();
        auto.setLongVar( 123456789L );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<>().createFromBean( auto );

        assertEquals( createAttributeValue( NUMBER, 123456789L ), actual.get( "longVar" ) );
    }

    @Test
    public void testPrimitiveFloat() throws ReflectiveOperationException {
        PrimitivesHolder auto = new PrimitivesHolder();
        auto.setFloatVar( ( float ) 1.123456789 );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<>().createFromBean( auto );

        assertEquals( createAttributeValue( NUMBER, 1.1234568 ), actual.get( "floatVar" ) );
    }

    @Test
    public void testPrimitiveDouble() throws ReflectiveOperationException {
        PrimitivesHolder auto = new PrimitivesHolder();
        auto.setDoubleVar( 1.123456789 );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<PrimitivesHolder>().createFromBean( auto );

        assertEquals( createAttributeValue( NUMBER, 1.123456789 ), actual.get( "doubleVar" ) );
    }

    @Test
    public void testListOfIntegers() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1, 2, 3 ) ) );
        TestClassWithListOfIntegers auto = new TestClassWithListOfIntegers();
        auto.setField( Lists.of( 1, 2, 3 ) );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<TestClassWithListOfIntegers>().createFromBean( auto );

        assertEquals( arguments, actual );
    }

    @Test
    public void testSetOfIntegers() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1, 2 ) ) );
        TestClassWithSetOfIntegers auto = new TestClassWithSetOfIntegers();
        auto.setField( Sets.of( 1, 2, 1 ) );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<TestClassWithSetOfIntegers>().createFromBean( auto );

        assertEquals( arguments, actual );
    }

    @Test
    public void testListOfDoubles() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1123456789, 2.2123456789, 3.3123456789 ) ) );
        TestClassWithListOfDoubles auto = new TestClassWithListOfDoubles();
        auto.setField( Lists.of( 1.1123456789, 2.2123456789, 3.3123456789 ) );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<TestClassWithListOfDoubles>().createFromBean( auto );

        assertEquals( arguments, actual );
    }

    @Test
    public void testSetOfDoubles() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Sets.of( 1.1123456789, 2.2123456789, 1.1123456789 ) ) );
        TestClassWithSetOfDoubles auto = new TestClassWithSetOfDoubles();
        auto.setField( Sets.of( 1.1123456789, 2.2123456789, 1.1123456789 ) );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<TestClassWithSetOfDoubles>().createFromBean( auto );

        assertEquals( arguments, actual );
    }

    @Test
    public void testListOfLongs() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1L, 2L, 3L ) ) );
        TestClassWithListOfLongs auto = new TestClassWithListOfLongs();
        auto.setField( Lists.of( 1L, 2L, 3L ) );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<TestClassWithListOfLongs>().createFromBean( auto );

        assertEquals( arguments, actual );
    }

    @Test
    public void testSetOfLongs() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Sets.of( 1L, 2L, 3L ) ) );
        TestClassWithSetOfLongs auto = new TestClassWithSetOfLongs();
        auto.setField( Sets.of( 1L, 2L, 3L ) );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<TestClassWithSetOfLongs>().createFromBean( auto );

        assertEquals( arguments, actual );
    }

    @Test
    public void testListOfFloats() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( ( float ) 1.1123456789, ( float ) 2.2123456789, ( float ) 3.3123456789 ) ) );
        TestClassWithListOfFloats auto = new TestClassWithListOfFloats();
        auto.setField( Lists.of( ( float ) 1.1123456789, ( float ) 2.2123456789, ( float ) 3.3123456789 ) );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<TestClassWithListOfFloats>().createFromBean( auto );

        assertEquals( arguments, actual );
    }

    @Test
    public void testSetOfFloats() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Sets.of( ( float ) 1.1123456789, ( float ) 2.2123456789, ( float ) 3.3123456789 ) ) );
        TestClassWithSetOfFloats auto = new TestClassWithSetOfFloats();
        auto.setField( Sets.of( ( float ) 1.1123456789, ( float ) 2.2123456789, ( float ) 3.3123456789 ) );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<TestClassWithSetOfFloats>().createFromBean( auto );

        assertEquals( arguments, actual );
    }

    @Test
    public void testIgnoreField() throws ReflectiveOperationException {
        UsingIgnoreFieldAnno auto = new UsingIgnoreFieldAnno();
        auto.setField( "usual field" );
        auto.setIgnoreField( "to be ignored" );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<UsingIgnoreFieldAnno>().createFromBean( auto );

        assertNotNull( actual.get( "field" ) );
        assertThat( actual.get( "ignoreField" ).nul() ).isTrue();
    }

    @Test
    // "135 sec for 100 k iterations"
    public void fullTestViaReflection() throws ReflectiveOperationException {
        Map<String, AttributeValue> arguments = createArguments();
        Autonomious auto = new PojoBeanFromDynamoCreator<Autonomious>().createBean( Autonomious.class, arguments );
        auto.setNumberVar( 3.1415926535 );

        Map<String, AttributeValue> actual = new PojoBeanToDynamoCreator<Autonomious>().createFromBean( auto );

        MapDifference<String, AttributeValue> diff = com.google.common.collect.Maps.difference( arguments, actual );
        assertTrue( diff.areEqual() );
    }

    @Test( enabled = false )
    // "1050 sec for 100 k iterations"
    public void fullTestViaAmazonSDK() {
        Map<String, AttributeValue> arguments = createArguments();
        AutonomiousDynamo auto = new PojoBeanFromDynamoCreator<AutonomiousDynamo>().createBean( AutonomiousDynamo.class, arguments );
        arguments.remove( "publicNumberVar" );
        arguments.remove( "staticVar" );
        arguments.remove( "finalVar" );
        auto.setNumberVar( 3.1415926535 );
        Map<String, List<String>> mapOfStrings = Maps.of(
                new Pair<>( "One", Collections.singletonList( "1" ) ),
                new Pair<>( "Two", Collections.singletonList( "2" ) )
        );
        Map<String, AttributeValue> mapOfAttributeValues = Maps.of(
                new Pair<>( "One", AttributeValue.fromL( Collections.singletonList( AttributeValue.fromS( "1" ) ) ) ),
                new Pair<>( "Two", AttributeValue.fromL( Collections.singletonList( AttributeValue.fromS( "2" ) ) ) )
        );
        auto.setMapOfObjects( mapOfStrings );
        arguments.put( "mapOfObjects", AttributeValue.builder().m( mapOfAttributeValues ).build() );

        Map<String, AttributeValue> actual = new HashMap<>( new PojoBeanToDynamoCreator<AutonomiousDynamo>().fromDynamo( auto ) );
        actual.remove( "superVar" );

        MapDifference<String, AttributeValue> diff = com.google.common.collect.Maps.difference( arguments, actual );
        assertTrue( diff.areEqual() );
    }

    @Test
    public void fullTestViaAmazonSDKNonGenericMap() {
        Map<String, AttributeValue> arguments = createArguments();
        arguments.put( "mapOfObjects", createAttributeValue( MAP, Maps.of( new Pair<>( "One", 1.0 ), new Pair<>( "Two", false ) ) ) );
        // replaced with Map<String, Object> as DynamoDB sdk needs a properly declared class, not an Object as values
        //so in this case it throws an exception.
        assertThatThrownBy( () -> new PojoBeanFromDynamoCreator<AutonomiousDynamo>().fromDynamo( AutonomiousDynamo.class, arguments ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter "
                        + "cannot convert an attribute of type N into the requested type interface java.util.List" );
    }

    @NotNull
    private Map<String, AttributeValue> createArguments() {
        Map<String, AttributeValue> arguments = new LinkedHashMap<>();
        arguments.put( "bytesVar", createAttributeValue( BINARY, Base64.getDecoder().decode( "QUJDRA==" ) ) );
        arguments.put( "stringVar", createAttributeValue( STRING, "string value for 'stringVar'" ) );
        arguments.put( "listOfStrings", createAttributeValue( SET_OF_STRINGS, Lists.of( "string 1", "string 2", "string 3" ) ) );
        arguments.put( "publicDoubleVar", createAttributeValue( NUMBER, 1.23456789012345 ) );
        arguments.put( "longVar", createAttributeValue( NUMBER, 1234567890 ) );
        arguments.put( "floatVar", createAttributeValue( NUMBER, 1.21 ) );
        arguments.put( "doubleVar", createAttributeValue( NUMBER, 1.23456789012345 ) );

        arguments.put( "publicNumberVar", createAttributeValue( NUMBER, 3.1415926535 ) );

        arguments.put( "intVar", createAttributeValue( NUMBER, 123 ) );
        arguments.put( "numberVar", createAttributeValue( NUMBER, 3.1415926535 ) );
        arguments.put( "listOfIntegers", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1, 2, 3, 4 ) ) );
        arguments.put( "mapOfObjects",
                createAttributeValue( MAP,
                        Maps.of(
                                new Pair<>( "One", Collections.singletonList( 1.0 ) ),
                                new Pair<>( "Two", Collections.singletonList(  false ) ) )
                )
        );
        arguments.put( "booleanVar", createAttributeValue( BOOLEAN, true ) );
        arguments.put( "publicLongVar", createAttributeValue( NUMBER, 1234567890 ) );
        arguments.put( "staticVar", createAttributeValue( NULL, null ) );
        arguments.put( "publicFloatVar", createAttributeValue( NUMBER, 1.21 ) );
        arguments.put( "finalVar", createAttributeValue( STRING, "string value for 'finalVar'" ) );
        arguments.put( "publicIntVar", createAttributeValue( NUMBER, 123 ) );
        arguments.put( "publicBytesVar", createAttributeValue( BINARY, Base64.getDecoder().decode( "QUJDRA==" ) ) );
        arguments.put( "id", createAttributeValue( STRING, "id attribute" ) );
        arguments.put( "listOfBinaries", createAttributeValue( SET_OF_BINARIES, Lists.of( new byte[] { 0x01 }, new byte[] { 0x02 } ) ) );
        arguments.put( "publicStringVar", createAttributeValue( STRING, "string value for 'public_finalVar'" ) );
        arguments.put( "publicBooleanVar", createAttributeValue( BOOLEAN, true ) );

        return arguments;
    }
}
