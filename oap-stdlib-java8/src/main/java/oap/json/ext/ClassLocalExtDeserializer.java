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

package oap.json.ext;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.Resources;
import oap.util.Pair;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ClassLocalExtDeserializer<T> extends StdDeserializer<T> {
    private static Map<Class<?>, Class<?>> extmap = new HashMap<>();

    static {
        for( val p : Resources.readLines( "META-INF/json-ext.properties" ) )
            try {
                if( p.startsWith( "#" ) || StringUtils.isBlank( p ) ) continue;
                log.trace( "mapping ext {}", p );
                Pair<String, String> split = Strings.split( p, "=" );
                extmap.put( Class.forName( split._1.trim() ), Class.forName( split._2.trim() ) );
            } catch( ClassNotFoundException e ) {
                throw new ExceptionInInitializerError( e );
            }
        log.trace( "mapped extensions: {}", extmap );
    }

    private Class<T> clazz;

    public ClassLocalExtDeserializer( Class<T> clazz ) {
        super( clazz );
        this.clazz = clazz;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public T deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) throws IOException {
        return ( T ) jsonParser.readValueAs( Objects.requireNonNull( extmap.get( clazz ), "extension class lookup failed for " + clazz ) );
    }
}
