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

import oap.storage.dynamo.client.creator.samples.TestClassWithSetOfIntegers;
import oap.storage.dynamo.client.creator.samples.Autonomious;
import oap.storage.dynamo.client.creator.samples.AutonomiousDynamo;
import oap.storage.dynamo.client.creator.samples.BeanWithRestrictedField;
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
import oap.util.HashMaps;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.BINARY;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.BOOLEAN;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.MAP;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.NUMBER;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_BINARIES;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_NUMBERS;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.SET_OF_STRINGS;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.STRING;
import static oap.storage.dynamo.client.convertors.DynamodbDatatype.createAttributeValue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PojoBeanFromDynamoCreatorTest extends Fixtures {

    @Test
    public void testPrimitiveBoolean() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "booleanVar", createAttributeValue( BOOLEAN, true ) );

        PrimitivesHolder auto = new PojoBeanFromDynamoCreator<PrimitivesHolder>().createBean( PrimitivesHolder.class, arguments );

        assertTrue( auto.isBooleanVar() );
    }

    @Test
    public void testPrimitiveInteger() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "intVar", createAttributeValue( NUMBER, 567 ) );

        PrimitivesHolder auto = new PojoBeanFromDynamoCreator<PrimitivesHolder>().createBean( PrimitivesHolder.class, arguments );

        assertEquals( 567, auto.getIntVar() );
    }

    @Test
    public void testPrimitiveLong() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "longVar", createAttributeValue( NUMBER, 123456789L ) );

        PrimitivesHolder auto = new PojoBeanFromDynamoCreator<PrimitivesHolder>().createBean( PrimitivesHolder.class, arguments );

        assertEquals( 123456789L, auto.getLongVar() );
    }

    @Test
    public void testPrimitiveFloat() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "floatVar", createAttributeValue( NUMBER, 1.123456789 ) );

        PrimitivesHolder auto = new PojoBeanFromDynamoCreator<PrimitivesHolder>().createBean( PrimitivesHolder.class, arguments );

        assertEquals( ( float ) 1.1234568, auto.getFloatVar() );
    }

    @Test
    public void testPrimitiveDouble() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "doubleVar", createAttributeValue( NUMBER, 1.123456789 ) );

        PrimitivesHolder auto = new PojoBeanFromDynamoCreator<PrimitivesHolder>().createBean( PrimitivesHolder.class, arguments );

        assertEquals( 1.123456789, auto.getDoubleVar() );
    }

    @Test
    public void testListOfIntegers() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1123456789, 2.2123456789, 3.3123456789 ) ) );

        TestClassWithListOfIntegers auto = new PojoBeanFromDynamoCreator<TestClassWithListOfIntegers>().createBean( TestClassWithListOfIntegers.class, arguments );

        assertEquals( "[1, 2, 3]", auto.getField().toString() );
    }

    @Test
    public void testSetOfIntegers() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1123456789, 2.2123456789, 1.1123456789 ) ) );

        TestClassWithSetOfIntegers auto = new PojoBeanFromDynamoCreator<TestClassWithSetOfIntegers>().createBean( TestClassWithSetOfIntegers.class, arguments );

        assertEquals( "[1, 2]", auto.getField().toString() );
    }

    @Test
    public void testListOfDoubles() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1123456789, 2.2123456789, 3.3123456789 ) ) );

        TestClassWithListOfDoubles auto = new PojoBeanFromDynamoCreator<TestClassWithListOfDoubles>().createBean( TestClassWithListOfDoubles.class, arguments );

        assertEquals( "[1.1123456789, 2.2123456789, 3.3123456789]", auto.getField().toString() );
    }

    @Test
    public void testSetOfDoubles() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1123456789, 2.2123456789, 1.1123456789 ) ) );

        TestClassWithSetOfDoubles auto = new PojoBeanFromDynamoCreator<TestClassWithSetOfDoubles>().createBean( TestClassWithSetOfDoubles.class, arguments );

        assertEquals( "[1.1123456789, 2.2123456789]", auto.getField().toString() );
    }

    @Test
    public void testListOfLongs() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1, 2.2, 3.3 ) ) );

        TestClassWithListOfLongs auto = new PojoBeanFromDynamoCreator<TestClassWithListOfLongs>().createBean( TestClassWithListOfLongs.class, arguments );

        assertEquals( "[1, 2, 3]", auto.getField().toString() );
    }

    @Test
    public void testSetOfLongs() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1, 2.2, 1.1 ) ) );

        TestClassWithSetOfLongs auto = new PojoBeanFromDynamoCreator<TestClassWithSetOfLongs>().createBean( TestClassWithSetOfLongs.class, arguments );

        assertEquals( "[1, 2]", auto.getField().toString() );
    }

    @Test
    public void testListOfFloats() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1123456789, 2.2123456789, 3.3123456789 ) ) );

        TestClassWithListOfFloats auto = new PojoBeanFromDynamoCreator<TestClassWithListOfFloats>().createBean( TestClassWithListOfFloats.class, arguments );

        assertEquals( "[1.1123457, 2.2123456, 3.3123457]", auto.getField().toString() );
    }

    @Test
    public void testSetOfFloats() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1.1123456789, 2.2123456789, 1.1123456789 ) ) );

        TestClassWithSetOfFloats auto = new PojoBeanFromDynamoCreator<TestClassWithSetOfFloats>().createBean( TestClassWithSetOfFloats.class, arguments );

        assertEquals( "[1.1123457, 2.2123456]", auto.getField().toString() );
    }

    @Test
    public void testIgnoreField() {
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "field", createAttributeValue( STRING, "field1" ) );
        arguments.put( "ignoreField", createAttributeValue( STRING, "field2" ) );

        UsingIgnoreFieldAnno auto = new PojoBeanFromDynamoCreator<UsingIgnoreFieldAnno>().createBean( UsingIgnoreFieldAnno.class, arguments );

        assertNotNull( auto.getField() );
        assertNull( auto.getIgnoreField() );
    }

    @Test
    // "10.5 sec for 100 k iterations"
    public void fullTestViaReflection() {
        Map<String, AttributeValue> arguments = createArguments();

        Autonomious auto = new PojoBeanFromDynamoCreator<Autonomious>().createBean( Autonomious.class, arguments );

        assertEquals( "Autonomious(super=Supernatural(superVar=null), id=id attribute, finalVar=string value for 'finalVar', publicLongVar=1234567890, publicIntVar=123, publicFloatVar=1.21, publicDoubleVar=1.23456789012345, publicStringVar=string value for 'public_finalVar', publicBooleanVar=true, publicNumberVar=3.1415926535, publicBytesVar=[65, 66, 67, 68], intVar=123, longVar=1234567890, floatVar=1.21, doubleVar=1.23456789012345, stringVar=string value for 'stringVar', numberVar=3.1415926535, booleanVar=true, bytesVar=[65, 66, 67, 68], listOfStrings=[string 1, string 2, string 3], listOfIntegers=[1, 2, 3, 4], mapOfObjects={One=[1.0], Two=[false]})",
                auto.toString() );
    }

    @Test
    // "Trying to process a bean class with field 'name' which is reserved in DynamoDB"
    public void testWithRestrictedNameOfField() {
        Map<String, AttributeValue> arguments = HashMaps.of(
            "id", AttributeValue.fromS( "One" ),
            "name", AttributeValue.fromS( "Obi One Kenobee" ),
            "c", AttributeValue.fromN( "5" )
        );

        BeanWithRestrictedField auto = new PojoBeanFromDynamoCreator<BeanWithRestrictedField>().createBean( BeanWithRestrictedField.class, arguments );

        assertEquals( "BeanWithRestrictedField(id=One, name=Obi One Kenobee, c=5)",
                auto.toString() );
    }

    @Test
    public void fullTestViaReflectionNonGenericMap() {
        Map<String, AttributeValue> arguments = createArguments();
        arguments.put( "mapOfObjects", createAttributeValue( MAP, Maps.of( new Pair<>( "One", 1.0 ), new Pair<>( "Two", false ) ) ) );

        Autonomious auto = new PojoBeanFromDynamoCreator<Autonomious>().createBean( Autonomious.class, arguments );

        assertEquals( "Autonomious(super=Supernatural(superVar=null), id=id attribute, finalVar=string value for 'finalVar', publicLongVar=1234567890, publicIntVar=123, publicFloatVar=1.21, publicDoubleVar=1.23456789012345, publicStringVar=string value for 'public_finalVar', publicBooleanVar=true, publicNumberVar=3.1415926535, publicBytesVar=[65, 66, 67, 68], intVar=123, longVar=1234567890, floatVar=1.21, doubleVar=1.23456789012345, stringVar=string value for 'stringVar', numberVar=3.1415926535, booleanVar=true, bytesVar=[65, 66, 67, 68], listOfStrings=[string 1, string 2, string 3], listOfIntegers=[1, 2, 3, 4], mapOfObjects={One=1.0, Two=false})",
                auto.toString() );
    }

    @Test
    // "1050 sec for 100 k iterations"
    public void fullTestViaAmazonSDK() {
        Map<String, AttributeValue> arguments = createArguments();

        AutonomiousDynamo auto = new PojoBeanFromDynamoCreator<AutonomiousDynamo>().fromDynamo( AutonomiousDynamo.class, arguments );

        assertEquals( "AutonomiousDynamo(super=Supernatural(superVar=null), id=id attribute, finalVar=field is not set, publicLongVar=1234567890, publicIntVar=123, publicFloatVar=1.21, publicDoubleVar=1.23456789012345, publicStringVar=string value for 'public_finalVar', publicBooleanVar=true, publicBytesVar=[65, 66, 67, 68], intVar=123, longVar=1234567890, floatVar=1.21, doubleVar=1.23456789012345, stringVar=string value for 'stringVar', numberVar=3.1415926535, booleanVar=true, bytesVar=[65, 66, 67, 68], listOfStrings=[string 1, string 2, string 3], listOfIntegers=[1, 2, 3, 4], mapOfObjects={One=[1.0], Two=[false]})",
                auto.toString() );
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
        Map<String, AttributeValue> arguments = new HashMap<>();
        arguments.put( "finalVar", createAttributeValue( STRING, "string value for 'finalVar'" ) );
        arguments.put( "id", createAttributeValue( STRING, "id attribute" ) );
        arguments.put( "stringVar", createAttributeValue( STRING, "string value for 'stringVar'" ) );
        arguments.put( "listOfStrings", createAttributeValue( SET_OF_STRINGS, Lists.of( "string 1", "string 2", "string 3" ) ) );
        arguments.put( "listOfIntegers", createAttributeValue( SET_OF_NUMBERS, Lists.of( 1, 2, 3, 4 ) ) );
        arguments.put( "listOfBinaries", createAttributeValue( SET_OF_BINARIES, Lists.of( new byte[] { 0x01 }, new byte[] { 0x02 } ) ) );
        arguments.put( "mapOfObjects",
                createAttributeValue( MAP,
                        Maps.of(
                                new Pair<>( "One", Collections.singletonList( 1.0 ) ),
                                new Pair<>( "Two", Collections.singletonList(  false ) ) )
                )
        );
        arguments.put( "numberVar", createAttributeValue( NUMBER, 3.1415926535 ) );
        arguments.put( "intVar", createAttributeValue( NUMBER, 123 ) );
        arguments.put( "floatVar", createAttributeValue( NUMBER, 1.21 ) );
        arguments.put( "longVar", createAttributeValue( NUMBER, 1234567890 ) );
        arguments.put( "doubleVar", createAttributeValue( NUMBER, 1.23456789012345 ) );
        arguments.put( "booleanVar", createAttributeValue( BOOLEAN, true ) );
        arguments.put( "bytesVar", createAttributeValue( BINARY, Base64.getDecoder().decode( "QUJDRA==" ) ) );

        arguments.put( "publicStringVar", createAttributeValue( STRING, "string value for 'public_finalVar'" ) );
        arguments.put( "publicNumberVar", createAttributeValue( NUMBER, 3.1415926535 ) );
        arguments.put( "publicIntVar", createAttributeValue( NUMBER, 123 ) );
        arguments.put( "publicFloatVar", createAttributeValue( NUMBER, 1.21 ) );
        arguments.put( "publicLongVar", createAttributeValue( NUMBER, 1234567890 ) );
        arguments.put( "publicDoubleVar", createAttributeValue( NUMBER, 1.23456789012345 ) );
        arguments.put( "publicBooleanVar", createAttributeValue( BOOLEAN, true ) );
        arguments.put( "publicBytesVar", createAttributeValue( BINARY, Base64.getDecoder().decode( "QUJDRA==" ) ) );
        return arguments;
    }
}
