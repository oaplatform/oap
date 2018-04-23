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

package oap.storage;

import oap.concurrent.Threads;
import oap.util.Stream;

import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by igor.petrenko on 23.04.2018.
 */
public interface ROStorage<T> extends Closeable, Iterable<T>, Function<String, Optional<T>> {
    Identifier<T> getIdentifier();

    Stream<T> select();

    Optional<T> get( String id );

    Optional<T> apply( String id );

    long size();

    Map<String, T> toMap();

    void addDataListener( DataListener<T> dataListener );

    void removeDataListener( DataListener<T> dataListener );

    interface DataListener<T2> {
        @Deprecated
        /**
         * updated( T object, boolean isNew )
         */
        default void updated( T2 object ) {
        }

        default void updated( T2 object, boolean added ) {
            updated( object );
        }


        @Deprecated
        /**
         * updated( Collection<T> objects, boolean isNew )
         */
        default void updated( Collection<T2> objects ) {
        }

        default void updated( Collection<T2> objects, boolean added ) {
            updated( objects );

            objects.forEach( obj -> updated( obj, added ) );
        }


        default void deleted( T2 object ) {
        }

        default void deleted( Collection<T2> objects ) {
            objects.forEach( this::deleted );
        }

    }

    interface LockStrategy {
        LockStrategy NoLock = new NoLock();
        LockStrategy Lock = new Lock();

        void synchronizedOn( String id, Runnable run );

        <R> R synchronizedOn( String id, Supplier<R> run );

        final class NoLock implements LockStrategy {
            @Override
            public final void synchronizedOn( String id, Runnable run ) {
                run.run();
            }

            @Override
            public final <R> R synchronizedOn( String id, Supplier<R> run ) {
                return run.get();
            }
        }

        final class Lock implements LockStrategy {
            @Override
            public final void synchronizedOn( String id, Runnable run ) {
                Threads.synchronizedOn( id, run );
            }

            @Override
            public final <R> R synchronizedOn( String id, Supplier<R> run ) {
                return Threads.synchronizedOn( id, run );
            }
        }
    }
}
