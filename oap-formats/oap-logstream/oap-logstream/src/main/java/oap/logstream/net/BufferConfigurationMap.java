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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Pair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * configuration name -&gt; configuration; the first configuration ( in the order of insertion ) whose pattern matches
 * the logType wins
 */
@ToString( callSuper = true )
@EqualsAndHashCode( callSuper = true )
public final class BufferConfigurationMap extends LinkedHashMap<String, BufferConfigurationMap.BufferConfiguration> {
    private static final Pattern ALL = Pattern.compile( ".*" );

    private BufferConfigurationMap() {
    }

    private BufferConfigurationMap( String name, BufferConfiguration bufferConfiguration ) {
        put( name, bufferConfiguration );
    }

    public BufferConfigurationMap( Map<String, BufferConfiguration> m ) {
        super( m );
    }

    public static BufferConfigurationMap defaultMap( int bufferSize ) {
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
    public static final class BufferConfiguration {
        public static final int DEFAULT_PRIORITY = 0;

        public final int bufferSize;
        public final Pattern pattern;
        /**
         * message priority of the buffers of this configuration: higher - sent earlier ( oap-message MessageSender )
         */
        public final int priority;

        public BufferConfiguration( int bufferSize, Pattern pattern ) {
            this( bufferSize, pattern, DEFAULT_PRIORITY );
        }

        @JsonCreator
        public BufferConfiguration( @JsonProperty( "bufferSize" ) int bufferSize,
                                    @JsonProperty( "pattern" ) Pattern pattern,
                                    @JsonProperty( "priority" ) int priority ) {
            this.bufferSize = bufferSize;
            this.pattern = pattern;
            this.priority = priority;
        }

        public BufferConfiguration withBufferSize( int bufferSize ) {
            return new BufferConfiguration( bufferSize, pattern, priority );
        }

        public BufferConfiguration withPriority( int priority ) {
            return new BufferConfiguration( bufferSize, pattern, priority );
        }
    }
}
