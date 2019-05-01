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
import stormpot.Allocator;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.Pool;
import stormpot.Poolable;
import stormpot.Slot;
import stormpot.Timeout;

import java.io.Writer;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * Created by igor.petrenko on 01.05.2019.
 */
public class StringBuilderPool {
    private static final Timeout TIMEOUT = new Timeout( 10, TimeUnit.MICROSECONDS );
    private Pool<StringBuilderWriter> pool;

    public StringBuilderPool() {
        var allocator = new StringBuilderAllocator();
        var config = new Config<StringBuilderWriter>()
            .setBackgroundExpirationEnabled( false )
            .setExpiration( info -> false )
            .setAllocator( allocator );
        pool = new BlazePool<>( config );
    }

    @SneakyThrows
    public StringBuilderWriter claim() {
        var claim = pool.claim( TIMEOUT );
        while( claim == null ) claim = pool.claim( TIMEOUT );

        claim.reset();
        return claim;
    }

    private static class StringBuilderAllocator implements Allocator<StringBuilderWriter> {

        @Override
        public final StringBuilderWriter allocate( Slot slot ) {
            return new StringBuilderWriter( slot );
        }

        @Override
        public final void deallocate( StringBuilderWriter stringBuilder ) {
            stringBuilder.release();
        }
    }

    public static class StringBuilderWriter extends Writer implements Poolable {
        private static Field countField;

        static {
            try {
                countField = StringBuilder.class.getSuperclass().getDeclaredField( "count" );
                countField.setAccessible( true );
            } catch( NoSuchFieldException e ) {
                throw Throwables.propagate( e );
            }
        }

        public final StringBuilder sb = new StringBuilder();
        private final Slot slot;

        public StringBuilderWriter( Slot slot ) {
            this.slot = slot;
        }

        @Override
        public void write( char[] cbuf, int off, int len ) {
            sb.append( cbuf, off, len );
        }

        @Override
        public void write( char[] cbuf ) {
            sb.append( cbuf );
        }

        @Override
        public void write( String str ) {
            sb.append( str );
        }

        @Override
        public void write( String str, int off, int len ) {
            sb.append( str, off, len );
        }

        @Override
        public void write( int c ) {
            sb.append( ( char ) c );
        }

        @Override
        public Writer append( char c ) {
            sb.append( c );
            return this;
        }

        @Override
        public Writer append( CharSequence csq ) {
            sb.append( csq );
            return this;
        }

        @Override
        public Writer append( CharSequence csq, int start, int end ) {
            sb.append( csq, start, end );
            return this;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public void release() {
            slot.release( this );
        }

        @Override
        public String toString() {
            return sb.toString();
        }

        @SneakyThrows
        public void reset() {
            countField.set( sb, 0 );
        }
    }
}
