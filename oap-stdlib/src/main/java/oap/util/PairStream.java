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

import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static oap.util.Pair.__;

@Deprecated
/**
 * @see BiStream
 */
public class PairStream<A, B> extends Stream<Pair<A, B>> {
    protected PairStream( java.util.stream.Stream<Pair<A, B>> underlying ) {
        super( underlying );
    }


    public static <A, B> PairStream<A, B> of( Stream<Pair<A, B>> stream ) {
        return new PairStream<>( stream );
    }

    public static <A, B> PairStream<A, B> of( Map<A, B> map ) {
        return new PairStream<>( map.entrySet().stream().map( e -> __( e.getKey(), e.getValue() ) ) );
    }

    public static <A, B> PairStream<A, B> reversed( Map<A, B> map ) {
        return new PairStream<>( Stream.of( of( map )
            .collect( java.util.stream.Collectors.toCollection( LinkedList::new ) )
            .descendingIterator()
        ) );
    }

    public <R> Stream<R> mapToObj( BiFunction<A, B, R> mapper ) {
        return super.map( p -> mapper.apply( p._1, p._2 ) );
    }

    public <R> Stream<R> flatMapToObj( BiFunction<A, B, Stream<? extends R>> mapper ) {
        return super.flatMap( p -> mapper.apply( p._1, p._2 ) );
    }

    public <A2, B2> PairStream<A2, B2> flatMap( BiFunction<A, B, ? extends PairStream<A2, B2>> mapper ) {
        return of( flatMap( p -> mapper.apply( p._1, p._2 ) ) );
    }

    public <A2, B2> PairStream<A2, B2> map( BiFunction<? super A, ? super B, Pair<A2, B2>> mapper ) {
        return new PairStream<>( super.map( p -> mapper.apply( p._1, p._2 ) ) );
    }

    @Override
    public PairStream<A, B> filter( Predicate<? super Pair<A, B>> predicate ) {
        return new PairStream<>( super.filter( predicate ) );
    }

    public PairStream<A, B> filter( BiPredicate<? super A, ? super B> predicate ) {
        return new PairStream<>( super.filter( p -> predicate.test( p._1, p._2 ) ) );
    }

    public void forEach( BiConsumer<A, B> consumer ) {
        super.forEach( p -> consumer.accept( p._1, p._2 ) );
    }

    public Map<A, B> toMap() {
        return collect( Maps.Collectors.toMap() );
    }
}
