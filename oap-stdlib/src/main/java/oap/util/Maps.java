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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static oap.util.Maps.Collectors.toListMultimap;
import static oap.util.Maps.Collectors.toSetMultimap;

public class Maps {

    @Deprecated
    public static <K, V> Map<K, V> empty() {
        return of();
    }

    @SafeVarargs
    public static <K, V> Map<K, V> addAll( Map<K, V> map, Pair<K, V>... pairs ) {
        for( Pair<K, V> pair : pairs ) map.put( pair._1, pair._2 );
        return map;
    }

    @SafeVarargs
    public static <K, V> LinkedHashMap<K, V> of( Pair<K, V>... pairs ) {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        addAll( map, pairs );
        return map;
    }

    public static <K, V> Map<K, V> of( Iterable<Pair<K, V>> pairs ) {
        Map<K, V> map = new LinkedHashMap<>();
        for( Pair<K, V> pair : pairs ) map.put( pair._1, pair._2 );
        return map;
    }

    /**
     * @see Map#of(Object, Object)
     */
    @Deprecated
    public static <K, V> LinkedHashMap<K, V> of2( K key1, V value1 ) {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put( key1, value1 );
        return map;
    }

    /**
     * @see Map#of(Object, Object, Object, Object)
     */
    @Deprecated
    public static <K, V> LinkedHashMap<K, V> of2( K key1, V value1, K key2, V value2 ) {
        LinkedHashMap<K, V> map = of2( key1, value1 );
        map.put( key2, value2 );
        return map;
    }

    /**
     * @see Map#of(Object, Object, Object, Object, Object, Object)
     */
    @Deprecated
    public static <K, V> LinkedHashMap<K, V> of2( K key1, V value1, K key2, V value2, K key3, V value3 ) {
        LinkedHashMap<K, V> map = of2( key1, value1, key2, value2 );
        map.put( key3, value3 );
        return map;
    }

    /**
     * @see Map#of(Object, Object, Object, Object, Object, Object, Object, Object)
     */
    @Deprecated
    public static <K, V> LinkedHashMap<K, V> of2( K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4 ) {
        LinkedHashMap<K, V> map = of2( key1, value1, key2, value2, key3, value3 );
        map.put( key4, value4 );
        return map;
    }

    /**
     * @see Map#of(Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object, Object)
     */
    @Deprecated
    public static <K, V> LinkedHashMap<K, V> of2( K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5 ) {
        LinkedHashMap<K, V> map = of2( key1, value1, key2, value2, key3, value3, key4, value4 );
        map.put( key5, value5 );
        return map;
    }

    @SafeVarargs
    public static <V> Map<String, V> ofStrings( Pair<String, V>... pairs ) {
        HashMap<String, V> map = new HashMap<>();
        addAll( map, pairs );
        return map;
    }

    public static <K, V> Optional<V> get( Map<? super K, V> map, K key ) {
        return Optional.ofNullable( map.get( key ) );
    }

    public static <E extends Throwable, K, V> V getOrThrow( Map<? super K, V> map, K key, Supplier<E> ex ) throws E {
        V v = map.get( key );
        if( v == null ) throw ex.get();
        return v;
    }

    @SafeVarargs
    public static <K, V> SetMultimap<K, V> setmmap( Pair<K, V>... pairs ) {
        return Stream.of( pairs ).collect( toSetMultimap() );
    }

    @SafeVarargs
    public static <K, V> ListMultimap<K, V> listmmap( Pair<K, V>... pairs ) {
        return Stream.of( pairs ).collect( toListMultimap() );
    }

    public static <K, V, R> List<R> toList( Map<K, V> map, BiFunction<K, V, R> mapper ) {
        return toList( map, mapper, new ArrayList<>( map.size() ) );
    }

    public static <K, V, R> List<R> toList( Map<K, V> map, BiFunction<K, V, R> mapper, List<R> list ) {
        map.forEach( ( k, v ) -> list.add( mapper.apply( k, v ) ) );
        return list;
    }

    public static <K, V, M1 extends Multimap<K, V>,
        M2 extends Multimap<? extends K, ? extends V>> M1 add( M1 base, M2 map ) {
        base.putAll( map );
        return base;
    }

    /**
     * This is highly controversial implementation, type-wise
     */
    @SuppressWarnings( "unchecked" )
    public static <K, V> Map<K, V> deepMerge( Map original, Map newMap ) {
        for( Object key : newMap.keySet() ) {
            if( newMap.get( key ) instanceof Map && original.get( key ) instanceof Map ) {
                Map originalChild = ( Map ) original.get( key );
                Map newChild = ( Map ) newMap.get( key );
                original.put( key, deepMerge( originalChild, newChild ) );
            } else {
                original.put( key, newMap.get( key ) );
            }
        }
        return ( Map<K, V> ) original;
    }

    public static <K, V> Optional<K> byValue( Map<K, ? extends V> map, V value ) {
        return Stream.of( map.entrySet() )
            .filter( e -> value.equals( e.getValue() ) )
            .map( Entry::getKey )
            .findFirst();
    }

    public static <K, V> Entry<K, V> head( Map<K, V> map ) {
        return map.entrySet().iterator().next();
    }

    public static Map<String, ?> flatten( Map<String, ?> map ) {
        return map.entrySet()
            .stream()
            .flatMap( Maps::flatten )
            .collect( LinkedHashMap::new, ( m, e ) -> m.put( "/" + e.getKey(), e.getValue() ), LinkedHashMap::putAll );
    }

    private static Stream<Entry<String, ?>> flatten( Entry<String, ?> entry ) {

        if( entry == null ) return Stream.empty();

        if( entry.getValue() instanceof Map<?, ?> properties ) return Stream.of( properties.entrySet() )
            .flatMap( e -> flatten( new SimpleEntry<>( entry.getKey() + "/" + e.getKey(), e.getValue() ) ) );

        if( entry.getValue() instanceof List<?> list ) return Stream.of( IntStream.range( 0, list.size() )
            .mapToObj( i -> new SimpleEntry<>( entry.getKey() + "/" + i, list.get( i ) ) )
            .flatMap( Maps::flatten ) );

        return Stream.of( entry );
    }

    public static class Collectors {
        public static <K, V> Collector<? super Pair<K, V>, ?, SetMultimap<K, V>> toSetMultimap() {
            return Collector.of(
                HashMultimap::create,
                ( mm, pair ) -> mm.put( pair._1, pair._2 ),
                ( left, right ) -> {
                    left.putAll( right );
                    return left;
                },
                Collector.Characteristics.UNORDERED );
        }

        public static <K, V> Collector<? super Pair<K, V>, ?, ListMultimap<K, V>> toListMultimap() {
            return Collector.of(
                ArrayListMultimap::create,
                ( mm, pair ) -> mm.put( pair._1, pair._2 ),
                ( left, right ) -> {
                    left.putAll( right );
                    return left;
                },
                Collector.Characteristics.UNORDERED );
        }

        public static <K, V> Collector<? super Pair<K, V>, Map<K, V>, Map<K, V>> toMap( Supplier<Map<K, V>> supplier ) {
            return Collector.of(
                supplier,
                ( map, pair ) -> map.put( pair._1, pair._2 ),
                ( left, right ) -> {
                    left.putAll( right );
                    return left;
                },
                Collector.Characteristics.UNORDERED );

        }

        public static <K, V> Collector<? super Pair<K, V>, Map<K, V>, Map<K, V>> toMap() {
            return toMap( LinkedHashMap::new );
        }

        public static <K, V> Collector<? super Pair<K, V>, Map<K, V>, Map<K, V>> toTreeMap() {
            return toMap( TreeMap::new );
        }

        public static <K, V> Collector<? super Pair<K, V>, ?, ConcurrentMap<K, V>> toConcurrentMap() {
            return toConcurrentMap( ConcurrentHashMap::new );
        }

        public static <K, V> Collector<? super Pair<K, V>, ?, ConcurrentMap<K, V>> toConcurrentMap( ConcurrentMap<K, V> map ) {
            return toConcurrentMap( () -> map );
        }

        private static <K, V> Collector<? super Pair<K, V>, ?, ConcurrentMap<K, V>> toConcurrentMap( Supplier<ConcurrentMap<K, V>> supplier ) {
            return Collector.of(
                supplier,
                ( map, pair ) -> map.put( pair._1, pair._2 ),
                ( left, right ) -> {
                    left.putAll( right );
                    return left;
                },
                Collector.Characteristics.UNORDERED );
        }
    }
}
