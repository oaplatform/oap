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

package oap.template.runtime;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JVM-wide cache of {@link Field} and {@link Method} references, pre-made accessible,
 * to minimise reflection overhead in the runtime interpreter.
 */
public final class ReflectionCache {
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private ReflectionCache() {}

    /**
     * Read the value of {@code fieldName} on {@code obj}, traversing the class hierarchy.
     * Returns {@code null} when {@code obj} is {@code null} or the field cannot be found.
     */
    @Nullable
    public static Object getFieldValue( @Nullable Object obj, String fieldName ) {
        if( obj == null ) return null;
        String key = obj.getClass().getName() + '#' + fieldName;
        Field f = FIELD_CACHE.computeIfAbsent( key, k -> findField( obj.getClass(), fieldName ) );
        if( f == null ) return null;
        try {
            return f.get( obj );
        } catch( IllegalAccessException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Invoke a zero-or-more-argument instance method by name, with pre-parsed arguments.
     */
    public static Object invokeMethod( Object obj, String methodName, List<String> rawArgs ) {
        if( obj == null ) return null;
        String key = obj.getClass().getName() + '#' + methodName + '/' + rawArgs.size();
        Method m = METHOD_CACHE.computeIfAbsent( key, k -> findMethod( obj.getClass(), methodName, rawArgs.size() ) );
        if( m == null ) return null;
        try {
            Object[] args = parseArgs( m.getParameterTypes(), rawArgs );
            return m.invoke( obj, args );
        } catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /** Parse raw string args to the concrete types expected by the method. */
    public static Object[] parseArgs( Class<?>[] paramTypes, List<String> rawArgs ) {
        Object[] args = new Object[rawArgs.size()];
        for( int i = 0; i < rawArgs.size(); i++ ) {
            args[i] = parseArg( paramTypes[i], rawArgs.get( i ) );
        }
        return args;
    }

    /** Parse a single raw argument string to the target Java type. */
    public static Object parseArg( Class<?> targetType, String raw ) {
        String s = raw;
        if( s.startsWith( "\"" ) && s.endsWith( "\"" ) ) {
            s = s.substring( 1, s.length() - 1 );
            if( String.class.equals( targetType ) ) return s;
        }
        if( int.class.equals( targetType ) || Integer.class.equals( targetType ) ) return Integer.parseInt( s );
        if( long.class.equals( targetType ) || Long.class.equals( targetType ) ) return Long.parseLong( s );
        if( double.class.equals( targetType ) || Double.class.equals( targetType ) ) return Double.parseDouble( s );
        if( float.class.equals( targetType ) || Float.class.equals( targetType ) ) return Float.parseFloat( s );
        if( short.class.equals( targetType ) || Short.class.equals( targetType ) ) return Short.parseShort( s );
        if( byte.class.equals( targetType ) || Byte.class.equals( targetType ) ) return Byte.parseByte( s );
        if( boolean.class.equals( targetType ) || Boolean.class.equals( targetType ) ) return Boolean.parseBoolean( s );
        return s;
    }

    @Nullable
    private static Field findField( Class<?> clazz, String name ) {
        Class<?> c = clazz;
        while( c != null ) {
            try {
                Field f = c.getDeclaredField( name );
                f.setAccessible( true );
                return f;
            } catch( NoSuchFieldException e ) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    @Nullable
    private static Method findMethod( Class<?> clazz, String name, int argCount ) {
        return Arrays.stream( clazz.getMethods() )
            .filter( m -> m.getName().equals( name ) && m.getParameterCount() == argCount )
            .findFirst()
            .map( m -> {
                m.setAccessible( true );
                return m;
            } )
            .orElse( null );
    }
}
