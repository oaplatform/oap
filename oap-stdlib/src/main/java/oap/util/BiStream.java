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

public class BiStream<A, B> extends Stream<Pair<A, B>> {
    protected BiStream( java.util.stream.Stream<Pair<A, B>> underlying ) {
        super( underlying );
    }

    public static <A, B> BiStream<A, B> of( Stream<Pair<A, B>> stream ) {
        return new BiStream<>( stream );
    }

    public static <A, B> BiStream<A, B> of( Map<A, B> map ) {
        return new BiStream<>( map.entrySet().stream().map( e -> __( e.getKey(), e.getValue() ) ) );
    }

    public BiStream<A, B> reversed() {
        return new BiStream<>( Stream.of(
            this.collect( java.util.stream.Collectors.toCollection( LinkedList::new ) )
                .descendingIterator()
        ) );
    }

    public void forEach( BiConsumer<A, B> consumer ) {
        super.forEach( p -> consumer.accept( p._1, p._2 ) );
    }


    public <R> Stream<R> mapToObj( BiFunction<A, B, ? extends R> mapper ) {
        return super.map( p -> mapper.apply( p._1, p._2 ) );
    }

    public <R> Stream<R> flatMapToObj( BiFunction<A, B, Stream<? extends R>> mapper ) {
        return super.flatMap( p -> mapper.apply( p._1, p._2 ) );
    }

    public <A2, B2> BiStream<A2, B2> map( BiFunction<? super A, ? super B, Pair<A2, B2>> mapper ) {
        return new BiStream<>( super.map( p -> mapper.apply( p._1, p._2 ) ) );
    }


    @Override
    public BiStream<A, B> filter( Predicate<? super Pair<A, B>> predicate ) {
        return new BiStream<>( super.filter( predicate ) );
    }

    public BiStream<A, B> filter( BiPredicate<? super A, ? super B> predicate ) {
        return new BiStream<>( super.filter( p -> predicate.test( p._1, p._2 ) ) );
    }

    public <A2, B2> BiStream<A2, B2> flatMap( BiFunction<A, B, ? extends BiStream<A2, B2>> mapper ) {
        return new BiStream<>( flatMap( p -> mapper.apply( p._1, p._2 ) ) );
    }


    public Pair<BiStream<A, B>, BiStream<A, B>> partition( BiPredicate<A, B> predicate ) {
        Pair<Stream<Pair<A, B>>, Stream<Pair<A, B>>> partition = super.partition( p -> predicate.test( p._1, p._2 ) );
        return new Pair<>( of( partition._1 ), of( partition._2 ) );
    }

    public Map<A, B> toMap() {
        return collect( Maps.Collectors.toMap() );
    }
}
