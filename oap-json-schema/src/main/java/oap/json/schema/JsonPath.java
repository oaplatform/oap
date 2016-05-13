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

import java.util.*;

public class JsonPath {
    private final String[] paths;

    public JsonPath( String path ) {
        paths = StringUtils.split( path, '.' );
    }

    @SuppressWarnings( "unchecked" )
    public List<Object> traverse( Object json ) {
        final Optional<Object> result = traverse( json, 0 );

        return result.map( r -> {
            if( r instanceof List<?> ) return ( List<Object> ) r;

            return Collections.singletonList( r );
        } ).orElseGet( Collections::emptyList );

    }

    private Optional<Object> traverse( Object json, int index ) {
        Object last = json;

        for( int i = index; i < paths.length; i++ ) {
            final String field = paths[i];

            if( last == null ) return Optional.empty();
            else if( last instanceof Map<?, ?> ) {
                final Map<?, ?> map = ( Map<?, ?> ) last;

                last = map.get( field );
            } else if( last instanceof List<?> ) {
                final List<?> list = ( List<?> ) last;

                final ArrayList<Object> result = new ArrayList<>();

                for( Object item : list ) {
                    final Optional<Object> value = traverse( item, i );
                    value.ifPresent( result::add );
                }

                last = result;

                i = paths.length;
            } else return Optional.empty();
        }

        return Optional.ofNullable( last );
    }
}
