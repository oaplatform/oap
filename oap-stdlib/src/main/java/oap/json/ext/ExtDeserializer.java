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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.configuration.ConfigurationLoader;
import oap.json.ClassDeserializer;
import oap.json.ext.ExtDeserializer.Configuration.ClassConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ExtDeserializer extends StdDeserializer<Ext> {
    private static volatile HashMap<String, ClassConfiguration> extmap;

    protected ExtDeserializer() {
        this( Ext.class );
    }

    public ExtDeserializer( Class clazz ) {
        super( clazz );
    }

    private static void init() {
        if( extmap == null ) {
            synchronized( ExtDeserializer.class ) {
                if( extmap == null ) {
                    extmap = new HashMap<>();
                    List<Configuration> conf = ConfigurationLoader.read( ExtDeserializer.class, new TypeReference<>() {} );
                    for( Configuration c : conf ) {
                        for( ClassConfiguration cc : c.config ) {
                            String key = extensionKey( cc.clazz, cc.field );
                            var oldConf = extmap.get( key );
                            if( oldConf != null && oldConf.disableOverwrite ) continue;
                            extmap.put( key, cc );
                            log.debug( "add ext rule: {} = {}", key, cc.implementation );
                        }
                    }
                    log.trace( "mapped extensions: {}", extmap );
                }
            }
        }
    }

    private static String extensionKey( Class<?> clazz, String field ) {
        return clazz.getName() + "#" + field;
    }

    public static Class<?> extensionOf( Class<?> clazz, String field ) {
        init();

        var classConfiguration = extmap.get( extensionKey( clazz, field ) );
        if( classConfiguration != null ) return classConfiguration.implementation;
        return null;
    }

    @SuppressWarnings( "rawtypes" )
    public static Map<Class, ExtDeserializer> getDeserializers() {
        init();

        var ret = new HashMap<Class, ExtDeserializer>();

        ret.put( Ext.class, new ExtDeserializer() );
        for( var c : extmap.values() ) {
            if( c.abstractClass == null ) continue;
            var clazz = c.abstractClass;

            ret.put( clazz, new ExtDeserializer( c.implementation ) );
        }

        return ret;
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

    @ToString
    public static class Configuration extends ConfigurationLoader.Configuration<ArrayList<ClassConfiguration>> {
        @ToString
        public static class ClassConfiguration {
            @JsonProperty( "class" )
            @JsonDeserialize( using = ClassDeserializer.class )
            public Class<?> clazz;
            public String field;
            public boolean disableOverwrite;
            @JsonDeserialize( using = ClassDeserializer.class )
            public Class<?> implementation;
            @JsonProperty( "abstract" )
            @JsonDeserialize( using = ClassDeserializer.class )
            public Class<?> abstractClass = null;
        }
    }
}
