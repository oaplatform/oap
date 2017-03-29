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

import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.http.Session;
import oap.json.Binder;
import oap.metrics.Metrics;
import oap.metrics.Name;
import oap.reflect.Coercions;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Optionals;
import oap.util.Result;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.Throwables;
import oap.util.WrappingRuntimeException;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.Validators;
import org.apache.http.entity.ContentType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.http.HttpResponse.NOT_FOUND;
import static oap.http.HttpResponse.NO_CONTENT;
import static oap.util.Collectors.toLinkedHashMap;
import static oap.ws.WsResponse.TEXT;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class WsService implements Handler {
    private final Object impl;
    private final Logger logger;
    private final boolean sessionAware;
    private final Reflection reflection;
    private final WsResponse defaultResponse;
    private final SessionManager sessionManager;
    private final List<Interceptor> interceptors;
    private final Coercions coercions = Coercions.basic()
        .with( r -> true, ( r, value ) -> Binder.json.unmarshal( r.underlying,
            value instanceof String ? ( String ) value : new String( ( byte[] ) value, UTF_8 ) ) );
    private Map<String, Pattern> compiledPaths = new HashMap<>();
    private String cookieId;

    public WsService( Object impl, boolean sessionAware,
                      SessionManager sessionManager, List<Interceptor> interceptors, WsResponse defaultResponse ) {
        this.impl = impl;
        this.logger = LoggerFactory.getLogger( impl.getClass() );
        this.reflection = Reflect.reflect( impl.getClass() );
        this.defaultResponse = defaultResponse;
        this.reflection.methods.forEach( m -> m.findAnnotation( WsMethod.class )
            .ifPresent( a -> compiledPaths.put( a.path(), WsServices.compile( a.path() ) ) )
        );
        this.sessionAware = sessionAware;
        this.sessionManager = sessionManager;
        this.interceptors = interceptors;
    }

    private List<Object> convert( Reflection type, List<String> values ) {
        try {
            return Stream.of( values )
                .map( v -> coercions.cast( type.typeParameters.get( 0 ), v ) )
                .toList();
        } catch( Exception e ) {
            throw new WsClientException( e.getMessage(), e );
        }
    }

    private Object convert( String name, Reflection type, Optional<?> value ) {
        try {
            Optional<Object> result = value.map( v -> coercions.cast( type.isOptional() ?
                type.typeParameters.get( 0 ) : type, v ) );
            return type.isOptional() ? result :
                result.orElseThrow( () -> new WsClientException( name + " is required" ) );
        } catch( Exception e ) {
            throw new WsClientException( e.getMessage(), e );
        }
    }

    private void wsError( Response response, Throwable e ) {
        if( e instanceof ReflectException && e.getCause() != null )
            wsError( response, e.getCause() );
        else if( e instanceof WrappingRuntimeException && e.getCause() != null )
            wsError( response, e.getCause() );
        else if( e instanceof InvocationTargetException )
            wsError( response, ( ( InvocationTargetException ) e ).getTargetException() );
        else if( e instanceof WsClientException ) {
            WsClientException clientException = ( WsClientException ) e;
            logger.debug( e.toString(), e );
            HttpResponse wsResponse = HttpResponse.status( clientException.code, e.getMessage() );
            if( !clientException.errors.isEmpty() ) {
                if( defaultResponse == TEXT ) {
                    wsResponse.withContent( String.join( "\n", clientException.errors ), TEXT_PLAIN );
                } else {
                    final String json = Binder.json.marshal( new JsonErrorResponse( clientException.errors ) );
                    wsResponse.withContent( json, APPLICATION_JSON );
                }
            }
            response.respond( wsResponse );
        } else {
            logger.error( e.toString(), e );
            final HttpResponse wsResponse = HttpResponse.status( HTTP_INTERNAL_ERROR, e.getMessage() );
            if( defaultResponse == TEXT ) {
                wsResponse.withContent( Throwables.getRootCause( e ).getMessage(), TEXT_PLAIN );
            } else {
                final String json = Binder.json.marshal( new JsonStackTraceResponse( e ) );
                wsResponse.withContent( json, APPLICATION_JSON );
            }

            response.respond( wsResponse );
        }
    }

    private boolean methodMatches( String requestLine, Request.HttpMethod httpMethod, Reflection.Method m ) {
        return m.findAnnotation( WsMethod.class )
            .map( a -> oap.util.Arrays.contains( httpMethod, a.method() ) && (
                    ( Strings.isUndefined( a.path() ) && Objects.equals( requestLine, "/" + m.name() ) )
                        || compiledPaths.get( a.path() ).matcher( requestLine ).find()
                )
            ).orElse( m.isPublic() && Objects.equals( requestLine, "/" + m.name() ) );
    }

    @Override
    public void handle( Request request, Response response ) {
        try {
            Optionals.fork( reflection.method(
                method -> methodMatches( request.requestLine, request.httpMethod, method ) ) )
                .ifAbsent( () -> response.respond( NOT_FOUND ) )
                .ifPresent(
                    method -> {
                        Name name = Metrics
                            .name( "rest_timer" )
                            .tag( "service", impl.getClass().getSimpleName() )
                            .tag( "method", method.name() );

                        if( !sessionAware ) {
                            handleInternal( request, response, method, name, false );
                        } else {
                            final Optional<String> internalSession = request.cookie( "SID" );
                            if( internalSession.isPresent() &&
                                sessionManager.getSessionById( internalSession.get() ) != null ) {

                                cookieId = internalSession.get();
                                logger.debug( "Valid SID [{}] found in cookie", cookieId );

                                handleInternal( request, response, method, name, false );
                            } else {
                                cookieId = UUID.randomUUID().toString();

                                logger.debug( "Creating new session with SID [{}]", cookieId );
                                sessionManager.put( cookieId, new Session() );

                                handleInternal( request, response, method, name, true );
                            }
                        }
                    } );
        } catch( Throwable e ) {
            wsError( response, e );
        }
    }

    private void handleInternal( Request request, Response response, Reflection.Method method,
                                 Name name, boolean setCookie ) {
        final Session sessionById = sessionManager.getSessionById( cookieId );

        logger.trace( "Internal session status: [{}] with content [{}]", cookieId, sessionById );

        final HttpResponse interceptorResponse =
            runInterceptors( request, sessionById, method );

        if( interceptorResponse != null ) {
            response.respond( interceptorResponse );
        } else {
            Metrics.measureTimer( name, () -> {
                final Optional<WsMethod> wsMethod = method.findAnnotation( WsMethod.class );
                final ValidationErrors paramValidation = ValidationErrors.empty();
                final List<Reflection.Parameter> parameters = method.parameters;
                final LinkedHashMap<Reflection.Parameter, Object> originalValues = getOriginalValues( parameters, request, wsMethod );

                originalValues.forEach( ( parameter, value ) ->
                    paramValidation.merge(
                        Validators
                            .forParameter( originalValues, method, parameter, impl, true )
                            .validate( value )
                    )
                );

                paramValidation.throwIfInvalid();

                Validators.forMethod( originalValues, method, impl, true )
                    .validate( originalValues.values().toArray( new Object[originalValues.size()] ) )
                    .throwIfInvalid();

                final LinkedHashMap<Reflection.Parameter, Object> values = getValues( originalValues );
                final Object[] paramValues = values.values().toArray( new Object[values.size()] );

                values.forEach( ( parameter, value ) ->
                    paramValidation.merge(
                        Validators
                            .forParameter( values, method, parameter, impl, false )
                            .validate( value )
                    ) );

                paramValidation.throwIfInvalid();

                Validators.forMethod( values, method, impl, false )
                    .validate( paramValues )
                    .throwIfInvalid();

                Object result = method.invoke( impl, paramValues );

                Boolean isRaw = wsMethod.map( WsMethod::raw ).orElse( false );
                ContentType produces =
                    wsMethod.map( wsm -> ContentType.create( wsm.produces() )
                        .withCharset( UTF_8 ) )
                        .orElse( APPLICATION_JSON );

                final String cookie = setCookie ?
                    new HttpResponse.CookieBuilder()
                        .withSID( cookieId )
                        .withPath( sessionManager.cookiePath )
                        .withExpires( DateTime.now().plusMinutes( sessionManager.cookieExpiration ) )
                        .withDomain( sessionManager.cookieDomain )
                        .build()
                    : null;

                if( method.isVoid() ) response.respond( NO_CONTENT );
                else if( result instanceof HttpResponse )
                    response.respond( ( ( HttpResponse ) result ).withCookie( cookie ) );
                else if( result instanceof Optional<?> ) {
                    response.respond(
                        ( ( Optional<?> ) result )
                            .map( r -> HttpResponse.ok( r, isRaw, produces ).withCookie( cookie ) )
                            .orElse( NOT_FOUND )
                    );
                } else if( result instanceof Result<?, ?> ) {
                    final Result<HttpResponse, HttpResponse> resp = ( ( Result<?, ?> ) result )
                        .mapSuccess( r -> HttpResponse.ok( r, isRaw, produces ).withCookie( cookie ) )
                        .mapFailure( r -> HttpResponse.status( HTTP_INTERNAL_ERROR, "", r ).withCookie( cookie ) );

                    response.respond( resp.isSuccess() ? ( ( Result<?, ?> ) result )
                        .mapSuccess( r -> HttpResponse.ok( r, isRaw, produces ).withCookie( cookie ) ).successValue
                        : ( ( Result<?, ?> ) result )
                            .mapFailure( r -> HttpResponse.status( HTTP_INTERNAL_ERROR, "", r ).withCookie( cookie ) )
                            .failureValue );

                } else if( result instanceof Stream<?> ) {
                    response.respond( HttpResponse.stream( ( Stream<?> ) result, isRaw, produces ).withCookie( cookie ) );
                } else response.respond( HttpResponse.ok( result, isRaw, produces ).withCookie( cookie ) );
            } );
        }
    }

    private LinkedHashMap<Reflection.Parameter, Object> getValues( LinkedHashMap<Reflection.Parameter, Object> values ) {
        return values
            .entrySet()
            .stream()
            .collect( toLinkedHashMap( Map.Entry::getKey, p -> p.getValue() instanceof List
                ? convert( p.getKey().type(), ( List ) p.getValue() )
                : convert( p.getKey().name(), p.getKey().type(),
                    p.getValue() instanceof Optional ? ( Optional ) p.getValue()
                        : Optional.ofNullable( p.getValue() ) ) ) );
    }

    private HttpResponse runInterceptors( Request request, Session session, Reflection.Method method ) {

        for( Interceptor interceptor : interceptors ) {
            final Optional<HttpResponse> interceptorResponse = interceptor.intercept( request, session, method );
            if( interceptorResponse.isPresent() ) {
                return interceptorResponse.get();
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return impl.getClass().getName();
    }

    private Object unwrap( Reflection.Parameter parameter, Optional<?> opt ) {
        if( parameter.type().isOptional() ) return opt;

        return opt.orElseThrow( () -> new WsClientException( parameter.name() + " is required" ) );
    }

    public LinkedHashMap<Reflection.Parameter, Object> getOriginalValues(
        List<Reflection.Parameter> parameters, Request request, Optional<WsMethod> wsMethod ) {

        final Session sessionById = sessionManager.getSessionById( cookieId );

        return parameters.stream().collect( toLinkedHashMap( parameter -> parameter, parameter -> {
            Object value = parameter.findAnnotation( WsParam.class )
                .<Object>map( wsParam -> {
                    switch( wsParam.from() ) {
                        case REQUEST:
                            return request;
                        case SESSION:
                            return parameter.type().isOptional() ?
                                sessionById.get( parameter.name() ) :
                                sessionById.get( parameter.name() ).orElse( null );
                        case HEADER:
                            return unwrap( parameter, request.header( parameter.name() ) );
                        case PATH:
                            return wsMethod.map( wsm -> WsServices.pathParam( wsm.path(), request.requestLine,
                                parameter.name() ) )
                                .orElseThrow( () -> new WsException(
                                    "path parameter " + parameter.name() + " without " +
                                        WsMethod.class.getName() + " annotation" ) );
                        case BODY:
                            return parameter.type().assignableFrom( byte[].class ) ?
                                ( parameter.type().isOptional() ? request.readBody() :
                                    request.readBody()
                                        .orElseThrow( () -> new WsClientException(
                                            "no body for " + parameter.name() ) )
                                ) :
                                unwrap( parameter, request.readBody().map( String::new ) );
                        default:
                            return parameter.type().assignableTo( List.class ) ?
                                request.parameters( parameter.name() ) :
                                unwrap( parameter, request.parameter( parameter.name() ) );

                    }
                } )
                .orElseGet( () -> parameter.type().assignableTo( List.class ) ?
                    request.parameters( parameter.name() ) :
                    unwrap( parameter, request.parameter( parameter.name() ) )
                );
            return value;
        } ) );
    }


    private static class JsonErrorResponse implements Serializable {
        private static final long serialVersionUID = 4949051855248389697L;
        public List<String> errors;

        public JsonErrorResponse( List<String> errors ) {
            this.errors = errors;
        }
    }

    private static class JsonStackTraceResponse implements Serializable {
        private static final long serialVersionUID = 8431608226448804296L;

        public final String message;

        public JsonStackTraceResponse( Throwable t ) {
            message = Throwables.getRootCause( t ).getMessage();
        }
    }
}
