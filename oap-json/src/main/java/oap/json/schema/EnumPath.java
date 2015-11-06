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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnumPath {
    private final String path;

    public EnumPath( String path ) {
        this.path = path;
    }

    @SuppressWarnings( "unchecked" )
    public List<Object> traverse( Object json ) {
        String[] paths = path.split( "\\." );

        Optional<Object> result = traverse( json, paths );

        return result.map( r -> {
            if( r instanceof List<?> ) return (List<Object>) r;

            return Collections.singletonList( r );
        } ).orElseGet( Collections::emptyList );

    }

    private Optional<Object> traverse( Object json, String[] paths ) {
        Object last = json;

        for( int i = 0; i < paths.length; i++ ) {
            String field = paths[i];

            if( last == null ) return Optional.empty();
            else if( last instanceof Map<?, ?> ) {
                Map<?, ?> map = (Map<?, ?>) last;

                last = map.get( field );
            } else if( last instanceof List<?> ) {
                List<?> list = (List<?>) last;

                ArrayList<Object> result = new ArrayList<>();

                for( Object item : list ) {
                    Optional<Object> value = traverse( item, Arrays.copyOfRange( paths, i, paths.length ) );
                    value.ifPresent( result::add );
                }

                last = result;

                i = paths.length;
            } else return Optional.empty();
        }

        return Optional.of( last );
    }
}
