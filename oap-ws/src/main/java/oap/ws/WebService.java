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
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.http.Session;
import oap.json.Binder;
import oap.json.JsonException;
import oap.metrics.Metrics;
import oap.metrics.Metrics2;
import oap.metrics.Name;
import oap.reflect.Reflect;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Result;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.Throwables;
import oap.ws.interceptor.Interceptor;
import oap.ws.interceptor.Interceptors;
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
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.http.HttpResponse.NOT_FOUND;
import static oap.util.Collectors.toLinkedHashMap;
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
            var clientException = ( WsClientException ) e;
            log.debug( this + ": " + e.toString(), e );
            var builder = HttpResponse.status( clientException.code, e.getMessage() );
            if( !clientException.errors.isEmpty() )
                if( defaultResponse == TEXT )
                    builder.withContent( String.join( "\n", clientException.errors ), TEXT_PLAIN );
                else
                    builder.withContent( Binder.json.marshal( new JsonErrorResponse( clientException.errors ) ), APPLICATION_JSON );
            response.respond( builder.response() );
        } else {
            log.error( this + ": " + e.toString(), e );

            var code = exceptionToHttpCode.getOrDefault( e.getClass(), HTTP_INTERNAL_ERROR );

            var builder = HttpResponse.status( code, e.getMessage() );
            if( defaultResponse == TEXT ) builder.withContent( Throwables.getRootCause( e ).getMessage(), TEXT_PLAIN );
            else builder.withContent( Binder.json.marshal( new JsonStackTraceResponse( e ) ), APPLICATION_JSON );

            response.respond( builder.response() );
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
            var method = reflection.method( m -> methodMatches( request.getRequestLine(), request.getHttpMethod(), m ), ( o1, o2 ) -> {
                var path1 = o1.findAnnotation( WsMethod.class ).map( WsMethod::path ).orElse( o1.name() );
                var path2 = o2.findAnnotation( WsMethod.class ).map( WsMethod::path ).orElse( o1.name() );

                return path1.compareTo( path2 );
            } )
                .orElse( null );

            if( method == null ) {
                if( log.isTraceEnabled() )
                    log.trace( "[{}] not found", request.getRequestLine() );
                response.respond( NOT_FOUND );
            } else {
                var name = Metrics
                    .name( "rest_timer" )
                    .tag( "service", toString() )
                    .tag( "method", method.name() );

                Session session = null;
                if( sessionAware ) {
                    session = sessionManager.getOrInit( request.cookie( SessionManager.COOKIE_ID ).orElse( null ) );
                    log.trace( "session for {} is {}", this, session );
                }

                handleInternal( request, response, method, name, session );

//                if( !sessionAware ) handleInternal( request, response, method, name, null );
//                else {
//
//                    var cookieId = request.cookie( SessionManager.COOKIE_ID ).orElse( null );

//                    var authToken = SecurityInterceptor.getSessionToken( request );
//                    Session session;
//                    if( cookieId != null
//                        && ( session = sessionManager.get( cookieId ) ) != null
//                        && Objects.equals( authToken, session.get( SecurityInterceptor.AUTHORIZATION ).orElse( null ) )
//                    ) {
//                        log.debug( "{}: Valid SID [{}] found in cookie", service(), cookieId );
//
//                        handleInternal( request, response, method, name, __( cookieId, session ) );
//                    } else {
//                        cookieId = UUID.randomUUID().toString();
//
//                        log.debug( "{}: Creating new session with SID [{}]", service(), cookieId );
//
//                        session = new Session();
//                        if( authToken != null ) session.set( SecurityInterceptor.AUTHORIZATION, authToken );
//                        sessionManager.put( cookieId, session );
//
//                        handleInternal( request, response, method, name, __( cookieId, session ) );
//                    }
//                }
            }
        } catch( Throwable e ) {
            wsError( response, e );
        }
    }

    private void handleInternal( Request request, Response response, Reflection.Method method,
                                 Name name, Session session ) {
        log.trace( "{}: session: [{}]", this, session );

        var wsMethod = method.findAnnotation( WsMethod.class );

        Function<Reflection.Parameter, Object> func = p -> {
            var ret = getValue( session, request, wsMethod, p ).orElse( Optional.empty() );
            if( ret instanceof Optional ) return ( ( Optional<?> ) ret ).orElse( null );

            return ret;
        };

        Interceptors.before( interceptors, request, session, method )
            .ifPresentOrElse( response::respond, () -> Metrics2.measureTimer( name, () -> {
                var parameters = method.parameters;
                var originalValues = getOriginalValues( session, parameters, request, wsMethod );

                var paramValidation = ValidationErrors.empty()
                    .validateParameters( originalValues, method, instance, true )
                    .throwIfInvalid();

                Validators.forMethod( method, instance, true )
                    .validate( originalValues.values().toArray( new Object[0] ), originalValues )
                    .throwIfInvalid();

                var values = getValues( originalValues );

                paramValidation
                    .validateParameters( values, method, instance, false )
                    .throwIfInvalid();

                var paramValues = values.values().toArray( new Object[0] );

                Validators.forMethod( method, instance, false )
                    .validate( paramValues, values )
                    .throwIfInvalid();

                var result = method.invoke( instance, paramValues );

                var isRaw = wsMethod.map( WsMethod::raw ).orElse( false );
                var produces =
                    wsMethod.map( wsm -> ContentType.create( wsm.produces() )
                        .withCharset( UTF_8 ) )
                        .orElse( APPLICATION_JSON );

                var cookie = session != null
                    ? new HttpResponse.CookieBuilder()
                    .withValue( SessionManager.COOKIE_ID, session.id )
                    .withPath( sessionManager.cookiePath )
                    .withExpires( DateTime.now().plusMinutes( sessionManager.cookieExpiration ) )
                    .withDomain( sessionManager.cookieDomain )
                    .build()
                    : null;


                HttpResponse.Builder responseBuilder;
                if( method.isVoid() )
                    responseBuilder = HttpResponse.status( HTTP_NO_CONTENT );
                else if( result instanceof HttpResponse ) {
                    HttpResponse r = ( HttpResponse ) result;
                    if( session != null && !r.session.isEmpty() )
                        session.setAll( r.session );
                    responseBuilder = r.modify();
                } else if( result instanceof Optional<?> )
                    responseBuilder = ( ( Optional<?> ) result )
                        .map( r -> HttpResponse.ok( r, isRaw, produces ) )
                        .orElse( NOT_FOUND.modify() );
                else if( result instanceof Result<?, ?> )
                    responseBuilder = ( ( Result<?, ?> ) result ).isSuccess()
                        ? ( ( Result<?, ?> ) result ).mapSuccess( r -> HttpResponse.ok( r, isRaw, produces ) ).successValue
                        : ( ( Result<?, ?> ) result ).mapFailure( r -> HttpResponse.status( HTTP_INTERNAL_ERROR, "", r ) ).failureValue;
                else if( result instanceof Stream<?> )
                    responseBuilder = HttpResponse.stream( ( ( Stream<?> ) result ), isRaw, produces );
                else responseBuilder = HttpResponse.ok( result, isRaw, produces );

                response.respond( Interceptors.after( interceptors, responseBuilder.withCookie( cookie ).response(), session ) );
            } ) );
    }

    private LinkedHashMap<Reflection.Parameter, Object> getValues( LinkedHashMap<Reflection.Parameter, Object> values ) {
        try {
            var res = new LinkedHashMap<Reflection.Parameter, Object>();

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
        if( reflection.isOptional() )
            if( ( ( Optional ) value ).isEmpty() ) return Optional.empty();
            else return Optional.ofNullable(
                map( reflection.typeParameters.get( 0 ), ( ( Optional ) value ).get() )
            );
        else {
            if( value instanceof Optional ) return map( reflection, ( ( Optional ) value ).get() );
            if( reflection.isEnum() ) return Enum.valueOf( ( Class<Enum> ) reflection.underlying, ( String ) value );

            // what is this for? I sincerelly hope there is a test for it.
            if( !( value instanceof String ) && Collection.class.isAssignableFrom( reflection.underlying ) )
                value = Binder.json.marshal( value );
            if( reflection.underlying.isInstance( value ) ) return value;

            return Binder.json.unmarshal( reflection, ( String ) value );
        }
    }

    private Optional<HttpResponse> runInterceptors( Request request, Session session, Reflection.Method method ) {
        for( var interceptor : interceptors ) {
            var response = interceptor.before( request, session, method );
            if( response.isPresent() ) return response;
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return instance.getClass().getName();
    }

    private Object unwrap( Reflection.Parameter parameter, Optional<?> opt ) {
        if( parameter.type().isOptional() ) return opt;

        return opt.orElseThrow( () -> new WsClientException( parameter.name() + " is required" ) );
    }

    public LinkedHashMap<Reflection.Parameter, Object> getOriginalValues( Session session,
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
        Session session,
        Request request,
        Optional<WsMethod> wsMethod,
        Reflection.Parameter parameter ) {

        return
            parameter.type().assignableFrom( Request.class )
                ? Optional.of( request )
                : parameter.type().assignableFrom( Session.class )
                    ? Optional.ofNullable( session )
                    : parameter.findAnnotation( WsParam.class )
                        .map( wsParam -> {
                            switch( wsParam.from() ) {
                                case SESSION:
                                    if( session == null ) return null;
                                    return parameter.type().isOptional()
                                        ? session.get( parameter.name() )
                                        : session.get( parameter.name() ).orElse( null );
                                case HEADER:
                                    return unwrap( parameter, request.header( parameter.name() ) );
                                case PATH:
                                    return wsMethod.map( wsm -> WsServices.pathParam( wsm.path(), request.getRequestLine(),
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
