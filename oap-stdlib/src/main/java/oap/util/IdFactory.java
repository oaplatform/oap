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

package oap.util;

import lombok.SneakyThrows;
import lombok.val;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by igor.petrenko on 04.01.2018.
 */
public class IdFactory {
    private static final ConcurrentHashMap<Class, Field> ids = new ConcurrentHashMap<>();

    @SneakyThrows
    public static String getId( Object value ) {
        final Field field = getField( value );

        return ( String ) field.get( value );
    }

    private static Field getField( Object value ) {
        return ids.computeIfAbsent( value.getClass(), ( c ) -> {
            val df = Stream.of( c.getDeclaredFields() )
                .filter( f -> f.getAnnotation( Id.class ) != null )
                .findAny()
                .orElseThrow( () -> new RuntimeException( "no @Id annotation" ) );
            df.setAccessible( true );
            return df;
        } );
    }

    @SneakyThrows
    public static void setId( Object value, String id ) {
        getField( value ).set( value, id );
    }
}
