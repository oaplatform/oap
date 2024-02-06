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

import lombok.extern.slf4j.Slf4j;
import oap.storage.dynamo.client.convertors.DynamodbDatatype;
import oap.util.Pair;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates attribute values from a Bean class.
 */
@Slf4j
public class PojoBeanToDynamoCreator<T> {

    public Pair<String, Map<String, AttributeValue>> createExpressionsFromBean( T bean, String prefix ) throws ReflectiveOperationException {
        Map<String, AttributeValue> result = new HashMap<>();
        StringBuilder expression = new StringBuilder( 1024 );

        for ( Field field : bean.getClass().getDeclaredFields() ) {
//            if ( !ReservedWords.isAttributeNameAppropriate( field.getName() ) ) {
//                throw new IllegalArgumentException( "The given bean '" + bean.getClass().getCanonicalName() + "' has a field '" + field.getName() + "' which name is not supported by DynamoDB SDK" );
//            }
            Object value = callGetterMethod( bean, field );
            if ( value == null ) {
                expression.append( field.getName() ).append( "=:" ).append( field.getName() ).append( ", " );
                result.put( prefix + field.getName(), AttributeValue.fromNul( true ) );
                continue;
            }
            AttributeValue attributeValue = null;
            if ( field.getGenericType().toString().equals( "java.util.Set<java.lang.String>" ) ) {
                attributeValue = AttributeValue.fromNs( new ArrayList<>( ( Set ) value ) );
            } else if ( field.getGenericType().toString().equals( "java.util.List<java.lang.String>" ) ) {
                attributeValue = AttributeValue.fromSs( ( List ) value );
            } else if ( field.getGenericType().toString().equals( "java.util.List<byte[]>" ) ) {
                attributeValue = AttributeValue.fromBs( ( ( List<byte[]> ) value ).stream().map( SdkBytes::fromByteArray ).collect( Collectors.toList() ) );
            } else if ( field.getGenericType().toString().equals( "java.util.Set<byte[]>" ) ) {
                attributeValue = AttributeValue.fromBs( ( ( Set<byte[]> ) value ).stream().map( SdkBytes::fromByteArray ).collect( Collectors.toList() ) );
            } else if ( field.getGenericType().toString().startsWith( "java.util.List<java.lang." ) ) {
                //numbers
                List<String> collect = ( ( List<Number> ) value ).stream().map( Object::toString ).collect( Collectors.toList() );
                attributeValue = AttributeValue.fromNs( collect );
            } else if ( field.getGenericType().toString().startsWith( "java.util.Set<java.lang." ) ) {
                //numbers
                List<String> collect = ( ( Set<Number> ) value ).stream().map( Object::toString ).collect( Collectors.toList() );
                attributeValue = AttributeValue.fromNs( collect );
            } else {
                attributeValue = DynamodbDatatype.createAttributeValueFromObject( value );
            }
            expression.append( field.getName() ).append( "=:" ).append( field.getName() ).append( ", " );
            result.put( prefix + field.getName(), attributeValue );
        }
        if ( expression.length() > 2 ) {
            expression.setLength( expression.length() - ", ".length() );
        }
        return new Pair<>( expression.toString(), result );
    }

    public Map<String, AttributeValue> createFromBean( T bean ) throws ReflectiveOperationException {
        return createExpressionsFromBean( bean, "" )._2;
    }

    private Object callGetterMethod( T bean, Field field ) throws IllegalAccessException, InvocationTargetException {
        try {
            Method methodToCall = findGetterMethodForField( field, bean.getClass() );
            if ( methodToCall.canAccess( bean ) ) {
                if ( methodToCall.getAnnotation( DynamoDbIgnore.class ) != null ) {
                    // ignore field with @DynamoDbIgnore anno
                    return null;
                }
                return methodToCall.invoke( bean );
            }
        } catch ( NoSuchMethodException ex ) {
            boolean exceptionLogged = false;
            for ( Method method : bean.getClass().getDeclaredMethods() ) {
                if ( method.canAccess( bean )
                        && method.getName().startsWith( "get" )
                        && !method.getReturnType().getCanonicalName().equals( "void" )
                        && method.getName().equals( "get" + toCapitalize( field.getName() ) ) ) {
                    //candidate
                    try {
                        return method.invoke( bean );
                    } catch ( Exception pex ) {
                        log.debug( "Cannot find appropriate setter for field '{}':{}", field.getName(), ex.getMessage() );
                    }
                    exceptionLogged = true;
                    break;
                }
            }
            if ( !exceptionLogged ) {
                log.debug( "Cannot find appropriate setter for field '{}':{}", field.getName(), ex.getMessage() );
            }
        }
        return null;
    }

    public static <T> Method findGetterMethodForField( Field field, Class<? extends T> clazz ) throws NoSuchMethodException {
        Objects.requireNonNull( field );
        Objects.requireNonNull( clazz );
        Method getterMethod = null;
        if ( field.getType().isPrimitive() && field.getType() == boolean.class ) {
            //for boolean method might be 'is'
            try {
                getterMethod = clazz.getMethod( "is" + toCapitalize( field.getName() ) );
            } catch ( NoSuchMethodException ex ) {
                getterMethod = clazz.getMethod( "get" + toCapitalize( field.getName() ) );
            }
        } else {
            getterMethod = clazz.getMethod( "get" + toCapitalize( field.getName() ) );
        }
        return getterMethod;
    }

    private static String toCapitalize( String name ) {
        if ( name.length() == 1 ) {
            return name.toUpperCase( Locale.ROOT );
        }
        if ( name.length() > 1 ) {
            return name.substring( 0, 1 ).toUpperCase( Locale.ROOT ) + name.substring( 1 );
        }
        throw new IllegalArgumentException( "Cannot find setter for '" + name + "'" );
    }

    public Map<String, AttributeValue> fromDynamo( T bean ) {
        TableSchema<T> schema = TableSchema.fromClass( ( Class<T> ) bean.getClass() );
        return schema.itemToMap( bean, false );
    }
}
