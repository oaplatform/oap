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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Created by Admin on 10.05.2016.
 */
public class BiStream<K, V> extends Stream<Pair<K, V>> {
    BiStream( java.util.stream.Stream<Pair<K, V>> underlying ) {
        super( underlying );
    }

    static <K, V> BiStream<K, V> of2( java.util.stream.Stream<Pair<K, V>> stream ) {
        return new BiStream<>( stream );
    }

    public void forEach( BiConsumer<K, V> action ) {
        super.forEach( p -> action.accept( p._1, p._2 ) );
    }

    public <R> Stream<R> map( BiFunction<K, V, ? extends R> mapper ) {
        return super.map( p -> mapper.apply( p._1, p._2 ) );
    }

    public <RK, RV> BiStream<RK, RV> map2( BiFunction<K, V, Pair<RK, RV>> mapper ) {
        return of2( super.map( p -> mapper.apply( p._1, p._2 ) ) );
    }

    public BiStream<K, V> filter( BiPredicate<K, V> predicate ) {
        return of2( super.filter( p -> predicate.test( p._1, p._2 ) ) );
    }

    public <R> Stream<R> flatMap( BiFunction<K, V, ? extends java.util.stream.Stream<? extends R>> mapper ) {
        return super.flatMap( p -> mapper.apply( p._1, p._2 ) );
    }

    public <RK, RV> BiStream<RK, RV> flatMap2( BiFunction<K, V, BiStream<RK, RV>> mapper ) {
        return of2( super.flatMap( p -> mapper.apply( p._1, p._2 ) ) );
    }

    public Pair<BiStream<K, V>, BiStream<K, V>> partition( BiPredicate<K, V> criteria ) {
        Pair<Stream<Pair<K, V>>, Stream<Pair<K, V>>> partition = super.partition( p -> criteria.test( p._1, p._2 ) );
        return new Pair<>( of2( partition._1 ), of2( partition._2 ) );
    }

    public Map<K, V> toMap() {
        return collect( Maps.Collectors.toMap() );
    }
}
