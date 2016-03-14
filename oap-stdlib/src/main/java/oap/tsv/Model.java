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

import lombok.ToString;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Model {
    public final boolean withHeader;
    private Predicate<List<String>> filter;
    private List<Function<List<String>, Object>> columns = new ArrayList<>();

    public Model( boolean withHeader ) {
        this.withHeader = withHeader;
    }

    public Model column( Function<String, Object> mapper, int index, int... more ) {
        this.columns.add( new Column( index, mapper ) );
        return column( mapper, more );
    }

    public Model column( Object value ) {
        this.columns.add( new Value( value ) );
        return this;
    }

    public Model column( Function<String, Object> mapper, int[] indices ) {
        for( int i : indices ) this.columns.add( new Column( i, mapper ) );
        return this;
    }

    public List<Object> convert( List<String> line ) {
        return Stream.of( columns )
            .map( f -> {
                try {
                    return f.apply( line );
                } catch( IndexOutOfBoundsException e ) {
                    String lineToPrint = "[" + Stream.of( line ).collect( Collectors.joining( "|" ) ) + "]";
                    throw new TsvException(
                        "line does not contain a column with index " + f + ": " + lineToPrint, e );
                } catch( Exception e ) {
                    throw new TsvException( "at column " + f + " " + e, e );
                }
            } )
            .toList();
    }

    public Model s( int[] indices ) {
        return column( s -> s, indices );
    }

    public Model s( int index, int... more ) {
        return column( s -> s, index, more );
    }

    public Model i( int index, int... more ) {
        return column( Integer::parseInt, index, more );
    }

    public Model l( int index, int... more ) {
        return column( Long::parseLong, index, more );
    }

    public Model d( int index, int... more ) {
        return column( Double::parseDouble, index, more );
    }

    public Model v( Object value ) {
        return column( value );
    }

    public Model filtered( Predicate<List<String>> filter ) {
        this.filter = this.filter == null ? filter : this.filter.and( filter );
        return this;
    }

    public Model filterColumnCount( int count ) {
        return filtered( l -> l.size() == count );
    }

    public int size() {
        return columns.size();
    }

    public Model join( Model model ) {
        this.columns.addAll( model.columns );
        return this;
    }

    public Predicate<? super List<String>> filter() {
        return this.filter == null ? l -> true : this.filter;
    }

    public static Model withoutHeader() {
        return new Model( false );
    }
    public static Model withHeader() {
        return new Model( true );
    }

    public static Complex complex( Function<Path, Model> modelBuilder ) {
        return new Complex( modelBuilder );
    }

    public static class Complex {
        private Function<Path, Model> modelBuilder;

        private Complex( Function<Path, Model> modelBuilder ) {
            this.modelBuilder = modelBuilder;
        }

        public Model modelFor( Path path ) {
            return modelBuilder.apply( path );
        }

    }

    @ToString( exclude = "mapper" )
    private static class Column implements Function<List<String>, Object> {
        Function<String, Object> mapper;
        int index;

        public Column( int index, Function<String, Object> mapper ) {
            this.index = index;
            this.mapper = mapper;
        }

        @Override
        public Object apply( List<String> line ) {
            return mapper.apply( line.get( index ) );
        }

    }

    @ToString
    private static class Value implements Function<List<String>, Object> {
        Object value;

        public Value( Object value ) {
            this.value = value;
        }

        @Override
        public Object apply( List<String> line ) {
            return value;
        }
    }
}
