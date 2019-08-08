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
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;
import oap.util.Throwables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by igor.petrenko on 07.08.2019.
 */
@Slf4j
public class ExtDeserializer2 extends StdDeserializer<Ext> {
    private static final HashMap<ClassFieldKey, Class<?>> extmap = new HashMap<>();

    static {
        try {
            for( var p : Resources.readStrings( "META-INF/json-ext.yaml" ) ) {
                log.trace( "mapping ext {}", p );

                var mapper = new ObjectMapper( new YAMLFactory() );
                mapper.getDeserializationConfig().with( new JacksonAnnotationIntrospector() );
                mapper.registerModule( new ParameterNamesModule( JsonCreator.Mode.DEFAULT ) );
                var conf = mapper.readValue( p, JsonExtConfiguration.class );

                for( var cc : conf.jackson.ext ) {
                    var key = new ClassFieldKey( cc.klass, cc.field );
                    var impl = cc.implementation;
                    extmap.put( key, impl );

                    log.debug( "add ext rule: {} = {}", key, impl );
                }

            }
            log.trace( "mapped extensions: {}", extmap );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
            throw Throwables.propagate( e );
        }
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

    @ToString
    public static class JsonExtConfiguration {
        public final JacksonConfiguration jackson;

        @JsonCreator
        public JsonExtConfiguration( JacksonConfiguration jackson ) {
            this.jackson = jackson;
        }

        @ToString
        public static class JacksonConfiguration {
            public final ArrayList<ClassConfiguration> ext = new ArrayList<>();
        }

        @ToString
        public static class ClassConfiguration {
            @JsonProperty( "class" )
            public final Class<?> klass;
            public final String field;
            public final Class<?> implementation;

            @JsonCreator
            public ClassConfiguration( Class<?> klass, String field, Class<?> implementation ) {
                this.klass = klass;
                this.field = field;
                this.implementation = implementation;
            }
        }
    }
}
