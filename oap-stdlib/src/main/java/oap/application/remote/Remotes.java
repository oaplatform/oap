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

package oap.application.remote;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ArrayTableSerializer;
import de.javakaffee.kryoserializers.guava.HashBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableTableSerializer;
import de.javakaffee.kryoserializers.guava.LinkedHashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ReverseListSerializer;
import de.javakaffee.kryoserializers.guava.TreeBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.TreeMultimapSerializer;
import de.javakaffee.kryoserializers.guava.UnmodifiableNavigableSetSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaDateTimeSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalDateSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalDateTimeSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalTimeSerializer;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * Created by igor.petrenko on 2020-04-27.
 */
public interface Remotes {
    int VERSION = 2;

    Pool<Kryo> kryoPool = new Pool<>( true, false, 8 ) {
        protected Kryo create() {
            var kryo = new Kryo();
            kryo.setRegistrationRequired( false );
            kryo.setReferences( true );
            kryo.register( RemoteInvocation.class );
            kryo.register( RemoteInvocationException.class );
            kryo.setInstantiatorStrategy( new DefaultInstantiatorStrategy( new StdInstantiatorStrategy() ) );

            UnmodifiableCollectionsSerializer.registerSerializers( kryo );
            SynchronizedCollectionsSerializer.registerSerializers( kryo );

            // joda DateTime, LocalDate, LocalDateTime and LocalTime
            kryo.register( DateTime.class, new JodaDateTimeSerializer() );
            kryo.register( LocalDate.class, new JodaLocalDateSerializer() );
            kryo.register( LocalDateTime.class, new JodaLocalDateTimeSerializer() );
            kryo.register( LocalDateTime.class, new JodaLocalTimeSerializer() );

            // guava ImmutableList, ImmutableSet, ImmutableMap, ImmutableMultimap, ImmutableTable, ReverseList, UnmodifiableNavigableSet
            ImmutableListSerializer.registerSerializers( kryo );
            ImmutableSetSerializer.registerSerializers( kryo );
            ImmutableMapSerializer.registerSerializers( kryo );
            ImmutableMultimapSerializer.registerSerializers( kryo );
            ImmutableTableSerializer.registerSerializers( kryo );
            ReverseListSerializer.registerSerializers( kryo );
            UnmodifiableNavigableSetSerializer.registerSerializers( kryo );

            // guava ArrayListMultimap, HashMultimap, LinkedHashMultimap, LinkedListMultimap, TreeMultimap, ArrayTable, HashBasedTable, TreeBasedTable
            ArrayListMultimapSerializer.registerSerializers( kryo );
            HashMultimapSerializer.registerSerializers( kryo );
            LinkedHashMultimapSerializer.registerSerializers( kryo );
            LinkedListMultimapSerializer.registerSerializers( kryo );
            TreeMultimapSerializer.registerSerializers( kryo );
            ArrayTableSerializer.registerSerializers( kryo );
            HashBasedTableSerializer.registerSerializers( kryo );
            TreeBasedTableSerializer.registerSerializers( kryo );

            return kryo;
        }
    };

    Pool<Output> outputPool = new Pool<>( true, false, 16 ) {
        protected Output create() {
            return new Output( 1024, -1 );
        }
    };

    Pool<Input> inputPool = new Pool<>( true, false, 16 ) {
        protected Input create() {
            return new Input( 1024 );
        }
    };
}
