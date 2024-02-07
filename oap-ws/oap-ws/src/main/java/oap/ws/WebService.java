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

import io.undertow.server.handlers.Cookie;
import lombok.extern.slf4j.Slf4j;
import oap.http.Http;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.reflect.ReflectException;
import oap.reflect.Reflection;
import oap.util.Result;
import oap.util.Throwables;
import oap.ws.interceptor.Interceptor;
import oap.ws.interceptor.Interceptors;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.Validators;
import org.joda.time.DateTime;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class WebService implements HttpHandler {
    public final boolean compressionSupport;
    private final boolean sessionAware;
    private final SessionManager sessionManager;
    private final List<Interceptor> interceptors;
    private final Object instance;
    private final WsMethodMatcher methodMatcher;

    public WebService( Object instance, boolean sessionAware,
                       SessionManager sessionManager, List<Interceptor> interceptors, boolean compressionSupport ) {
        this.instance = instance;
        this.methodMatcher = new WsMethodMatcher( instance.getClass() );
        this.sessionAware = sessionAware;
        this.sessionManager = sessionManager;
        this.interceptors = interceptors;
        this.compressionSupport = compressionSupport;
    }

    private void wsError( HttpServerExchange exchange, Throwable e ) {
        if( e instanceof ReflectException && e.getCause() != null )
            wsError( exchange, e.getCause() );
        else if( e instanceof InvocationTargetException itException )
            wsError( exchange, itException.getTargetException() );
        else if( e instanceof WsClientException clientException ) {
            log.debug( this + ": " + clientException, clientException );
            if( !exchange.isResponseStarted() ) {
                exchange.setStatusCodeReasonPhrase( clientException.code, e.getMessage() );
                if( !clientException.errors.isEmpty() )
                    exchange.responseJson( new ValidationErrors.ErrorResponse( clientException.errors ) );
            }
        } else {
            log.error( this + ": " + e.toString(), e );
            if( !exchange.isResponseStarted() )
                exchange.responseJson( Http.StatusCode.INTERNAL_SERVER_ERROR, e.getMessage(), new JsonStackTraceResponse( e ) );
        }
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) {
        try {
            var requestLine = exchange.getRelativePath();
            var method = methodMatcher.findMethod( requestLine, exchange.getRequestMethod() ).orElse( null );
            log.trace( "invoking {} for {}", method, requestLine );
            if( method != null ) {
                Session session = null;
                if( sessionAware ) {
                    String cookie = exchange.getRequestCookieValue( SessionManager.COOKIE_ID );
                    log.trace( "session cookie {}", cookie );
                    session = sessionManager.getOrInit( cookie );
                    log.trace( "session for {} is {}", this, session );
                }

                handleInternal( new InvocationContext( exchange, session, method ) );
            } else {
                log.trace( "[{}] not found", requestLine );
                exchange.responseNotFound();
                exchange.endExchange();
            }

        } catch( Throwable e ) {
            log.trace( e.getMessage(), e );
            wsError( exchange, e );
        }
    }

    private void buildErrorResponse( HttpServerExchange exchange, ValidationErrors validationErrors ) {
        exchange.responseJson( validationErrors.code, "validation failed", new ValidationErrors.ErrorResponse( validationErrors.errors ) );
    }


    private void handleInternal( InvocationContext context ) {
        log.trace( "{}: session: [{}]", this, context.session );

        var wsMethod = context.method.findAnnotation( WsMethod.class );

        Interceptors.before( interceptors, context )
            .ifPresentOrElse(
                response -> response.send( context.exchange ),
                () -> {
                    var unparsedParameters = context.unparsedParameters();

                    ValidationErrors validationErrors = ValidationErrors.empty().validateParameters( unparsedParameters, context.method, instance, true );

                    if( !validationErrors.isEmpty() ) {
                        buildErrorResponse( context.exchange, validationErrors );
                        return;
                    }

                    validationErrors = Validators.forMethod( context.method, instance, true )
                        .validate( unparsedParameters.values().toArray( new Object[0] ), unparsedParameters );

                    if( !validationErrors.isEmpty() ) {
                        buildErrorResponse( context.exchange, validationErrors );
                        return;
                    }

                    Map<Reflection.Parameter, Object> values = context.parsedParameters();

                    validationErrors = ValidationErrors.empty()
                        .validateParameters( values, context.method, instance, false );

                    if( !validationErrors.isEmpty() ) {
                        buildErrorResponse( context.exchange, validationErrors );
                        return;
                    }

                    var paramValues = values.values().toArray( new Object[0] );
                    validationErrors = Validators
                        .forMethod( context.method, instance, false )
                        .validate( paramValues, values );

                    if( !validationErrors.isEmpty() ) {
                        buildErrorResponse( context.exchange, validationErrors );
                        return;
                    }

                    if( context.session != null && !containsSessionCookie( context.exchange.responseCookies() ) ) {
                        var cookie = new oap.http.Cookie( SessionManager.COOKIE_ID, context.session.id )
                            .withPath( sessionManager.cookiePath )
                            .withExpires( DateTime.now().plus( sessionManager.cookieExpiration ) )
                            .withDomain( sessionManager.cookieDomain )
                            .secure( sessionManager.cookieSecure )
                            .httpOnly( true );

                        context.exchange.setResponseCookie( cookie );
                    }

                    var response = produceResultResponse( context.method, wsMethod, context.method.invoke( instance, paramValues ) );

                    Interceptors.after( interceptors, response, context );

                    response.send( context.exchange );
                } );
    }

    private boolean containsSessionCookie( Iterable<Cookie> cookies ) {
        for( var p : cookies ) {
            if( SessionManager.COOKIE_ID.equals( p.getName() ) ) return true;
        }
        return false;
    }

    private Response produceResultResponse( Reflection.Method method, Optional<WsMethod> wsMethod, Object result ) {
        boolean isRaw = wsMethod.map( WsMethod::raw ).orElse( false );
        var produces = wsMethod.map( WsMethod::produces )
            .orElse( Http.ContentType.APPLICATION_JSON );

        if( method.isVoid() ) return Response.noContent();
        else if( result instanceof Response response ) return response;
        else if( result instanceof Optional<?> optResult ) return optResult.isEmpty()
            ? Response.notFound()
            : Response.ok().withBody( optResult.get(), isRaw ).withContentType( produces );
        else if( result instanceof Result<?, ?> resultResult ) if( resultResult.isSuccess() )
            return Response.ok().withBody( resultResult.successValue, isRaw ).withContentType( produces );
        else return new Response( Http.StatusCode.INTERNAL_SERVER_ERROR, "" )
                .withBody( resultResult.failureValue, false )
                .withContentType( Http.ContentType.APPLICATION_JSON );
        else if( result instanceof java.util.stream.Stream<?> stream )
            return Response.ok().withBody( stream, isRaw ).withContentType( produces );
        else return Response.ok().withBody( result, isRaw ).withContentType( produces );
    }

    @Override
    public String toString() {
        return instance.getClass().getName();
    }


    private static class JsonStackTraceResponse implements Serializable {
        @Serial
        private static final long serialVersionUID = 8431608226448804296L;

        public final String message;

        private JsonStackTraceResponse( Throwable t ) {
            message = Throwables.getRootCause( t ).getMessage();
        }
    }
}
