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
import oap.util.Arrays;
import oap.util.Pair;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SuppressWarnings( "UnstableApiUsage" )
public class Reflect {

    private static final HashMap<TypeToken<?>, Reflection> reflections = new HashMap<>();
    private static final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private static final SecurityManager securityManager = new SecurityManager();

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
        return reflections.computeIfAbsent( token, typeToken -> new Reflection( typeToken, coercions ) ).init();
    }

    public static Reflection reflect( String className ) throws ReflectException {
        return reflect( classes.computeIfAbsent( className, cn -> {
            try {
                return Class.forName( cn );
            } catch( ClassNotFoundException e ) {
                throw new ReflectException( e );
            }
        } ) );
    }

    public static Reflection reflect( String className, Coercions coercions ) throws ReflectException {
        return reflect( classes.computeIfAbsent( className, cn -> {
            try {
                return Class.forName( cn );
            } catch( ClassNotFoundException e ) {
                throw new ReflectException( e );
            }
        } ), coercions );
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
            } else if( field.startsWith( "[" ) && next instanceof List<?> ) {
                List<?> list = ( List<?> ) next;
                int key = Integer.parseInt( field.substring( 1, field.length() - 1 ) );
                next = list.size() < key ? null : list.get( key );
            } else {
                Object instance = next;

                next = reflect( next.getClass() )
                    .field( field )
                    .map( f -> f.get( instance ) )
                    .orElse( null );
                if( next instanceof Optional ) next = ( ( Optional ) next ).orElse( null );
            }
        }
        return ( T ) next;
    }

    public static void set( Object object, String path, Object value ) {
        set( object, path, value, false );
    }

    @SuppressWarnings( "unchecked" )
    public static void set( Object object, String path, Object value, boolean removeNullValues ) {
        String[] splittedPath = StringUtils.split( path, '.' );
        Pair<String[], String[]> split = Arrays.splitAt( splittedPath.length - 1, splittedPath );
        String field = split._2[0];
        Object next = get( object, String.join( ".", split._1 ) );
        if( next == null ) return;
        if( field.startsWith( "[" ) && next instanceof Map<?, ?> ) {
            Map<Object, Object> map = ( Map<Object, Object> ) next;
            String key = field.substring( 1, field.length() - 1 );
            if( value == null && removeNullValues ) map.remove( key );
            else map.put( key, value );
        } else if( field.startsWith( "[" ) && next instanceof List<?> ) {
            List<Object> list = ( List<Object> ) next;
            String index = field.substring( 1, field.length() - 1 ).trim();
            if( "*".equals( index ) ) list.add( value );
            else {
                int key = Integer.parseInt( index );
                if( value == null && removeNullValues ) {
                    if( key < list.size() ) list.remove( key );
                } else {
                    while( list.size() <= key ) list.add( null );
                    list.set( key, value );
                }
            }
        } else {
            reflect( next.getClass() )
                .field( field )
                .ifPresent( f -> f.set( next, f.type().isOptional() ? Optional.ofNullable( value ) : value ) );
        }

    }

    public static Function<String, Object> substitutor( Map<String, Object> bindings ) {
        return macro -> {
            Pair<String, String> split = Strings.split( macro, "." );
            return get( bindings.get( split._1 ), split._2 );
        };
    }

    private static class SecurityManager extends java.lang.SecurityManager {
        @Override
        public Class[] getClassContext() {
            return super.getClassContext();
        }
    }
}
