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
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Storage<T> extends Closeable, Iterable<T>, Function<String, Optional<T>> {
    <M> M updateMetadata( String id, Function<M, M> func );

    default Object getDefaultMetadata(T object) {
        return null;
    }

    <M> M getMetadata( String id );

    <M> Stream<M> selectMetadata();

    Stream<T> select();

    T store( T object );

    void store( Collection<T> objects );

    default Optional<T> update( String id, Function<T, T> update ) {
        return update( id, update, null );
    }

    default Optional<T> update( String id, Predicate<T> predicate, Function<T, T> update ) {
        return update( id, predicate, update, null );
    }

    Optional<T> update( String id, T object );

    Optional<T> update( String id, Predicate<T> predicate, Function<T, T> update, Supplier<T> init );

    default Optional<T> update( String id, Function<T, T> update, Supplier<T> init ) {
        return update( id, t -> true, update, init );
    }

    default void update( Collection<String> ids, Function<T, T> update ) {
        update( ids, t -> true, update, null );
    }

    default void update( Collection<String> ids, Predicate<T> predicate, Function<T, T> update ) {
        update( ids, predicate, update, null );
    }

    default void update( Collection<String> ids, Function<T, T> update, Supplier<T> init ) {
        update( ids, t -> true, update, init );
    }

    void update( Collection<String> ids, Predicate<T> predicate, Function<T, T> update, Supplier<T> init );

    Optional<T> get( String id );

    default Optional<T> apply( String id ) {
        return get( id );
    }

    Optional<T> delete( String id );

    void deleteAll();

    long size();

    Storage<T> copyAndClean();

    void fsync();

    Map<String, T> toMap();

    void addDataListener( DataListener<T> dataListener );

    void removeDataListener( DataListener<T> dataListener );

    interface DataListener<T> {
        @Deprecated
        /**
         * updated( T object, boolean isNew )
         */
        default void updated( T object ) {
        }

        default void updated( T object, boolean added ) {
            updated( object );
        }


        @Deprecated
        /**
         * updated( Collection<T> objects, boolean isNew )
         */
        default void updated( Collection<T> objects ) {
        }

        default void updated( Collection<T> objects, boolean added ) {
            updated( objects );

            objects.forEach( obj -> updated( obj, added ) );
        }


        default void deleted( T object ) {
        }

        default void deleted( Collection<T> objects ) {
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
