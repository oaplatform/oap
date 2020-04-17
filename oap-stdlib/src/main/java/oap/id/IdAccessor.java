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

package oap.id;

import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Lists;

import java.util.concurrent.ConcurrentHashMap;

public class IdAccessor {
    private static final ConcurrentHashMap<Class<?>, Accessor<?>> ids = new ConcurrentHashMap<>();

    public static <I> I get( Object value ) {
        return IdAccessor.<I>accessor( value.getClass() ).get( value );
    }

    @SuppressWarnings( "unchecked" )
    private static <I> Accessor<I> accessor( Class<?> clazz ) {
        return ( Accessor<I> ) ids.computeIfAbsent( clazz, c -> {
            Reflection reflect = Reflect.reflect( c );

            var idFields = reflect.annotatedFields( Id.class );
            if( !idFields.isEmpty() ) return new FieldAccessor<I>( Lists.head( idFields ) );

            var idMethods = reflect.annotatedMethods( Id.class );

            Reflection.Method setter = null;
            Reflection.Method getter = null;

            for( Reflection.Method m : idMethods )
                if( m.returnType().assignableTo( String.class ) ) getter = m;
                else setter = m;

            if( setter == null || getter == null ) throw new IllegalArgumentException( "no @Id annotation" );

            return new MethodAccessor<I>( setter, getter );
        } );
    }

    public static void set( Object value, String id ) {
        accessor( value.getClass() ).set( value, id );
    }

    public interface Accessor<I> {
        void set( Object object, I id );

        I get( Object object );
    }

    private static class FieldAccessor<I> implements Accessor<I> {
        private final Reflection.Field field;

        public FieldAccessor( Reflection.Field field ) {
            this.field = field;
        }

        @Override
        public void set( Object object, I id ) {
            field.set( object, id );
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public I get( Object object ) {
            return ( I ) field.get( object );
        }
    }

    private static class MethodAccessor<I> implements Accessor<I> {
        private final Reflection.Method setter;
        private final Reflection.Method getter;

        public MethodAccessor( Reflection.Method setter, Reflection.Method getter ) {
            this.setter = setter;
            this.getter = getter;
        }

        @Override
        public void set( Object object, I id ) {
            setter.invoke( object, id );
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public I get( Object object ) {
            return ( I ) getter.invoke( object );
        }
    }
}
