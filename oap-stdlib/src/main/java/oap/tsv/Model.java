/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

import oap.util.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static oap.util.Lists.Collectors.toArrayList;

public class Model {
    public boolean withHeader = false;
    private List<Field> fields = new ArrayList<>();
    private Predicate<List<String>> filter;

    public static Model withHeader() {
        Model model = new Model();
        model.withHeader = true;
        return model;
    }

    public Model filtered( Predicate<List<String>> filter ) {
        this.filter = this.filter == null ? filter : this.filter.and( filter );
        return this;
    }

    public Model columns( int count ) {
        return filtered( l -> l.size() == count );
    }

    public static Model withoutHeader() {
        return new Model();
    }

    public Model s( int[] indices ) {
        return field( s -> s, indices );
    }

    public Model s( int index, int... more ) {
        return field( s -> s, index, more );
    }

    public Model i( int index, int... more ) {
        return field( Integer::parseInt, index, more );
    }

    public Model l( int index, int... more ) {
        return field( Long::parseLong, index, more );
    }

    public Model d( int index, int... more ) {
        return field( Double::parseDouble, index, more );
    }

    public Model field( Function<String, Object> mapper, int[] indices ) {
        for( int i : indices ) this.fields.add( new Field( i, mapper ) );
        return this;
    }

    public Model field( Function<String, Object> mapper, int index, int... more ) {
        this.fields.add( new Field( index, mapper ) );
        return field( mapper, more );
    }

    public List<Object> convert( List<String> line ) {
        return Stream.of( fields )
            .map( f -> {
                try {
                    return f.mapper.apply( line.get( f.index ) );
                } catch( Exception e ) {
                    throw new TsvException( "at column " + f.index + " " + e, e );
                }
            } )
            .collect( toArrayList() );
    }

    public int size() {
        return fields.size();
    }

    public Model join( Model model ) {
        this.fields.addAll( model.fields );
        return this;
    }

    public Predicate<? super List<String>> filter() {
        return this.filter == null ? l -> true : this.filter;
    }

    private class Field {
        int index;
        Function<String, Object> mapper;

        public Field( int index, Function<String, Object> mapper ) {
            this.index = index;
            this.mapper = mapper;
        }
    }
}
