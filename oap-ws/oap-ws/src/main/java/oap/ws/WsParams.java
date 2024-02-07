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

import lombok.extern.slf4j.Slf4j;
import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflection;
import oap.util.Sets;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toUpperCase;

@Slf4j
public class WsParams {

    /**
     * Helper method to transform camelCase names into Capitalized-Dash-Divided,
     * this method is convenient for creating HTTP header names, like below
     * xCustomHeader -> X-Custom-Header
     * @param camelCase text
     * @return
     */
    @Nonnull
    public static String uncamelHeaderName( @Nonnull String camelCase ) {
        StringBuilder result = new StringBuilder();
        for( int i = 0; i < camelCase.length(); i++ ) {
            char c = camelCase.charAt( i );
            if( i == 0 ) result.append( toUpperCase( c ) );
            else if( isUpperCase( c ) ) result.append( "-" ).append( c );
            else result.append( c );
        }
        return result.toString();
    }

    public static Object fromSession( Session session, Reflection.Parameter parameter ) {
        if( session == null ) return null;
        Optional<Object> value = session.get( parameter.name() );
        return wrapOptional( parameter, value.orElse( null ) );
    }

    public static Object fromHeader( HttpServerExchange exchange, Reflection.Parameter parameter, WsParam wsParam ) {
        log.trace( "headers: {}", exchange.getRequestHeaders() );

        var names = Sets.of( wsParam.name() );
        names.add( uncamelHeaderName( parameter.name() ) );
        names.add( parameter.name() );
        log.trace( "names: {}", names );
        String value = null;
        for( String name : names ) {
            value = exchange.getRequestHeader( name );
            if( value != null ) break;
        }
        return wrapOptional( parameter, value );
    }

    public static Object wrapOptional( Reflection.Parameter parameter, Object value ) throws WsClientException {
        if( parameter.type().isOptional() ) return Optional.ofNullable( value );
        if( value == null ) throw new WsClientException( "'" + parameter + "' is required" );
        return value;
    }

    public static Object fromCookie( HttpServerExchange exchange, Reflection.Parameter parameter, WsParam wsParam ) throws WsClientException {
        var names = Sets.of( wsParam.name() );
        names.add( parameter.name() );
        String cookie = null;
        for( String name : names ) {
            cookie = exchange.getRequestCookieValue( name );
            if( cookie != null ) break;
        }

        return wrapOptional( parameter, cookie );
    }

    public static Optional<String> fromPath( HttpServerExchange exchange, Optional<WsMethod> wsMethod, Reflection.Parameter parameter ) {
        return wsMethod
            .map( wsm -> WsMethodMatcher.pathParam( wsm.path(), exchange.getRelativePath(), parameter.name() ) )
            .orElseThrow( () -> new WsException( "path parameter " + parameter.name() + " without " + WsMethod.class.getName() + " annotation" ) );
    }

    public static Object fromBody( HttpServerExchange exchange, Reflection.Parameter parameter ) throws WsClientException {
        try {
            if( parameter.type().assignableFrom( byte[].class ) ) {
                if( parameter.type().isOptional() ) {
                    var bytes = exchange.readBody();
                    return bytes.length > 0 ? Optional.of( bytes ) : Optional.empty();
                }
                var bytes = exchange.readBody();
                if( bytes.length < 1 ) throw new WsClientException( "no body defined for: " + parameter.type() + ":" + parameter.name() );
                return bytes;
            }
            if( parameter.type().assignableFrom( InputStream.class ) ) {
                return exchange.getInputStream();
            }
            byte[] bytes = exchange.readBody();
            if( parameter.type().isOptional() ) {
                if( bytes.length < 1 ) return Optional.empty();
                return Optional.of( new String( bytes ) );
            }
            if( bytes.length < 1 ) throw new WsClientException( "no body defined for: " + parameter.type() + ":" + parameter.name() );
            return new String( bytes );
        } catch( IOException e ) {
            throw new WsClientException( "Cannot construct from: " + parameter.type() + ":" + parameter.name(), e );
        }
    }

    public static Object fromQuery( HttpServerExchange exchange, Reflection.Parameter parameter, WsParam wsParam ) throws WsClientException {
        Set<String> names = wsParam == null ? Sets.of() : Sets.of( wsParam.name() );
        names.add( parameter.name() );
        if( parameter.type().assignableTo( List.class ) ) {
            for( var name : names ) {
                var values = exchange.exchange.getQueryParameters().get( name );
                if( values == null || values.isEmpty() ) continue;
                return values.stream().toList();
            }
            return List.of();
        }
        String value = null;
        for( var name : names ) {
            value = exchange.getStringParameter( name );
            if( value != null ) break;
        }
        return wrapOptional( parameter, value );
    }

    public static Object fromQuery( HttpServerExchange exchange, Reflection.Parameter parameter ) throws WsClientException {
        return fromQuery( exchange, parameter, null );
    }
}
