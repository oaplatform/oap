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

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import static oap.util.Pair.__;
import static oap.util.function.Functions.empty.consume;

public class Stream<E> implements java.util.stream.Stream<E>, Closeable, AutoCloseable {
    private final java.util.stream.Stream<E> underlying;

    protected Stream( java.util.stream.Stream<E> underlying ) {
        this.underlying = underlying;
    }

    public static <T> Stream<T> empty() {
        return of( java.util.stream.Stream.empty() );
    }

    public static <T> Stream<T> of( java.util.stream.Stream<T> stream ) {
        return new Stream<>( stream );
    }

    public static <T> Stream<T> of( Iterator<T> iterator ) {
        return of( StreamSupport.stream( Spliterators.spliteratorUnknownSize( iterator, 0 ), false ) );
    }

    public static <T> Stream<T> of( Collection<T> collection ) {
        return of( collection.stream() );
    }

    public static <T> Stream<T> of( Enumeration<T> enumeration ) {
        return of( com.google.common.collect.Iterators.forEnumeration( enumeration ) );
    }

    public static <T> Stream<T> of( Supplier<Boolean> hasNext, Supplier<T> next ) {
        return of( Iterators.of( hasNext, next ) );
    }

    public static <T> Stream<T> of( T initialState, Predicate<T> hasNext, UnaryOperator<T> next ) {
        return of( Iterators.of( initialState, hasNext, next ) );
    }

    @SafeVarargs
    public static <T> Stream<T> of( T... values ) {
        return values == null ? empty() : of( java.util.stream.Stream.of( values ) );
    }

    public static <T> Stream<T> traverse( T initialState, UnaryOperator<T> traverse ) {
        return of( Iterators.traverse( initialState, traverse ) );
    }

    public static <T> Stream<T> flatTraverse( T initialState, Function<T, java.util.stream.Stream<T>> traverse ) {
        return of( Iterators.flatTraverse( initialState, e -> traverse.apply( e ).iterator() ) );
    }


    public static <T> Stream<T> iterate( T seed, UnaryOperator<T> f ) {
        return of( java.util.stream.Stream.iterate( seed, f ) );
    }

    public static <T> Stream<T> generate( Supplier<T> s ) {
        return of( java.util.stream.Stream.generate( s ) );
    }

    public Stream<E> takeWhile( Predicate<? super E> predicate ) {
        return of( StreamSupport.stream( takeWhile( spliterator(), predicate ), underlying.isParallel() ) );
    }

    static <T> Spliterator<T> takeWhile( Spliterator<T> spliterator, Predicate<? super T> predicate ) {
        return new Spliterators.AbstractSpliterator<>( spliterator.estimateSize(), 0 ) {
            boolean stillGoing = true;

            @Override
            public boolean tryAdvance( Consumer<? super T> consumer ) {
                if( stillGoing ) {
                    boolean hadNext = spliterator.tryAdvance( elem -> {
                        if( predicate.test( elem ) ) consumer.accept( elem );
                        else stillGoing = false;
                    } );
                    return hadNext && stillGoing;
                }
                return false;
            }
        };
    }

    public Stream<List<E>> grouped( int batchSize ) {
        return of( StreamSupport.stream( new BatchSpliterator<>( underlying.spliterator(), batchSize ), underlying.isParallel() ) );
    }

    /**
     * heavy operation
     * make use of SortedOps/StatefulOps
     */
    public <K> BiStream<K, List<E>> grouped( Function<E, K> classifier ) {
        HashMap<K, List<E>> result = new HashMap<>();

        Iterator<E> iterator = iterator();
        while( iterator.hasNext() ) {
            E e = iterator.next();
            K key = classifier.apply( e );
            result.computeIfAbsent( key, k -> new ArrayList<>() ).add( e );
        }
        return BiStream.of( result );
    }

    public <B, C> Stream<C> zip( java.util.stream.Stream<? extends B> b,
                                 BiFunction<? super E, ? super B, ? extends C> zipper ) {
        Objects.requireNonNull( zipper );
        Spliterator<E> aSpliterator = underlying.spliterator();
        Spliterator<? extends B> bSpliterator = b.spliterator();

        // Zipping looses DISTINCT and SORTED characteristics
        int both = aSpliterator.characteristics() & bSpliterator.characteristics()
            & ~( Spliterator.DISTINCT | Spliterator.SORTED );

        long zipSize = ( ( both & Spliterator.SIZED ) != 0 )
            ? Math.min( aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown() )
            : -1;

        Iterator<C> it = new Iterator<>() {
            final Iterator<E> aIterator = Spliterators.iterator( aSpliterator );
            final Iterator<B> bIterator = Spliterators.iterator( bSpliterator );

            @Override
            public boolean hasNext() {
                return aIterator.hasNext() && bIterator.hasNext();
            }

            @Override
            public C next() {
                return zipper.apply( aIterator.next(), bIterator.next() );
            }
        };

        return of( StreamSupport.stream(
            Spliterators.spliterator( it, zipSize, both ),
            underlying.isParallel() || b.isParallel() ) );
    }

    public <B> BiStream<E, B> zip( java.util.stream.Stream<? extends B> b ) {
        return BiStream.of( zip( b, Pair::__ ) );
    }

    public BiStream<E, Integer> zipWithIndex() {
        return zip( of( IntStream.iterate( 0, i -> i + 1 ).boxed() ) );
    }

    public Pair<Stream<E>, Stream<E>> partition( Predicate<E> criteria ) {
        final Iterator<E> it = this.iterator();
        final LinkedList<E> buffer1 = new LinkedList<>();
        final LinkedList<E> buffer2 = new LinkedList<>();

        class Partition implements Iterator<E> {

            final boolean b;

            Partition( boolean b ) {
                this.b = b;
            }

            void fetch() {
                while( buffer( b ).isEmpty() && it.hasNext() ) {
                    E next = it.next();
                    buffer( criteria.test( next ) ).offer( next );
                }
            }

            LinkedList<E> buffer( boolean test ) {
                return test ? buffer1 : buffer2;
            }

            @Override
            public boolean hasNext() {
                fetch();
                return !buffer( b ).isEmpty();
            }

            @Override
            public E next() {
                return buffer( b ).poll();
            }
        }

        return __( of( new Partition( true ) ), of( new Partition( false ) ) );
    }

    //    @todo create monadic stream processing
    public <R, X> Result<List<R>, X> tryMap( Function<? super E, Result<? extends R, ? extends X>> mapper ) {
        Iterator<E> iterator = this.iterator();
        LinkedList<R> container = new LinkedList<>();
        while( iterator.hasNext() ) {
            Result<? extends R, ? extends X> res = mapper.apply( iterator.next() );
            if( res.isSuccess() ) container.add( res.successValue );
            else return Result.failure( res.failureValue );
        }
        return Result.success( container );
    }

    public <T> Stream<Result<T, ? extends Throwable>> mapThrowableToResult( Function<? super E, ? extends T> mapper ) {
        return map( f -> {
            try {
                return Result.success( mapper.apply( f ) );
            } catch( Throwable t ) {
                return Result.failure( t );
            }
        } );
    }


    @Override
    public Stream<E> filter( Predicate<? super E> predicate ) {
        return of( underlying.filter( predicate ) );
    }

    @Override
    public <R> Stream<R> map( Function<? super E, ? extends R> mapper ) {
        return of( underlying.map( mapper ) );
    }

    public <R> Stream<R> mapWithIndex( BiFunction<Long, E, R> mapper ) {
        AtomicLong index = new AtomicLong( 0 );
        return map( e -> mapper.apply( index.getAndIncrement(), e ) );
    }

    @Override
    public IntStream mapToInt( ToIntFunction<? super E> mapper ) {
        return underlying.mapToInt( mapper );
    }

    @Override
    public LongStream mapToLong( ToLongFunction<? super E> mapper ) {
        return underlying.mapToLong( mapper );
    }

    @Override
    public DoubleStream mapToDouble( ToDoubleFunction<? super E> mapper ) {
        return underlying.mapToDouble( mapper );
    }

    public <A, B> BiStream<A, B> mapToPairs( Function<E, Pair<A, B>> mapper ) {
        return BiStream.of( map( mapper ) );
    }

    @Override
    public <R> Stream<R> flatMap( Function<? super E, ? extends java.util.stream.Stream<? extends R>> mapper ) {
        return of( underlying.flatMap( mapper ) );
    }

    public <R> Stream<R> flatMapOptional( Function<? super E, Optional<? extends R>> mapper ) {
        return of( underlying.map( mapper ).filter( Optional::isPresent ).map( Optional::get ) );
    }

    @Override
    public IntStream flatMapToInt( Function<? super E, ? extends IntStream> mapper ) {
        return underlying.flatMapToInt( mapper );
    }

    @Override
    public LongStream flatMapToLong( Function<? super E, ? extends LongStream> mapper ) {
        return underlying.flatMapToLong( mapper );
    }

    @Override
    public DoubleStream flatMapToDouble( Function<? super E, ? extends DoubleStream> mapper ) {
        return underlying.flatMapToDouble( mapper );
    }

    @Override
    public Stream<E> distinct() {
        return of( underlying.distinct() );
    }

    public <T> Stream<E> distinctByProperty( Function<? super E, T> distinctPropertyExtractor ) {
        final Map<T, Boolean> seen = underlying.isParallel() ? new ConcurrentHashMap<>() : new HashMap<>();
        return filter( e -> seen.putIfAbsent( distinctPropertyExtractor.apply( e ), Boolean.TRUE ) == null );
    }

    @Override
    public Stream<E> sorted() {
        return of( underlying.sorted() );
    }

    @Override
    public Stream<E> sorted( Comparator<? super E> comparator ) {
        return of( underlying.sorted( comparator ) );
    }

    @Override
    public Stream<E> peek( Consumer<? super E> action ) {
        return of( underlying.peek( action ) );
    }

    @Override
    public Stream<E> limit( long maxSize ) {
        return of( underlying.limit( maxSize ) );
    }

    @Override
    public Stream<E> skip( long n ) {
        return of( underlying.skip( n ) );
    }

    @Override
    public void forEach( Consumer<? super E> action ) {
        underlying.forEach( action );
    }

    @Override
    public void forEachOrdered( Consumer<? super E> action ) {
        underlying.forEachOrdered( action );
    }

    public Set<E> toSet() {
        return underlying.collect( Collectors.toSet() );
    }

    @Override
    @Nonnull
    public Object[] toArray() {
        return underlying.toArray();
    }

    @Override
    @Nonnull
    public <A> A[] toArray( IntFunction<A[]> generator ) {
        return underlying.toArray( generator );
    }

    public <R> R foldLeft( R seed, BiFunction<R, ? super E, R> function ) {
        final Iterator<E> it = underlying.iterator();
        R result = seed;
        while( it.hasNext() ) result = function.apply( result, it.next() );
        return result;
    }

    @Override
    public E reduce( E identity, BinaryOperator<E> accumulator ) {
        return underlying.reduce( identity, accumulator );
    }

    @Override
    @Nonnull
    public Optional<E> reduce( BinaryOperator<E> accumulator ) {
        return underlying.reduce( accumulator );
    }

    @Override
    public <U> U reduce( U identity, BiFunction<U, ? super E, U> accumulator, BinaryOperator<U> combiner ) {
        return underlying.reduce( identity, accumulator, combiner );
    }

    @Override
    public <R> R collect( Supplier<R> supplier, BiConsumer<R, ? super E> accumulator, BiConsumer<R, R> combiner ) {
        return underlying.collect( supplier, accumulator, combiner );
    }

    @Override
    public <R, A> R collect( Collector<? super E, A, R> collector ) {
        return underlying.collect( collector );
    }

    @Override
    @Nonnull
    public Optional<E> min( Comparator<? super E> comparator ) {
        return underlying.min( comparator );
    }

    @Override
    @Nonnull
    public Optional<E> max( Comparator<? super E> comparator ) {
        return underlying.max( comparator );
    }

    @Override
    public long count() {
        return underlying.count();
    }

    public long count( Predicate<? super E> predicate ) {
        return underlying.filter( predicate ).count();
    }

    @Override
    public boolean anyMatch( Predicate<? super E> predicate ) {
        return underlying.anyMatch( predicate );
    }

    @Override
    public boolean allMatch( Predicate<? super E> predicate ) {
        return underlying.allMatch( predicate );
    }

    @Override
    public boolean noneMatch( Predicate<? super E> predicate ) {
        return underlying.noneMatch( predicate );
    }

    @Override
    @Nonnull
    public Optional<E> findFirst() {
        return underlying.findFirst();
    }

    @Override
    @Nonnull
    public Optional<E> findAny() {
        return underlying.findAny();
    }

    public <R> Optional<R> findFirstWithMap( Function<E, Optional<R>> mapper ) {
        return this.map( mapper )
            .filter( Optional::isPresent )
            .findFirst()
            .flatMap( e -> e );
    }

    public Optional<E> random() {
        return Lists.random( toList() );
    }

    public Stream<E> concat( java.util.stream.Stream<? extends E> b ) {
        return of( java.util.stream.Stream.concat( underlying, b ) );
    }

    @SafeVarargs
    public final Stream<E> concat( E value, E... values ) {
        return of(
            java.util.stream.Stream.concat(
                java.util.stream.Stream.concat( underlying,
                    java.util.stream.Stream.of( value ) ), java.util.stream.Stream.of( values ) ) );
    }

    @Override
    @Nonnull
    public Iterator<E> iterator() {
        return underlying.iterator();
    }

    public Enumeration<E> enumeration() {
        return com.google.common.collect.Iterators.asEnumeration( iterator() );
    }

    @Override
    @Nonnull
    public Spliterator<E> spliterator() {
        return underlying.spliterator();
    }

    @Override
    public boolean isParallel() {
        return underlying.isParallel();
    }

    @Override
    @Nonnull
    public Stream<E> sequential() {
        return of( underlying.sequential() );
    }

    @Override
    @Nonnull
    public Stream<E> parallel() {
        return of( underlying.parallel() );
    }

    @Override
    @Nonnull
    public Stream<E> unordered() {
        return of( underlying.unordered() );
    }

    @Override
    @Nonnull
    public Stream<E> onClose( Runnable closeHandler ) {
        return of( underlying.onClose( closeHandler ) );
    }

    @Override
    public void close() {
        underlying.close();
    }

    public void drain() {
        underlying.forEach( consume() );
        close();
    }

    private static class BatchSpliterator<E> implements Spliterator<List<E>> {
        private final Spliterator<E> base;
        private final int batchSize;

        private BatchSpliterator( Spliterator<E> base, int batchSize ) {
            this.base = base;
            this.batchSize = batchSize;
        }

        @Override
        public boolean tryAdvance( Consumer<? super List<E>> action ) {
            List<E> batch = new ArrayList<>( batchSize );
            int i = 0;
            while( i < batchSize && base.tryAdvance( batch::add ) ) i++;

            if( batch.isEmpty() )
                return false;
            action.accept( batch );
            return true;
        }

        @Override
        public Spliterator<List<E>> trySplit() {
            if( base.estimateSize() <= batchSize )
                return null;
            Spliterator<E> splitBase = this.base.trySplit();
            return splitBase == null ? null
                : new BatchSpliterator<>( splitBase, batchSize );
        }

        @Override
        public long estimateSize() {
            double baseSize = base.estimateSize();
            return baseSize == 0
                ? 0
                : ( long ) Math.ceil( baseSize / ( double ) batchSize );
        }

        @Override
        public int characteristics() {
            return base.characteristics();
        }
    }
}
