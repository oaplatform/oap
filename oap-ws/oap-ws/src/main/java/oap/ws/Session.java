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
package oap.ws;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ToString
@EqualsAndHashCode( of = "id" )
public class Session {
    public final String id;
    private final Map<String, Object> values = new ConcurrentHashMap<>();

    public Session( String id ) {
        this.id = id;
    }

    @SuppressWarnings( "unchecked" )
    public <A> Optional<A> get( String key ) {
        return Optional.ofNullable( ( A ) values.get( key ) );
    }

    public void set( String key, Object value ) {
        values.put( key, value );
    }

    public void remove( String key ) {
        values.remove( key );
    }

    public void invalidate() {
        values.clear();
    }

    public void setAll( Map<String, Object> values ) {
        this.values.putAll( values );
    }

    public boolean containsKey( String key ) {
        return values.containsKey( key );
    }
}
