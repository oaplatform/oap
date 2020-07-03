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
import oap.json.ext.ExtDeserializer.Configuration.ClassConfiguration;
import oap.util.Throwables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ExtDeserializer<T extends Ext> extends StdDeserializer<T> {
    private static final HashMap<String, ClassConfiguration> extmap = new HashMap<>();

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
                    extmap.put( key, cc );

                    log.debug( "add ext rule: {} = {}", key, cc.implementation );
                }
            }
            log.trace( "mapped extensions: {}", extmap );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
            throw Throwables.propagate( e );
        }
    }

    protected ExtDeserializer() {
        this( Ext.class );
    }

    public ExtDeserializer( Class<? extends Ext> clazz ) {
        super( clazz );
    }

    private static String extensionKey( Class<?> clazz, String field ) {
        return clazz.getName() + "#" + field;
    }

    public static Class<?> extensionOf( Class<?> clazz, String field ) {
        var classConfiguration = extmap.get( extensionKey( clazz, field ) );
        if( classConfiguration != null ) return classConfiguration.implementation;
        return null;
    }

    public static Map<Class<?>, ExtDeserializer> getDeserializers() {
        var ret = new HashMap<Class<?>, ExtDeserializer>();

        ret.put( Ext.class, new ExtDeserializer() );
        for( var c : extmap.values() ) {
            if( c._abstract == null ) continue;
            var clazz = c._abstract;

            ret.put( clazz, new ExtDeserializer( clazz ) );
        }

        return ret;
    }

    @Override
    public T deserialize( JsonParser jsonParser, DeserializationContext ctxt ) throws IOException {
        var parsingContext = jsonParser.getParsingContext();
        var contextParent = parsingContext.getParent();
        var field = contextParent.getCurrentName();
        var clazz = contextParent.getCurrentValue().getClass();
        var implementation = extensionOf( clazz, field );
        if( implementation == null ) {
            log.trace( "extension class lookup failed for {}#{}", clazz, field );
            return null;
        }

        return ( T ) jsonParser.readValueAs( implementation );
    }

    @ToString
    public static class Configuration {

        public final ArrayList<ClassConfiguration> ext = new ArrayList<>();

        @ToString
        public static class ClassConfiguration {
            @JsonProperty( "class" )
            public Class<?> clazz;
            public String field;
            public Class<?> implementation;
            @JsonProperty( "abstract" )
            public Class<?> _abstract = null;
        }
    }
}
