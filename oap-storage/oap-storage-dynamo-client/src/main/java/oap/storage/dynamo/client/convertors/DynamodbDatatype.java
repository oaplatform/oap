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

import oap.storage.dynamo.client.creator.PojoBeanToDynamoCreator;
import oap.util.Pair;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static oap.storage.dynamo.client.serializers.JsonSerializationHelper.CHARACTER_ENCODING;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.Type.NUL;

/**
 * @link https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html
 */
public enum DynamodbDatatype {

    //scalars ScalarAttributeType
    BOOLEAN( "B",
        ( o, value, convertor ) -> o.bool( ( Boolean ) value ),
        ( o, convertor ) -> o == null || o.bool() == null ? null : o.bool(),
        o -> 1
    ),
    STRING( "S",
        ( o, value, convertor ) -> o.s( ( String ) value ),
        ( o, convertor ) -> o == null ? null : o.s(),
        o -> getLength( ( String ) o )
    ),
    NUMBER( "N",
        ( o, value, convertor ) -> o.n( String.valueOf( value ) ),
        ( o, convertor ) -> o == null || o.n() == null ? null : Double.parseDouble( o.n() ),
        o -> o == null ? 0 : getLength( String.valueOf( o ) )
    ),
    BINARY( "BB",
        ( o, value, convertor ) -> o.b( SdkBytes.fromByteArray( ( byte[] ) value ) ),
        ( o, convertor ) -> o == null || o.b() == null ? null : o.b().asByteArray(),
        o -> o == null ? 0 : ( ( byte[] ) o ).length
    ),
    NULL( "nul",
        ( o, value, convertor ) -> o.nul( true ),
        ( o, convertor ) -> o == null || o.type() == NUL || o.nul() != null,
        o -> 0
    ),
    //documents
    LIST( "L",
        ( o, value, convertor ) -> o.l( convertToList( ( List ) value ) ),
        ( o, convertor ) -> o == null || o.l() == null ? null : o.l().stream().map( DynamodbDatatype::createObjectFromAttributeValue ).collect( Collectors.toList() ),
        o -> o == null ? 0 : ( ( LinkedHashSet<Object> ) o ).stream().map( v -> getLength( String.valueOf( v.toString() ) ) ).reduce( 0, Integer::sum )
    ),
    MAP( "M",
        ( o, value, convertor ) -> o.m( convertToMap( ( Map ) value, convertor ) ),
        ( o, convertor ) -> o == null || o.m() == null ? null : o.m().entrySet().stream().collect( Collectors.toMap( k -> convertor == null ? k.getKey() : convertor.convert( k.getKey() ), v -> createObjectFromAttributeValue( v.getValue() ) ) ),
        o -> o == null ? 0 : getLength( o.toString() )
    ),
        //set structures
    SET_OF_BINARIES( "BS",
        ( o, value, convertor ) -> o.bs( ( ( List<byte[]> ) value ).stream().map( SdkBytes::fromByteArray ).collect( Collectors.toList() ) ),
        ( o, convertor ) -> {
            if( o == null || o.bs() == null && o.l() == null ) return null;
            return o.bs() != null && !o.bs().isEmpty()
                    ?  o.bs().stream().map( BytesWrapper::asByteArray ).collect( Collectors.toList() )
                    :  o.l().stream().map( av -> av.b().asByteArray() ).collect( Collectors.toList() );
        },
        o -> o == null ? 0 : ( ( List<byte[]> ) o ).stream().map( v -> v.length ).reduce( 0, Integer::sum )
    ),
    SET_OF_STRINGS( "SS",
        ( o, value, convertor ) -> o.ss( new ArrayList<>( ( Collection<String> ) value ) ),
        ( o, convertor ) -> {
            if ( o == null || o.ss() == null && o.l() == null ) return null;
            return o.ss() != null && !o.ss().isEmpty()
                    ? o.ss()
                    : o.l().stream().map( AttributeValue::s ).collect( Collectors.toList() );
        },
        o -> o == null ? 0 : ( ( Collection<Object> ) o ).stream().map( v -> getLength( v.toString() ) ).reduce( 0, Integer::sum )
    ),
    SET_OF_NUMBERS( "NS",
        ( o, value, convertor ) -> o.ns( new ArrayList<>( ( Collection<Number> ) value ).stream().map( String::valueOf ).collect( Collectors.toList() ) ),
        ( o, convertor ) -> {
            if ( o == null || o.ns() == null && o.l() == null ) return null;
            return o.ns() != null && !o.ns().isEmpty()
                    ? o.ns().stream().map( Double::parseDouble ).collect( Collectors.toList() )
                    : o.l().stream().map( v -> Double.parseDouble( v.n() ) ).collect( Collectors.toList() );
        },
        o -> o == null ? 0 : ( ( Collection<Number> ) o ).stream().map( n -> getLength( String.valueOf( n ) ) ).reduce( 0, Integer::sum )
    );

    private final ScalarAttributeType scalarAttributeType;
    private final DynamoDbToAttributeValueConvertor convertorTo;
    private final DynamoDbAttributeValueSizeCalculator sizeCalculator;
    private final DynamoDBFromAttributeValueConvertor convertorFrom;

    DynamodbDatatype( String scalarAttributeType,
                      DynamoDbToAttributeValueConvertor convertorTo,
                      DynamoDBFromAttributeValueConvertor convertorFrom,
                      DynamoDbAttributeValueSizeCalculator sizeCalculator ) {
        this.scalarAttributeType = ScalarAttributeType.fromValue( scalarAttributeType );
        this.convertorTo = convertorTo;
        this.sizeCalculator = sizeCalculator;
        this.convertorFrom = convertorFrom;
    }

    public ScalarAttributeType getScalarAttributeType() {
        return scalarAttributeType;
    }

    private static int getLength( String o ) {
        try {
            return o.getBytes( CHARACTER_ENCODING ).length;
        } catch ( Exception e ) {
            throw new RuntimeException( "Cannot detect length of '" + o + "'", e );
        }
    }

    public static Object fromAttributeValue( DynamodbDatatype type, AttributeValue attributeValue ) {
        return fromAttributeValue( type, attributeValue, null );
    }

    public static Object fromAttributeValue( DynamodbDatatype type, AttributeValue attributeValue, KeyConvertorForMap convertor ) {
        return type.convertorFrom.convert( attributeValue, convertor );
    }

    public static Object createObjectFromAttributeValue( AttributeValue binValue ) {
        return createObjectFromAttributeValue( binValue, null );
    }

    public static Object createObjectFromAttributeValue( AttributeValue binValue, KeyConvertorForMap convertor ) {
        if ( binValue == null ) return null;
        return switch ( binValue.type() ) {
            case NUL -> null;
            case BOOL -> fromAttributeValue( DynamodbDatatype.BOOLEAN, binValue, null );
            case S -> fromAttributeValue( DynamodbDatatype.STRING, binValue, null );
            case B -> fromAttributeValue( DynamodbDatatype.BINARY, binValue, null );
            case N -> fromAttributeValue( DynamodbDatatype.NUMBER, binValue, null );
            case SS -> fromAttributeValue( SET_OF_STRINGS, binValue, null );
            case NS -> fromAttributeValue( SET_OF_NUMBERS, binValue, null );
            case BS -> fromAttributeValue( SET_OF_BINARIES, binValue, null );
            case L -> fromAttributeValue( LIST, binValue, null );
            case M -> fromAttributeValue( MAP, binValue, convertor );
            default -> throw new UnsupportedOperationException( "for type " + binValue.type() );
        };
    }

    public static AttributeValue createAttributeValueFromObject( Object binValue ) {
        return createAttributeValueFromObject( binValue, null );
    }

    public static AttributeValue createAttributeValueFromObject( Object binValue, KeyConvertorForMap convertor ) {
        AttributeValue value;
        if( binValue instanceof Boolean ) {
            return createAttributeValue( BOOLEAN, binValue );
        }
        if( binValue instanceof Number ) {
            return createAttributeValue( NUMBER, binValue );
        }
        if( binValue instanceof String ) {
            return createAttributeValue( STRING, binValue );
        }
        if( binValue instanceof byte[] ) {
            return createAttributeValue( BINARY, binValue );
        }
        if( binValue instanceof Set ) {
            return createAttributeValue( SET_OF_STRINGS, binValue );
        }
        if( binValue instanceof List ) {
            return createAttributeValue( LIST, binValue );
        }
        if( binValue instanceof Map ) {
            return createAttributeValue( MAP, binValue, convertor );
        }
        try {
            Pair<String, Map<String, AttributeValue>> binNamesAndValues = new PojoBeanToDynamoCreator<>().createExpressionsFromBean( binValue, "" );
            return AttributeValue.fromM( binNamesAndValues._2() );
        } catch ( ReflectiveOperationException e ) {
            throw new RuntimeException( e );
        }
    }

    public static DynamodbDatatype of( Class clazz ) {
        if ( clazz == null ) return DynamodbDatatype.NULL;
        return switch ( clazz.getCanonicalName() ) {
            case "java.lang.Boolean", "boolean" -> DynamodbDatatype.BOOLEAN;
            case "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Number" -> DynamodbDatatype.NUMBER;
            case "int", "long", "float", "double" -> DynamodbDatatype.NUMBER;
            case "java.lang.String" -> DynamodbDatatype.STRING;
            case "java.util.List" -> DynamodbDatatype.LIST;
            case "byte[]" -> DynamodbDatatype.BINARY;

            default -> DynamodbDatatype.MAP;
        };
    }

    public int size( Object obj ) {
        return sizeCalculator.size( obj );
    }

    private void apply( AttributeValue.Builder builder, Object value, KeyConvertorForMap convertor ) {
        convertorTo.convert( builder, value, convertor );
    }

    private static List<AttributeValue> convertToList( List<Object> toConvert ) {
        return toConvert.stream().map( DynamodbDatatype::createAttributeValueFromObject ).toList();
    }

    private static Map<Object, AttributeValue> convertToMap( Map<Object, Object> toConvert, KeyConvertorForMap convertor ) {
        Map<Object, AttributeValue> result = new HashMap<>();
        for ( Map.Entry<Object, Object> entry : toConvert.entrySet() ) {
            result.put( convertor == null ? entry.getKey() : convertor.convert( entry.getKey() ), createAttributeValueFromObject( entry.getValue() ) );
        }
        return result;
    }

    public static AttributeValue createAttributeValue( DynamodbDatatype type, Object value ) {
        return createAttributeValue( type, value, null );
    }

    public static AttributeValue createAttributeValue( DynamodbDatatype type, Object value, KeyConvertorForMap convertor ) {
        Objects.requireNonNull( type );
        AttributeValue.Builder builder = AttributeValue.builder();
        type.apply( builder, value, convertor );
        return builder.build();
    }
}
