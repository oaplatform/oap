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

package oap.http;

import lombok.SneakyThrows;
import lombok.val;
import oap.util.Maps;
import oap.util.Pair;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

public class Uri {
    @SneakyThrows
    public static URI uri( String uri, Map<String, Object> params ) {
        URIBuilder uriBuilder = new URIBuilder( uri );
        for( Map.Entry<String, Object> entry : params.entrySet() ) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if( value instanceof Collection<?> )
                for( val v : ( Collection<?> ) value )
                    uriBuilder.addParameter( name, v == null ? "" : v.toString() );
            else uriBuilder.addParameter( name, value == null ? "" : value.toString() );
        }
        return uriBuilder.build();

    }

    @SafeVarargs
    public static URI uri( String uri, Pair<String, Object>... params ) {
        return uri( uri, Maps.of( params ) );
    }
}
