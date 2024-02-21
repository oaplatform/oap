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

package oap.json.schema;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class JsonPath {
    private final String[] path;
    private final Optional<String[]> fromPath;

    public JsonPath( String path, Optional<String> fromPath ) {
        this.path = StringUtils.split( path, '.' );
        this.fromPath = fromPath.map( fp -> StringUtils.split( fp, '/' ) );
    }

    public JsonPath( String path ) {
        this( path, Optional.empty() );
    }

    @SuppressWarnings( "unchecked" )
    public List<Object> traverse( Object json ) {
        final Optional<Object> result = traverse( json, 0, 0 );

        return result.map( r -> {
            if( r instanceof List<?> ) return ( List<Object> ) r;

            return singletonList( r );
        } ).orElseGet( Collections::emptyList );

    }

    @SuppressWarnings( "checkstyle:ModifiedControlVariable" )
    private Optional<Object> traverse( Object json, int index, int fromIndex ) {
        Object last = json;

        int fi = fromIndex;
        for( int i = index; i < path.length; i++, fi++ ) {
            final String field = path[i];
            Optional<String> fromField = Optional.empty();
            if( fromPath.isPresent() ) {
                final String[] fp = fromPath.get();
                if( fp.length <= fi )
                    throw new IllegalArgumentException( "[" + fi + "] path = " + asList( path ) + " != fromPath = " + fromPath.map( Arrays::asList ) );
                fromField = Optional.of( fp[fi] );
            }
            if( last == null ) return Optional.empty();
            else if( last instanceof Map<?, ?> ) {
                final Map<?, ?> map = ( Map<?, ?> ) last;

                last = map.get( field );
            } else if( last instanceof List<?> ) {
                final List<?> list = ( List<?> ) last;

                if( fromField.isPresent() ) {
                    final String arrayIndexStr = fromPath.get()[fi];
                    if( !NumberUtils.isDigits( arrayIndexStr ) ) return Optional.empty();
                    int arrayIndex = Integer.parseInt( arrayIndexStr );

                    final Optional<Object> value = traverse( list.get( arrayIndex ), i + 1, fi + 1 );
                    last = value.orElse( null );
                } else {
                    final ArrayList<Object> result = new ArrayList<>();

                    for( Object item : list ) {
                        final Optional<Object> value = traverse( item, i, fi );
                        value.ifPresent( result::add );
                    }
                    last = result;
                }

//                wtf?
                i = path.length;
            } else return Optional.empty();
        }

        return Optional.ofNullable( last );
    }

    public String getFixedPath() {
        if( fromPath.isEmpty() ) throw new IllegalArgumentException( "fromPath is required" );

        return getFixedPath( 0, 0 );
    }

    private String getFixedPath( int index, int fromIndex ) {
        final ArrayList<String> res = new ArrayList<>();
        int fi = fromIndex;
        for( int i = index; i < path.length; i++, fi++ ) {
            final String field = path[i];

            final String[] fp = fromPath.orElseThrow();
            if( fp.length <= fi )
                throw new IllegalArgumentException( "[" + fi + "] path = " + asList( path ) + " != fromPath = " + fromPath.map( Arrays::asList ) );
            String fromField = fp[fi];

            if( "items".equals( field ) && NumberUtils.isDigits( fromField ) ) {
                res.add( fromField );
            } else {
                res.add( field );
            }
        }

        return String.join( ".", res );
    }
}
