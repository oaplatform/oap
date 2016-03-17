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

import com.google.common.collect.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static oap.util.Maps.Collectors.toListMultimap;
import static oap.util.Maps.Collectors.toSetMultimap;

public class Maps {

    @SafeVarargs
    public static <K, V> Map<K, V> addAll( Map<K, V> map, Pair<K, V>... pairs ) {
        for( Pair<K, V> pair : pairs ) map.put( pair._1, pair._2 );
        return map;
    }

    @SafeVarargs
    public static <K, V> LinkedHashMap<K, V> of( Pair<K, V>... pairs ) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        addAll( map, pairs );
        return map;
    }

    @SafeVarargs
    public static <V> Map<String, V> ofStrings( Pair<String, V>... pairs ) {
        final HashMap<String, V> map = new HashMap<>();
        addAll( map, pairs );
        return map;
    }

    public static <K, V> Map<K, V> of( Iterable<Pair<K, V>> pairs ) {
        Map<K, V> map = new LinkedHashMap<>();
        for( Pair<K, V> pair : pairs ) map.put( pair._1, pair._2 );
        return map;
    }

    @SafeVarargs
    public static <K, V> SetMultimap<K, V> setmmap( Pair<K, V>... pairs ) {
        return Stream.of( pairs ).collect( toSetMultimap() );
    }

    @SafeVarargs
    public static <K, V> ListMultimap<K, V> listmmap( Pair<K, V>... pairs ) {
        return Stream.of( pairs ).collect( toListMultimap() );
    }

    public static <K, V, M1 extends Multimap<K, V>,
        M2 extends Multimap<? extends K, ? extends V>> M1 add( M1 base, M2 map ) {
        base.putAll( map );
        return base;
    }

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
            .map( Map.Entry::getKey )
            .findFirst();
    }

    public static class Collectors {
        public static <K, V> Collector<? super Pair<K, V>, ?, SetMultimap<K, V>> toSetMultimap() {
            return Collector.<Pair<K, V>, SetMultimap<K, V>>of(
                HashMultimap::create,
                ( mm, pair ) -> mm.put( pair._1, pair._2 ),
                ( left, right ) -> {
                    left.putAll( right );
                    return left;
                },
                Collector.Characteristics.UNORDERED );
        }

        public static <K, V> Collector<? super Pair<K, V>, ?, ListMultimap<K, V>> toListMultimap() {
            return Collector.<Pair<K, V>, ListMultimap<K, V>>of(
                ArrayListMultimap::create,
                ( mm, pair ) -> mm.put( pair._1, pair._2 ),
                ( left, right ) -> {
                    left.putAll( right );
                    return left;
                },
                Collector.Characteristics.UNORDERED );
        }

        public static <K, V> Collector<? super Pair<K, V>, Map<K, V>, Map<K, V>> toMap() {
            return Collector.<Pair<K, V>, Map<K, V>>of(
                LinkedHashMap::new,
                ( map, pair ) -> map.put( pair._1, pair._2 ),
                ( left, right ) -> {
                    left.putAll( right );
                    return left;
                },
                Collector.Characteristics.UNORDERED );
        }

        public static <K, V> Collector<? super Pair<K, V>, ?, ConcurrentMap<K, V>> toConcurrentMap() {
            return toConcurrentMap( ConcurrentHashMap::new );
        }

        public static <K, V> Collector<? super Pair<K, V>, ?, ConcurrentMap<K, V>> toConcurrentMap(
            ConcurrentMap<K, V> map ) {
            return toConcurrentMap( () -> map );
        }

        private static <K, V> Collector<? super Pair<K, V>, ?, ConcurrentMap<K, V>> toConcurrentMap(
            Supplier<ConcurrentMap<K, V>> supplier ) {
            return Collector.<Pair<K, V>, ConcurrentMap<K, V>>of(
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
