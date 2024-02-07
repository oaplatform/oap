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

package oap.ws;

import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Arrays;
import oap.util.Maps;
import oap.util.Stream;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oap.util.Pair.__;
import static oap.util.Strings.isUndefined;

public class WsMethodMatcher {
    private static final Pattern RX_PARAM_PATTERN = Pattern.compile( "\\{([^:]+):([^)]+\\))}" );
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile( "(?<=[/=])\\{([^}]+)}" );


    private final Reflection reflection;
    private final Map<String, Pattern> paths;

    public WsMethodMatcher( Class<?> wsClass ) {
        this.reflection = Reflect.reflect( wsClass );
        this.paths = Stream.of( reflection.methods )
            .map( m -> m.findAnnotation( WsMethod.class ) )
            .filter( Optional::isPresent )
            .map( Optional::get )
            .map( a -> __( a.path(), compile( a.path() ) ) )
            .collect( Maps.Collectors.toMap() );
    }

    static int constantFirst( Reflection.Method o1, Reflection.Method o2 ) {
        var path1 = o1.findAnnotation( WsMethod.class ).map( WsMethod::path ).orElse( o1.name() );
        var path2 = o2.findAnnotation( WsMethod.class ).map( WsMethod::path ).orElse( o1.name() );

        return path1.compareTo( path2 );
    }

    public static Pattern compile( String mapping ) {
        var pattern = NAMED_PARAM_PATTERN.matcher( RX_PARAM_PATTERN.matcher( mapping ).replaceAll( "$2" ) )
            .replaceAll( "([^/]+)" );
        return Pattern.compile( '^' + ( pattern.equals( "/" ) ? "/?" : pattern ).replace( "=", "\\=" ) + '$' );
    }

    private static String filter( String mapping ) {
        return RX_PARAM_PATTERN.matcher( mapping ).replaceAll( "{$1}" );
    }

    public static Optional<String> pathParam( String mapping, String path, String name ) {
        Matcher matcher = NAMED_PARAM_PATTERN.matcher( filter( mapping ) );
        return Stream.of( matcher::find, matcher::group )
            .zipWithIndex()
            .filter( p -> p._1.equals( "{" + name + "}" ) )
            .map( p -> p._2 )
            .findFirst()
            .flatMap( group -> {
                Matcher matcher1 = compile( mapping ).matcher( path );
                return matcher1.matches() && group <= matcher1.groupCount()
                    ? Optional.of( matcher1.group( group + 1 ) )
                    : Optional.empty();
            } );
    }

    @SuppressWarnings( "checkstyle:UnnecessaryParentheses" )
    private boolean match( String requestLine, HttpServerExchange.HttpMethod httpMethod, Reflection.Method m ) {
        WsMethod annotation = m.findAnnotation( WsMethod.class ).orElse( null );
        if( annotation == null )
            return m.isPublic() && Objects.equals( requestLine, "/" + m.name() );

        boolean contains = Arrays.contains( httpMethod, annotation.method() );
        boolean b = ( isUndefined( annotation.path() ) && Objects.equals( requestLine, "/" + m.name() ) )
            || paths.get( annotation.path() ).matcher( requestLine ).find();
        return contains && b;
    }

    public Optional<Reflection.Method> findMethod( String requestLine, HttpServerExchange.HttpMethod httpMethod ) {
        return reflection.method( m -> match( requestLine, httpMethod, m ), WsMethodMatcher::constantFirst );
    }
}
