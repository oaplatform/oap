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
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Igor Petrenko on 14.03.2016.
 */
public class TypeIdFactory implements TypeIdResolver {
    private final static ConcurrentHashMap<String, Class<?>> idToClass = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Class<?>, String> classToId = new ConcurrentHashMap<>();
    private JavaType baseType;

    public static boolean containsId( String id ) {
        return idToClass.contains( id );
    }

    public static Class<?> get( String id ) {
        return idToClass.get( id );
    }

    public static void register( Class<?> bean, String id ) {
        idToClass.put( id, bean );
        classToId.put( bean, id );
    }

    public static Set<String> ids() {
        return idToClass.keySet();
    }

    public static void clear() {
        idToClass.clear();
        classToId.clear();
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
        return classToId.computeIfAbsent( suggestedType, ( k ) -> {
            throw new IllegalStateException( "cannot find class '" + k + "'" );
        } );
    }

    @Override
    public String idFromBaseType() {
        return idFromValueAndType( null, baseType.getRawClass() );
    }

    @Override
    public JavaType typeFromId( String id ) {
        final Class<?> clazz = idToClass.computeIfAbsent( id, ( k ) -> {
            throw new IllegalStateException( "cannot find id '" + k + "'" );
        } );
        return TypeFactory.defaultInstance().constructSpecializedType( baseType, clazz );
    }

    @Override
    public JavaType typeFromId( DatabindContext context, String id ) {
        return typeFromId( id );
    }

    @Override
    public String getDescForKnownTypeIds() {
        return idToClass.keySet().stream().collect( Collectors.joining( "," ) );
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CLASS;
    }
}
