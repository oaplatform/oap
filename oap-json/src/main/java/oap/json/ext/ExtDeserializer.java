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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;
import oap.util.Throwables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
public class ExtDeserializer extends StdDeserializer<Ext> {
    private static final HashMap<String, Class<?>> extmap = new HashMap<>();

    static {
        try {
            for( var p : Resources.readStrings( "META-INF/json-ext.yaml" ) ) {
                log.trace( "mapping ext {}", p );

                var mapper = new ObjectMapper( new YAMLFactory() );
                mapper.getDeserializationConfig().with( new JacksonAnnotationIntrospector() );
                mapper.registerModule( new ParameterNamesModule( JsonCreator.Mode.DEFAULT ) );
                var conf = mapper.readValue( p, Configuration.class );
                for( var cc : conf.ext ) {
                    String key = extensionKey( cc.clazz, cc.field );
                    extmap.put( key, cc.implementation );

                    log.debug( "add ext rule: {} = {}", key, cc.implementation );
                }
            }
            log.trace( "mapped extensions: {}", extmap );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
            throw Throwables.propagate( e );
        }
    }

    private static String extensionKey( Class<?> clazz, String field ) {
        return clazz.getName() + "#" + field;
    }

    protected ExtDeserializer() {
        super( Ext.class );
    }

    @Override
    public Ext deserialize( JsonParser jsonParser, DeserializationContext ctxt ) throws IOException {
        var parsingContext = jsonParser.getParsingContext();
        var contextParent = parsingContext.getParent();
        var field = contextParent.getCurrentName();
        var clazz = contextParent.getCurrentValue().getClass();
        var implementation = extensionOf( clazz, field );
        if( implementation == null ) {
            log.trace( "extension class lookup failed for {}#{}", clazz, field );
            return null;
        }

        return ( Ext ) jsonParser.readValueAs( implementation );
    }

    public static Class<?> extensionOf( Class<?> clazz, String field ) {
        return extmap.get( extensionKey( clazz, field ) );
    }

    @ToString
    public static class Configuration {

        public final ArrayList<ClassConfiguration> ext = new ArrayList<>();

        @ToString
        public static class ClassConfiguration {
            @JsonProperty( "class" )
            public final Class<?> clazz;
            public final String field;
            public final Class<?> implementation;

            @JsonCreator
            public ClassConfiguration( Class<?> clazz, String field, Class<?> implementation ) {
                this.clazz = clazz;
                this.field = field;
                this.implementation = implementation;
            }
        }
    }
}
