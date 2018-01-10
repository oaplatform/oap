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
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import static oap.util.Collections.head2;

/**
 * Created by igor.petrenko on 04.01.2018.
 */
public class IdFactory {
    private static final ConcurrentHashMap<Class, IdAccess> ids = new ConcurrentHashMap<>();

    public static String getId( Object value ) {
        val idAccess = get( value );

        return idAccess.get( value );
    }

    private static IdAccess get( Object value ) {
        return ids.computeIfAbsent( value.getClass(), ( c ) -> {
            val idFields = ReflectionUtils.getAllFields( c, ( f ) -> f.getAnnotation( Id.class ) != null );
            if( !idFields.isEmpty() ) return new FieldIdAccess( head2( idFields ) );

            val idMethods = ReflectionUtils.getAllMethods( c, ( m ) -> m.getAnnotation( Id.class ) != null );

            Method setter = null;
            Method getter = null;


            for( val m : idMethods ) {
                if( String.class.equals( m.getReturnType() ) ) getter = m;
                else setter = m;
            }

            if( setter == null || getter == null ) throw new RuntimeException( "no @Id annotation" );

            return new MethodIdAccess( setter, getter );
        } );
    }

    public static void setId( Object value, String id ) {
        get( value ).set( value, id );
    }

    public interface IdAccess {
        void set( Object object, String id );

        String get( Object object );
    }

    private static class FieldIdAccess implements IdAccess {
        private final Field field;

        public FieldIdAccess( Field field ) {
            this.field = field;
            this.field.setAccessible( true );
        }

        @Override
        @SneakyThrows
        public void set( Object object, String id ) {
            field.set( object, id );
        }

        @Override
        @SneakyThrows
        public String get( Object object ) {
            return ( String ) field.get( object );
        }
    }

    private static class MethodIdAccess implements IdAccess {
        private final Method setter;
        private final Method getter;

        public MethodIdAccess( Method setter, Method getter ) {
            this.setter = setter;
            this.getter = getter;
        }

        @Override
        @SneakyThrows
        public void set( Object object, String id ) {
            setter.invoke( object, id );
        }

        @Override
        @SneakyThrows
        public String get( Object object ) {
            return ( String ) getter.invoke( object );
        }
    }
}
