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

package oap.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.configuration.ConfigurationLoader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TypeIdFactory implements TypeIdResolver {
    private static ConcurrentHashMap<Class<?>, String> classToId;
    private static volatile LinkedHashMap<String, Class<?>> idToClass;
    private JavaType baseType;

    public static boolean containsId( String id ) {
        init();

        return idToClass.containsKey( id );
    }

    public static boolean containsClass( Class<?> clazz ) {
        init();

        return classToId.containsKey( clazz );
    }

    public static Class<?> get( String id ) {
        init();

        return idToClass.get( id );
    }

    public static String get( Class clazz ) {
        init();

        return classToId.get( clazz );
    }

    public static void register( Class<?> bean, String id ) {
        init();

        idToClass.put( id, bean );
        classToId.put( bean, id );
    }

    public static Set<String> keys() {
        init();

        return idToClass.keySet();
    }

    public static Set<Class<?>> values() {
        init();

        return classToId.keySet();
    }

    public static void clear() {
        init();

        idToClass.clear();
        classToId.clear();
    }

    private static void init() {
        if( idToClass == null ) {
            synchronized( TypeIdFactory.class ) {
                if( idToClass == null ) {
                    idToClass = new LinkedHashMap<>();
                    classToId = new ConcurrentHashMap<>();

                    List<Configuration> conf = ConfigurationLoader.read( TypeIdFactory.class, new TypeReference<>() {} );
                    for( var c : conf ) {
                        c.config.forEach( ( k, v ) -> {
                            register( v, k );
                        } );
                    }
                }
            }
        }
    }

    @Override
    public void init( JavaType baseType ) {
        this.baseType = baseType;
    }

    @Override
    public String idFromValue( Object value ) {
        return idFromValueAndType( value, value.getClass() );
    }

    @Override
    public String idFromValueAndType( Object value, Class<?> suggestedType ) {
        init();

        return classToId.computeIfAbsent( suggestedType, k -> {
            throw new IllegalStateException( "cannot find class '" + k + "'" );
        } );
    }

    @Override
    public String idFromBaseType() {
        return idFromValueAndType( null, baseType.getRawClass() );
    }

    @Override
    public JavaType typeFromId( DatabindContext context, String id ) {
        init();

        final Class<?> clazz = idToClass.computeIfAbsent( id, k -> {
            throw new IllegalStateException( "cannot find id '" + k + "'" );
        } );
        return TypeFactory.defaultInstance().constructSpecializedType( baseType, clazz );
    }

    @Override
    public String getDescForKnownTypeIds() {
        init();

        return String.join( ",", idToClass.keySet() );
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CLASS;
    }

    @ToString
    public static class Configuration extends ConfigurationLoader.Configuration<Configuration.ClassConfiguration> {
        @ToString
        @JsonDeserialize( contentUsing = ClassDeserializer.class )
        public static class ClassConfiguration extends LinkedHashMap<String, Class<?>> {

        }

    }
}
