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

package oap.reflect;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by igor.petrenko on 01.09.2016.
 *
 */
public final class Types {
    public static Type getOptionalArgumentType( Type type ) {
        return ( ( ParameterizedType ) type ).getActualTypeArguments()[0];
    }

    public static boolean isInstance( Class<?> clazz, Type type ) {
        if( type instanceof Class ) return clazz.isAssignableFrom( ( Class ) type );
        else return isInstance( clazz, ( ( ParameterizedType ) type ).getRawType() );
    }

    public static boolean isPrimitive( Type type ) {
        return type instanceof ParameterizedType ? isPrimitive( ( ( ParameterizedType ) type ).getRawType() )
            : ( ( Class ) type ).isPrimitive();
    }

    public static boolean isPrimitiveOrWrapped( Type type ) {
        return type instanceof ParameterizedType ? isPrimitive( ( ( ParameterizedType ) type ).getRawType() )
            : ClassUtils.isPrimitiveOrWrapper( ( Class ) type );
    }

    public static String toJavaType( Type genericType ) {
        return genericType.getTypeName().replace( '$', '.' );
    }
}
