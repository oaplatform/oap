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
package oap.reflect;

import com.google.common.reflect.TypeToken;
import oap.util.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Reflect {

    private static HashMap<TypeToken<?>, Reflection> reflections = new HashMap<>();
    private static Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    public static Reflection reflect( TypeRef<?> ref ) {
        return reflect( ref.token );
    }

    public static Reflection reflect( Class<?> clazz ) {
        return reflect( TypeToken.of( clazz ) );
    }

    public static Reflection reflect( Class<?> clazz, Coercions coercions ) {
        return reflect( TypeToken.of( clazz ), coercions );
    }

    protected static synchronized Reflection reflect( TypeToken<?> token ) {
        return reflections.computeIfAbsent( token, Reflection::new );
    }

    protected static synchronized Reflection reflect( TypeToken<?> token, Coercions coercions ) {
        return reflections.computeIfAbsent( token, ( typeToken ) -> new Reflection( typeToken, coercions ) );
    }

    public static Reflection reflect( String className ) {
        return reflect( classes.computeIfAbsent( className, Try.map( Class::forName ) ) );
    }

    public static Reflection reflect( String className, Coercions coercions ) {
        return reflect( classes.computeIfAbsent( className, Try.map( Class::forName ) ), coercions );
    }

    public static <T> T newInstance( Class<T> clazz, Object... args ) {
        return reflect( clazz ).newInstance( args );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T eval( Object o, String path ) {
        Object next = o;
        for( String field : StringUtils.split( path, '.' ) ) {
            if( next == null ) break;
            final Object instance = next;
            next = reflect( next.getClass() )
                .field( field )
                .map( f -> f.get( instance ) )
                .map( v -> v instanceof Optional ? ( ( Optional ) v ).orElse( null ) : v )
                .orElse( null );
        }
        return ( T ) next;
    }

//    unapply - check possibility to parameterize with Function to pass parameters from class.
//    implementation should be based on unapply/constructor. Probably wont help because of erasure.
//    bet there is a chance to get good enough syntax for instance matching
}
