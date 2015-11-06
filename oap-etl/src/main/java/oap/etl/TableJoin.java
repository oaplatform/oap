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

import oap.io.IoStreams;
import oap.tsv.ModelSet;
import oap.tsv.Tsv;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static oap.io.Files.version;

public class TableJoin implements Join {
    private HashMap<String, List<Object>> map = new HashMap<>();
    private List<Object> defaults;

    public TableJoin( List<Object> defaults ) {
        this.defaults = defaults;
    }

    public static Optional<TableJoin> fromResource( Class<?> contextClass, String name,
        ModelSet modelSet, List<Object> defaults ) {
        return Tsv.fromResource( contextClass, name, modelSet)
            .map( s -> s.foldLeft( new TableJoin( defaults ), ( l, line ) -> {
                l.map.put( (String) line.remove( modelSet.size() - 1 ), line );
                return l;
            } ) );
    }

    public static TableJoin fromFiles( List<Path> files, IoStreams.Encoding encoding, ModelSet modelSet, List<Object> defaults ) {
        return Stream.of( files.stream() )
            .foldLeft( new TableJoin( defaults ), ( t, path ) -> {
                Tsv.fromPath( path, encoding, modelSet )
                        .forEach( line -> t.map.put( (String) line.remove( modelSet.size() - 1 ), line ) );
                return t;
            } );
    }

    public List<Object> on( String key ) {
        return map.getOrDefault( key, defaults );
    }

    public TableJoin withFlag( Object flag, Object defaultFlag ) {
        map.forEach( ( key, value ) -> value.add( flag ) );
        defaults.add( defaultFlag );
        return this;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
