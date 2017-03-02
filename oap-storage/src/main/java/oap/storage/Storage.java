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

import oap.util.Stream;

import java.io.Closeable;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Storage<T> extends Closeable {
    Stream<T> select();

    default <R> R lock( String id, Supplier<R> run ) {
        synchronized( id.intern() ) {
            return run.get();
        }
    }

    default void lock( String id, Runnable run ) {
        synchronized( id.intern() ) {
            run.run();
        }
    }

    void store( T object );

    void store( Collection<T> objects );

    Optional<T> update( String id, Consumer<T> update );

    Optional<T> update( String id, Consumer<T> update, Supplier<T> init );

    void update( Collection<String> ids, Consumer<T> update );

    void update( Collection<String> ids, Consumer<T> update, Supplier<T> init );

    Optional<T> get( String id );

    void delete( String id );

    void deleteAll();

    long size();

    void addDataListener( DataListener<T> dataListener );

    void removeDataListener( DataListener<T> dataListener );

    interface DataListener<T> {
        @Deprecated
        /**
         * updated( T object, boolean isNew )
         */
        default void updated( T object ) {
        }

        default void updated( T object, boolean isNew ) {
            updated( object );
        }


        @Deprecated
        /**
         * updated( Collection<T> objects, boolean isNew )
         */
        default void updated( Collection<T> objects ) {
        }

        default void updated( Collection<T> objects, boolean isNew ) {
            updated( objects );

            objects.forEach( obj -> updated( obj, isNew ) );
        }


        default void deleted( T object ) {
        }

        default void deleted( Collection<T> objects ) {
            objects.forEach( this::deleted );
        }

    }
}
