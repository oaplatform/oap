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
package oap.etl;

import oap.tsv.Model;
import oap.tsv.Tsv;
import oap.util.Stream;
import org.apache.commons.lang3.mutable.MutableLong;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class CountingKeyJoin implements Join {
    private HashMap<String, MutableLong> map = new HashMap<>();

    public static Optional<CountingKeyJoin> fromResource( Class<?> contextClass, String name, Model model ) {
        return Tsv.fromResource( contextClass, name, model )
            .map( CountingKeyJoin::fromTsv );
    }

    public static CountingKeyJoin fromPaths( List<Path> paths, Model.Complex complexModel ) {
        return fromTsv( Tsv.fromPaths( paths, complexModel ) );
    }

    public static CountingKeyJoin fromPaths( List<Path> paths, Model model ) {
        return fromTsv( Tsv.fromPaths( paths, model ) );
    }

    private static CountingKeyJoin fromTsv( Stream<List<Object>> tsv ) {
        return tsv.foldLeft( new CountingKeyJoin(), ( j, list ) -> {
            j.map.computeIfAbsent( ( String ) list.get( 0 ), k -> new MutableLong() ).increment();
            return j;
        } );
    }

    @Override
    public List<Object> on( String key ) {
        return map.containsKey( key ) ? singletonList( map.get( key ) ) : singletonList( 0L );
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
