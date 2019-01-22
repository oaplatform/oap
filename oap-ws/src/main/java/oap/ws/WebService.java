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
import lombok.val;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.http.Session;
import oap.json.Binder;
import oap.json.JsonException;
import oap.metrics.Metrics;
import oap.metrics.Name;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Pair;
import oap.util.Result;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.Throwables;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.Validators;
import org.apache.http.entity.ContentType;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.http.HttpResponse.NOT_FOUND;
import static oap.http.HttpResponse.NO_CONTENT;
import static oap.util.Collectors.toLinkedHashMap;
import static oap.util.Pair.__;
import static oap.ws.WsResponse.TEXT;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Slf4j
public class WebService implements Handler {
    private final boolean sessionAware;
    private final Reflection reflection;
    private final WsResponse defaultResponse;
    private final HashMap<Class<?>, Integer> exceptionToHttpCode = new HashMap<>();
    private final SessionManager sessionManager;
    private final List<Interceptor> interceptors;
    private Object instance;
    private Map<String, Pattern> compiledPaths = new HashMap<>();

    public WebService( Object instance, boolean sessionAware,
                       SessionManager sessionManager, List<Interceptor> interceptors, WsResponse defaultResponse,
                       Map<String, Integer> exceptionToHttpCode ) {
        this.instance = instance;
        this.reflection = Reflect.reflect( instance.getClass() );
        this.defaultResponse = defaultResponse;
        this.reflection.methods.forEach( m -> m.findAnnotation( WsMethod.class )
            .ifPresent( a -> compiledPaths.put( a.path(), WsServices.compile( a.path() ) ) )
        );
        this.sessionAware = sessionAware;
        this.sessionManager = sessionManager;
        this.interceptors = interceptors;

        exceptionToHttpCode.forEach( ( clazz, code ) -> {
            try {
                this.exceptionToHttpCode.put( Class.forName( clazz ), code );
            } catch( ClassNotFoundException e ) {
                log.trace( e.getMessage(), e );
            }
        } );
    }

    private void wsError( Response response, Throwable e ) {
        if( e instanceof ReflectException && e.getCause() != null )
            wsError( response, e.getCause() );
        else if( e instanceof InvocationTargetException )
            wsError( response, ( ( InvocationTargetException ) e ).getTargetException() );
        else if( e instanceof WsClientException ) {
            WsClientException clientException = ( WsClientException ) e;
            log.debug( service() + ": " + e.toString(), e );
            HttpResponse wsResponse = HttpResponse.status( clientException.code, e.getMessage() );
            if( !clientException.errors.isEmpty() ) {
                if( defaultResponse == TEXT ) {
                    wsResponse.withContent( String.join( "\n", clientException.errors ), TEXT_PLAIN );
                } else {
                    String json = Binder.json.marshal( new JsonErrorResponse( clientException.errors ) );
                    wsResponse.withContent( json, APPLICATION_JSON );
                }
            }
            response.respond( wsResponse );
        } else {
            log.error( service() + ": " + e.toString(), e );

            val code = exceptionToHttpCode.getOrDefault( e.getClass(), HTTP_INTERNAL_ERROR );

            HttpResponse wsResponse = HttpResponse.status( code, e.getMessage() );
            if( defaultResponse == TEXT ) {
                wsResponse.withContent( Throwables.getRootCause( e ).getMessage(), TEXT_PLAIN );
            } else {
                String json = Binder.json.marshal( new JsonStackTraceResponse( e ) );
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
            val method = reflection.method( m -> methodMatches( request.requestLine, request.httpMethod, m ), ( o1, o2 ) -> {
                val path1 = o1.findAnnotation( WsMethod.class ).map( WsMethod::path ).orElse( o1.name() );
                val path2 = o2.findAnnotation( WsMethod.class ).map( WsMethod::path ).orElse( o1.name() );

                return path1.compareTo( path2 );
            } )
                .orElse( null );

            if( method == null ) {
                log.trace( "[{}] not found", request.requestLine );
                response.respond( NOT_FOUND );
            } else {
                Name name = Metrics
                    .name( "rest_timer" )
                    .tag( "service", service() )
                    .tag( "method", method.name() );

                if( !sessionAware ) {
                    handleInternal( request, response, method, name, null );
                } else {
                    String cookieId = request.cookie( SessionManager.COOKIE_ID ).orElse( null );
                    val authToken = Interceptor.getSessionToken( request );
                    Session session;
                    if( cookieId != null
                        && ( session = sessionManager.getSessionById( cookieId ) ) != null
                        && Objects.equals( authToken, session.get( Interceptor.AUTHORIZATION ).orElse( null ) )
                    ) {
                        log.debug( "{}: Valid SID [{}] found in cookie", service(), cookieId );

                        handleInternal( request, response, method, name, __( cookieId, session ) );
                    } else {
                        cookieId = UUID.randomUUID().toString();

                        log.debug( "{}: Creating new session with SID [{}]", service(), cookieId );

                        session = new Session();
                        if( authToken != null ) session.set( Interceptor.AUTHORIZATION, authToken );
                        sessionManager.put( cookieId, session );

                        handleInternal( request, response, method, name, __( cookieId, session ) );
                    }
                }
            }
        } catch( Throwable e ) {
            wsError( response, e );
        }
    }

    public String service() {
        return instance.getClass().getSimpleName();
    }

    private void handleInternal( Request request, Response response, Reflection.Method method,
                                 Name name, Pair<String, Session> session ) {
        log.trace( "{}: Internal session status: [{}]", service(), session );

        Optional<WsMethod> wsMethod = method.findAnnotation( WsMethod.class );

        Function<Reflection.Parameter, Object> func = p -> {
            val ret = getValue( session, request, wsMethod, p ).orElse( Optional.empty() );
            if( ret instanceof Optional ) return ( ( Optional<?> ) ret ).orElse( null );

            return ret;
        };

        HttpResponse interceptorResponse = session != null
            ? runInterceptors( request, session._2, method, func )
            : null;

        if( interceptorResponse != null ) response.respond( interceptorResponse );
        else Metrics.measureTimer( name, () -> {
            List<Reflection.Parameter> parameters = method.parameters;
            LinkedHashMap<Reflection.Parameter, Object> originalValues = getOriginalValues( session, parameters, request, wsMethod );

            ValidationErrors paramValidation = ValidationErrors.empty()
                .validateParameters( originalValues, method, instance, true )
                .throwIfInvalid();

            Validators.forMethod( method, instance, true )
                .validate( originalValues.values().toArray( new Object[originalValues.size()] ), originalValues )
                .throwIfInvalid();

            LinkedHashMap<Reflection.Parameter, Object> values = getValues( originalValues );

            paramValidation
                .validateParameters( values, method, instance, false )
                .throwIfInvalid();

            Object[] paramValues = values.values().toArray( new Object[values.size()] );

            Validators.forMethod( method, instance, false )
                .validate( paramValues, values )
                .throwIfInvalid();

            Object result = method.invoke( instance, paramValues );

            Boolean isRaw = wsMethod.map( WsMethod::raw ).orElse( false );
            ContentType produces =
                wsMethod.map( wsm -> ContentType.create( wsm.produces() )
                    .withCharset( UTF_8 ) )
                    .orElse( APPLICATION_JSON );

            String cookie = session != null
                ? new HttpResponse.CookieBuilder()
                .withSID( session._1 )
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
                        .map( r -> HttpResponse.ok( runPostInterceptors( r, session, method ), isRaw, produces )
                            .withCookie( cookie ) )
                        .orElse( NOT_FOUND )
                );
            } else if( result instanceof Result<?, ?> ) {
                Result<HttpResponse, HttpResponse> resp = ( ( Result<?, ?> ) result )
                    .mapSuccess( r -> HttpResponse.ok( r, isRaw, produces ).withCookie( cookie ) )
                    .mapFailure( r -> HttpResponse.status( HTTP_INTERNAL_ERROR, "", r )
                        .withCookie( cookie ) );

                response.respond( resp.isSuccess() ? ( ( Result<?, ?> ) result )
                    .mapSuccess( r -> HttpResponse.ok( runPostInterceptors( r, session, method ), isRaw, produces )
                        .withCookie( cookie ) ).successValue
                    : ( ( Result<?, ?> ) result )
                        .mapFailure( r -> HttpResponse.status( HTTP_INTERNAL_ERROR, "", r ).withCookie( cookie ) )
                        .failureValue );

            } else if( result instanceof Stream<?> ) {
                response.respond(
                    HttpResponse.stream( ( ( Stream<?> ) result ).map( v -> runPostInterceptors( v, session, method ) ), isRaw, produces )
                        .withCookie( cookie ) );
            } else
                response.respond( HttpResponse.ok( runPostInterceptors( result, session, method ), isRaw, produces )
                    .withCookie( cookie ) );
        } );
    }

    private Object runPostInterceptors( Object value, Pair<String, Session> session, Reflection.Method method ) {
        if( session == null ) return value;
        Object result = value;
        for( Interceptor interceptor : interceptors ) result = interceptor.postProcessing( value, session._2, method );

        return result;
    }

    private LinkedHashMap<Reflection.Parameter, Object> getValues( LinkedHashMap<Reflection.Parameter, Object> values ) {
        try {
            val res = new LinkedHashMap<Reflection.Parameter, Object>();

            values.forEach( ( key, value ) -> {
                Object map = map( key.type(), value );
                res.put( key, map );
            } );

            return res;
        } catch( JsonException e ) {
            throw new WsClientException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    private Object map( Reflection reflection, Object value ) {
        if( reflection.isOptional() ) {
            if( !( ( Optional ) value ).isPresent() ) return Optional.empty();
            else
                return Optional.ofNullable( map( reflection.typeParameters.get( 0 ), ( ( Optional ) value ).get() ) );
        } else {
            if( value instanceof Optional ) return map( reflection, ( ( Optional ) value ).get() );
            if( reflection.isEnum() ) return Enum.valueOf( ( Class<Enum> ) reflection.underlying, ( String ) value );

            // what is this for? I sincerelly hope there is a test for it.
            if( !( value instanceof String ) && Collection.class.isAssignableFrom( reflection.underlying ) )
                value = Binder.json.marshal( value );
            if( reflection.underlying.isInstance( value ) ) return value;

            return Binder.json.unmarshal( reflection, ( String ) value );
        }
    }

    private HttpResponse runInterceptors( Request request, Session session, Reflection.Method method,
                                          Function<Reflection.Parameter, Object> getParameterValueFunc ) {

        for( Interceptor interceptor : interceptors ) {
            val interceptorResponse = interceptor.intercept( request, session, method, getParameterValueFunc );
            if( interceptorResponse.isPresent() ) return interceptorResponse.get();
        }

        return null;
    }

    @Override
    public String toString() {
        return instance.getClass().getName();
    }

    private Object unwrap( Reflection.Parameter parameter, Optional<?> opt ) {
        if( parameter.type().isOptional() ) return opt;

        return opt.orElseThrow( () -> new WsClientException( parameter.name() + " is required" ) );
    }

    public LinkedHashMap<Reflection.Parameter, Object> getOriginalValues( Pair<String, Session> session,
                                                                          List<Reflection.Parameter> parameters,
                                                                          Request request,
                                                                          Optional<WsMethod> wsMethod ) {

        return parameters.stream().collect( toLinkedHashMap(
            parameter -> parameter,
            parameter -> getValue( session, request, wsMethod, parameter )
                .orElseGet( () -> parameter.type().assignableTo( List.class )
                    ? request.parameters( parameter.name() )
                    : unwrap( parameter, request.parameter( parameter.name() ) )
                ) ) );
    }

    public Optional<Object> getValue(
        Pair<String, Session> session,
        Request request,
        Optional<WsMethod> wsMethod,
        Reflection.Parameter parameter ) {
        return parameter.findAnnotation( WsParam.class )
            .map( wsParam -> {
                switch( wsParam.from() ) {
                    case REQUEST:
                        return request;
                    case SESSION:
                        if( session == null ) return null;
                        return parameter.type().isOptional()
                            ? session._2.get( parameter.name() )
                            : session._2.get( parameter.name() ).orElse( null );
                    case HEADER:
                        return unwrap( parameter, request.header( parameter.name() ) );
                    case PATH:
                        return wsMethod.map( wsm -> WsServices.pathParam( wsm.path(), request.requestLine,
                            parameter.name() ) )
                            .orElseThrow( () -> new WsException(
                                "path parameter " + parameter.name() + " without "
                                    + WsMethod.class.getName() + " annotation" ) );
                    case BODY:
                        return parameter.type().assignableFrom( byte[].class )
                            ? ( parameter.type().isOptional() ? request.readBody()
                            : request.readBody()
                                .orElseThrow( () -> new WsClientException(
                                    "no body for " + parameter.name() ) )
                        ) : unwrap( parameter, request.readBody().map( String::new ) );
                    default:
                        return parameter.type().assignableTo( List.class )
                            ? request.parameters( parameter.name() )
                            : unwrap( parameter, request.parameter( parameter.name() ) );

                }
            } );
    }


    private static class JsonErrorResponse implements Serializable {
        private static long serialVersionUID = 4949051855248389697L;
        public List<String> errors;

        public JsonErrorResponse( List<String> errors ) {
            this.errors = errors;
        }
    }

    private static class JsonStackTraceResponse implements Serializable {
        private static long serialVersionUID = 8431608226448804296L;

        public String message;

        public JsonStackTraceResponse( Throwable t ) {
            message = Throwables.getRootCause( t ).getMessage();
        }
    }
}
