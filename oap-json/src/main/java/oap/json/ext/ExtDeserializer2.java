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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by igor.petrenko on 07.08.2019.
 */
@Slf4j
public class ExtDeserializer2 extends StdDeserializer<Ext> {
    private static final HashMap<ClassFieldKey, Class<?>> extmap = new HashMap<>();
    private static final Pattern LINE = Pattern.compile( "\\s*([^#]+)#([^\\s=]+)\\s*=\\s*([^\\s]+)" );

    static {
        for( var p : Resources.readLines( "META-INF/json-ext2.properties" ) )
            try {
                if( p.startsWith( "#" ) || StringUtils.isBlank( p ) ) continue;
                log.trace( "mapping ext {}", p );

                var matcher = LINE.matcher( p );
                Preconditions.checkArgument( matcher.find(), "invalid rule: " + p );

                var key = new ClassFieldKey( Class.forName( matcher.group( 1 ) ), matcher.group( 2 ) );
                var impl = Class.forName( matcher.group( 3 ).trim() );
                extmap.put( key, impl );

                log.debug( "add ext rule: {} = {}", key, impl );
            } catch( ClassNotFoundException e ) {
                throw new ExceptionInInitializerError( e );
            }
        log.trace( "mapped extensions: {}", extmap );
    }

    protected ExtDeserializer2() {
        super( Ext.class );
    }

    @Override
    public Ext deserialize( JsonParser jsonParser, DeserializationContext ctxt ) throws IOException {
        var parsingContext = jsonParser.getParsingContext();
        var contextParent = parsingContext.getParent();
        var fieldName = contextParent.getCurrentName();
        var klass = contextParent.getCurrentValue().getClass();
        var key = new ClassFieldKey( klass, fieldName );

        var impl = extmap.get( key );
        if( impl == null ) {
            log.trace( "extension class lookup failed for {}", key );
            return null;
        }

        return ( Ext ) jsonParser.readValueAs( impl );
    }

    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    private static class ClassFieldKey {
        public final Class<?> klass;
        public final String field;
    }
}
