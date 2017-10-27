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
import lombok.val;
import oap.util.Arrays;
import oap.util.Pair;
import oap.util.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Reflect {

    private static HashMap<TypeToken<?>, Reflection> reflections = new HashMap<>();
    private static Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private static SecurityManager securityManager = new SecurityManager();

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
        return reflections.computeIfAbsent( token, Reflection::new ).init();
    }

    protected static synchronized Reflection reflect( TypeToken<?> token, Coercions coercions ) {
        return reflections.computeIfAbsent( token, ( typeToken ) -> new Reflection( typeToken, coercions ) ).init();
    }

    public static Reflection reflect( String className ) {
        return reflect( classes.computeIfAbsent( className, Try.map( Class::forName ) ) );
    }

    public static Reflection reflect( String className, Coercions coercions ) {
        return reflect( classes.computeIfAbsent( className, Try.map( Class::forName ) ), coercions );
    }

    public static Class<?> caller() {
        return caller( 0 );
    }

    public static Class<?> caller( int depth ) {
        return securityManager.getClassContext()[2 + depth];
    }

    public static <T> T newInstance( Class<T> clazz, Object... args ) {
        return reflect( clazz ).newInstance( args );
    }

    /**
     * Retrieves value based on specified path inside object
     *
     * @deprecated use {@link #get(Object, String)}} instead.
     */
    @Deprecated
    public static <T> T eval( Object object, String path ) {
        return get( object, path );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T get( Object object, String path ) {
        Object next = object;
        for( String field : StringUtils.split( path, '.' ) ) {
            if( next == null ) break;
            if( field.startsWith( "[" ) && next instanceof Map<?, ?> ) {
                Map<?, ?> map = ( Map<?, ?> ) next;
                String key = field.substring( 1, field.length() - 1 );
                next = map.getOrDefault( key, null );
            } else {
                Object instance = next;
                val f = reflect( next.getClass() ).field( field );
                if( f == null ) {
                    next = null;
                    continue;
                }
                next = f.get( instance );
                if( next == null ) continue;
                if( next instanceof Optional ) next = ( ( Optional ) next ).orElse( null );
            }
        }
        return ( T ) next;
    }

    @SuppressWarnings( "unchecked" )
    public static void set( Object object, String path, Object value ) {
        String[] splittedPath = StringUtils.split( path, '.' );
        Pair<String[], String[]> split = Arrays.splitAt( splittedPath.length - 1, splittedPath );
        String field = split._2[0];
        Object next = get( object, String.join( ".", split._1 ) );
        if( next == null ) return;
        if( field.startsWith( "[" ) && next instanceof Map<?, ?> ) {
            Map<Object, Object> map = ( Map<Object, Object> ) next;
            String key = field.substring( 1, field.length() - 1 );
            map.put( key, value );
        } else {
            val f = reflect( next.getClass() ).field( field );
            if( f != null ) {
                f.set( next, f.type().isOptional() ? Optional.ofNullable( value ) : value );
            }
        }

    }

    private static class SecurityManager extends java.lang.SecurityManager {
        @Override
        public Class[] getClassContext() {
            return super.getClassContext();
        }
    }
}
