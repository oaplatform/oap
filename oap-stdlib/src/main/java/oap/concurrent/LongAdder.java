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

package oap.concurrent;

import lombok.SneakyThrows;
import oap.util.Throwables;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Objects;

public class LongAdder extends java.util.concurrent.atomic.LongAdder {
    private static Field baseField;

    static {
        try {
            baseField = java.util.concurrent.atomic.LongAdder.class.getSuperclass().getDeclaredField( "base" );
            baseField.setAccessible( true );
        } catch( ReflectiveOperationException e ) {
            throw Throwables.propagate( e );
        }
    }


    public LongAdder() {
    }

    public LongAdder( long value ) {
        setValue( value );
    }

    @SneakyThrows
    private void setValue( long value ) {
        baseField.set( this, value );
    }

    @Override
    public boolean equals( Object obj ) {
        return sum() == ( ( LongAdder ) Objects.requireNonNull( obj ) ).sum();
    }

    @Override
    public int hashCode() {
        return Long.hashCode( sum() );
    }

    private Object writeReplace() {
        return new SerializationProxy( this );
    }

    private void readObject( java.io.ObjectInputStream s )
        throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException( "Proxy required" );
    }

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 5418714082658153445L;

        private final long value;

        SerializationProxy( LongAdder a ) {
            value = a.sum();
        }

        private Object readResolve() {
            return new LongAdder( value );
        }
    }
}
