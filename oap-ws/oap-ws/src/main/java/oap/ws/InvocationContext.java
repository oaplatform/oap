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
import oap.json.Binder;
import oap.reflect.Reflection;
import oap.util.BiStream;
import oap.util.Stream;
import oap.util.function.Functions;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static oap.util.Pair.__;

@Slf4j
public class InvocationContext {
    public final HttpServerExchange exchange;
    public final Session session;
    public final Reflection.Method method;
    private final Supplier<Map<Reflection.Parameter, Object>> unparsedParameters = Functions.memoize( this::retrieveParameters );
    private final Supplier<Map<Reflection.Parameter, Object>> parsedParameters = Functions.memoize( this::parseParameters );
    private final Supplier<Map<String, Object>> namedParameters = Functions.memoize( this::nameParameters );
    private final Optional<WsMethod> methodAnnotation;

    public InvocationContext( HttpServerExchange exchange, Session session, Reflection.Method method ) {
        this.exchange = exchange;
        this.session = session;
        this.method = method;
        this.methodAnnotation = method.findAnnotation( WsMethod.class );
    }

    public Map<Reflection.Parameter, Object> unparsedParameters() {
        return unparsedParameters.get();
    }

    public Map<Reflection.Parameter, Object> parsedParameters() {
        return parsedParameters.get();
    }

    private Map<Reflection.Parameter, Object> retrieveParameters() {
        return Stream.of( method.parameters )
            .mapToPairs( parameter -> __( parameter, getValue( parameter ) ) )
            .toMap();
    }

    private Object getValue( Reflection.Parameter parameter ) {
        return parameter.type().assignableFrom( HttpServerExchange.class )
            ? new RoHttpServerExchange( exchange )
            : parameter.type().assignableFrom( Session.class )
                ? session
                : parameter.findAnnotation( WsParam.class )
                    .map( wsParam -> switch( wsParam.from() ) {
                        case SESSION -> WsParams.fromSession( session, parameter );
                        case HEADER -> WsParams.fromHeader( exchange, parameter, wsParam );
                        case COOKIE -> WsParams.fromCookie( exchange, parameter, wsParam );
                        case PATH -> WsParams.fromPath( exchange, methodAnnotation, parameter );
                        case BODY -> WsParams.fromBody( exchange, parameter );
                        case QUERY -> WsParams.fromQuery( exchange, parameter, wsParam );
                    } ).orElseGet( () -> WsParams.fromQuery( exchange, parameter ) );
    }

    private Map<Reflection.Parameter, Object> parseParameters() {
        try {
            return BiStream.of( unparsedParameters() )
                .map( ( key, value ) -> __( key, map( key.type(), value ) ) )
                .toMap();
        } catch( Exception ex ) {
            log.warn( "Cannot parse parameters from {}", unparsedParameters(), ex.getCause() != null ? ex.getCause() : ex );
            throw ex;
        }
    }

    @SuppressWarnings( { "unchecked", "checkstyle:ParameterAssignment" } )
    private Object map( Reflection reflection, Object value ) {
        try {
            if( reflection.isOptional() && value instanceof Optional optValue )
                return optValue.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable( map( reflection.typeParameters.get( 0 ), optValue.orElseThrow() ) );
            else {
                if( value instanceof Optional optValue ) return map( reflection, optValue.orElseThrow() );
                if( reflection.isEnum() )
                    return Enum.valueOf( ( Class<Enum> ) reflection.underlying, ( String ) value );

                // what is this for? I sincerelly hope there is a test for it.
                if( !( value instanceof String ) && Collection.class.isAssignableFrom( reflection.underlying ) )
                    value = Binder.json.marshal( value );
                if( reflection.underlying.isInstance( value ) ) {
                    return value;
                }
                return Binder.json.unmarshal( reflection, ( String ) value );
            }
        } catch( Exception e ) {
            log.error( "Cannot map/deserialize {} into {}", value, reflection.underlying, e );
            throw new WsClientException( "Cannot map/deserialize " + value + " into " + reflection.underlying, e );
        }
    }

    private Map<String, Object> nameParameters() {
        return BiStream.of( parsedParameters() )
            .map( ( key, value ) -> __( key.name(), value ) )
            .toMap();
    }

    @SuppressWarnings( "unchecked" )
    public <P> Optional<P> getParameter( String name ) {
        return Optional.ofNullable( ( P ) namedParameters.get().get( name ) );
    }
}
