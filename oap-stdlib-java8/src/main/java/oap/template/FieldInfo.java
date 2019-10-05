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

package oap.template;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.util.Lists;
import org.apache.commons.lang3.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by igor.petrenko on 2019-09-30.
 */
@ToString
@Slf4j
public class FieldInfo {
    public final String field;
    public final Type type;
    public final Annotation[] annotations;

    public FieldInfo( Type type ) {
        this( null, type, new Annotation[0] );
    }

    public FieldInfo( String field, Type type, Annotation[] annotations ) {
        if( log.isTraceEnabled() )
            log.trace( "field = {}, type = {}, annotations = {}", field, type, Lists.of( annotations ) );

        this.field = field;
        this.type = type;
        this.annotations = annotations;
    }

    public FieldInfo( String field, Field declaredField ) {
        this( field, declaredField.getGenericType(), declaredField.getDeclaredAnnotations() );
    }

    public static boolean isPrimitive( Type type ) {
        return type instanceof ParameterizedType ? isPrimitive( ( ( ParameterizedType ) type ).getRawType() )
            : ( ( Class ) type ).isPrimitive();
    }

    public static boolean isInstance( Class<?> clazz, Type type ) {
        if( type instanceof Class ) return clazz.isAssignableFrom( ( Class ) type );
        else return isInstance( clazz, ( ( ParameterizedType ) type ).getRawType() );
    }

    public boolean isOptional() {
        return type instanceof ParameterizedType
            && ( ( ParameterizedType ) type ).getRawType().equals( Optional.class );
    }

    public boolean isNullable() {
        return Lists.of( annotations ).stream().anyMatch( a -> a.annotationType().equals( Template.Nullable.class ) );
    }

    public FieldInfo getOptionalArgumentType() {
        return new FieldInfo( field, ( ( ParameterizedType ) type ).getActualTypeArguments()[0], annotations );
    }

    public String toJavaType() {
        return type.getTypeName().replace( '$', '.' );
    }

    private boolean isMap( Type type ) {
        if( type instanceof ParameterizedType )
            return isMap( ( ( ParameterizedType ) type ).getRawType() );

        return ( ( Class ) type ).isAssignableFrom( Map.class );
    }

    public boolean isMap() {
        return isMap( type );
    }

    public boolean isPrimitive() {
        return isPrimitive( type );
    }

    public boolean isPrimitiveOrWrapped() {
        return type instanceof ParameterizedType ? isPrimitive( ( ( ParameterizedType ) type ).getRawType() )
            : ClassUtils.isPrimitiveOrWrapper( ( Class ) type );
    }

    public boolean isInstance( Class<?> clazz ) {
        return isInstance( clazz, type );
    }

    public FieldInfo getRawType() {
        return new FieldInfo( field, ( ( ParameterizedType ) type ).getRawType(), annotations );
    }

    public boolean isParameterizedType() {
        return type instanceof ParameterizedType;
    }
}
