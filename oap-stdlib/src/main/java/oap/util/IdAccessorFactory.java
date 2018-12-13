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

import lombok.val;
import oap.reflect.Reflect;
import oap.reflect.Reflection;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by igor.petrenko on 04.01.2018.
 */
public class IdAccessorFactory {
    private static final ConcurrentHashMap<Class, Accessor> ids = new ConcurrentHashMap<>();

    public static String getter( Object value ) {
        return get( value.getClass() ).get( value );
    }

    private static Accessor get( Class<?> clazz ) {
        return ids.computeIfAbsent( clazz, c -> {
            Reflection reflect = Reflect.reflect( c );

            val idFields = reflect.annotatedFields( Id.class );
            if( !idFields.isEmpty() ) return new FieldAccessor( Lists.head( idFields ) );

            val idMethods = reflect.annotatedMethods( Id.class );

            Reflection.Method setter = null;
            Reflection.Method getter = null;


            for( Reflection.Method m : idMethods ) {
                if( m.returnType().assignableTo( String.class ) ) getter = m;
                else setter = m;
            }

            if( setter == null || getter == null ) throw new RuntimeException( "no @Id annotation" );

            return new MethodAccessor( setter, getter );
        } );
    }

    public static void setter( Object value, String id ) {
        get( value.getClass() ).set( value, id );
    }

    public interface Accessor {
        void set( Object object, String id );

        String get( Object object );
    }

    private static class FieldAccessor implements Accessor {
        private final Reflection.Field field;

        public FieldAccessor( Reflection.Field field ) {
            this.field = field;
        }

        @Override
        public void set( Object object, String id ) {
            field.set( object, id );
        }

        @Override
        public String get( Object object ) {
            return ( String ) field.get( object );
        }
    }

    private static class MethodAccessor implements Accessor {
        private final Reflection.Method setter;
        private final Reflection.Method getter;

        public MethodAccessor( Reflection.Method setter, Reflection.Method getter ) {
            this.setter = setter;
            this.getter = getter;
        }

        @Override
        public void set( Object object, String id ) {
            setter.invoke( object, id );
        }

        @Override
        public String get( Object object ) {
            return ( String ) getter.invoke( object );
        }
    }
}
