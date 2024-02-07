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

package oap.logstream.net;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@ToString( callSuper = true )
@EqualsAndHashCode( callSuper = true )
public final class BufferConfigurationMap extends HashMap<String, BufferConfigurationMap.BufferConfiguration> {
    private static final Pattern ALL = Pattern.compile( ".*" );

    private BufferConfigurationMap() {
    }

    private BufferConfigurationMap( String name, BufferConfiguration bufferConfiguration ) {
        put( name, bufferConfiguration );
    }

    public BufferConfigurationMap( Map<String, BufferConfiguration> m ) {
        super( m );
    }

    static BufferConfigurationMap defaultMap( int bufferSize ) {
        return new BufferConfigurationMap( "DEFAULT", new BufferConfiguration( bufferSize, ALL ) );
    }

    @SafeVarargs
    public static BufferConfigurationMap custom( Pair<String, BufferConfiguration>... bufferConfiguration ) {
        final BufferConfigurationMap bufferConfigurationMap = new BufferConfigurationMap();
        for( var p : bufferConfiguration ) {
            bufferConfigurationMap.put( p._1, p._2 );
        }
        return bufferConfigurationMap;
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    public static final class BufferConfiguration {
        public final int bufferSize;
        public final Pattern pattern;
    }
}
