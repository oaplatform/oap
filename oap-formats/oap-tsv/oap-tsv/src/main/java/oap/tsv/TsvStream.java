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

package oap.tsv;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Arrays;
import oap.util.IndexTranslatingList;
import oap.util.Lists;
import oap.util.Stream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.tsv.Printer.print;
import static oap.tsv.Tsv.DELIMITER_COMMA;
import static oap.tsv.Tsv.DELIMITER_TAB;

public class TsvStream {

    private final List<String> headers;
    private final Stream<List<String>> data;

    protected TsvStream( Stream<List<String>> data ) {
        this( List.of(), data );
    }


    protected TsvStream( List<String> headers, Stream<List<String>> data ) {
        this.headers = headers;
        this.data = data;
    }

    public static TsvStream of( Stream<List<String>> data ) {
        return new TsvStream( data );
    }

    public static TsvStream of( List<String> headers, Stream<List<String>> data ) {
        return new TsvStream( headers, data );
    }

    public TsvStream withHeaders() {
        if( !headers.isEmpty() ) return this;
        Iterator<List<String>> iterator = data.iterator();
        if( iterator.hasNext() ) return new TsvStream( iterator.next(), Stream.of( iterator ) );
        return of( Stream.of( iterator ) );
    }

    public List<String> headers() {
        return headers;
    }

    public TsvStream select( int... columns ) {
        return new TsvStream(
            this.headers.isEmpty() ? this.headers : new IndexTranslatingList<>( this.headers, columns ),
            data.map( line -> new IndexTranslatingList<>( line, columns ) ) );
    }

    public TsvStream select( String... headers ) {
        TsvStream tsv = withHeaders();
        return tsv.select( Lists.indices( tsv.headers, headers ) );
    }

    public TsvStream select( List<String> headers ) {
        return select( Arrays.of( String.class, headers ) );
    }

    public TsvStream select( Header header ) {
        return select( header.cols );
    }

    public TsvStream stripHeaders() {
        return headers.isEmpty() ? new TsvStream( List.of(), withHeaders().data )
            : new TsvStream( List.of(), this.data );
    }

    public TsvStream filter( Predicate<List<String>> filter ) {
        return new TsvStream( this.headers, data.filter( filter ) );
    }

    public <E> Stream<E> mapToObj( Function<List<String>, ? extends E> mapper ) {
        return data.map( mapper );
    }

    public Stream<List<String>> toStream() {
        return headers.isEmpty() ? data : Stream.of( List.of( headers ) ).concat( data );
    }

    public List<List<String>> toList() {
        return collect( java.util.stream.Collectors.toList() );
    }

    public String toTsvString() {
        return collect( Collectors.toTsvString() );
    }

    public String toCsvString() {
        return collect( Collectors.toCsvString() );
    }

    public String toCsvString( boolean quoted ) {
        return collect( Collectors.toCsvString( quoted ) );
    }

    public Tsv toTsv() {
        return new Tsv( headers, data.toList() );
    }

    public <R, A> R collect( Collector<List<String>, A, R> collector ) {
        var container = collector.supplier().get();
        if( !headers.isEmpty() ) collector.accumulator().accept( container, headers );
        return data.collect( Collector.of( () -> container,
            collector.accumulator(),
            collector.combiner(),
            collector.finisher(),
            collector.characteristics().toArray( new Collector.Characteristics[0] ) ) );
    }


    public static class Collectors {
        public static Collector<List<String>, ?, OutputStream> toTsvOutputStream( OutputStream os ) {
            return Collector.of(
                () -> os,
                ( out, line ) -> {
                    try {
                        out.write( print( line, DELIMITER_TAB ).getBytes( UTF_8 ) );
                    } catch( IOException e ) {
                        throw new UncheckedIOException( e );
                    }
                },
                ( out, outIgnored ) -> out );
        }

        public static Collector<List<String>, ?, String> toCsvString() {
            return toCsvString( true );
        }

        public static Collector<List<String>, ?, String> toCsvString( boolean quoted ) {
            return toXsv( line -> print( line, DELIMITER_COMMA, quoted ) );
        }

        public static Collector<List<String>, ?, String> toTsvString() {
            return toXsv( line -> print( line, DELIMITER_TAB ) );
        }

        private static Collector<List<String>, ?, String> toXsv( Function<List<String>, String> joiner ) {
            return Collector.of(
                StringBuilder::new,
                ( sb, line ) -> sb.append( joiner.apply( line ) ),
                StringBuilder::append,
                StringBuilder::toString );
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class Header {
        public final List<String> cols;

        public Header( String... cols ) {
            this.cols = List.of( cols );
        }

        public int size() {
            return cols.size();
        }
    }
}
