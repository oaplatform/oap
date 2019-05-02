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

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.application.Kernel;
import oap.application.KernelHelper;
import oap.http.HttpResponse;
import oap.http.HttpServer;
import oap.http.Protocol;
import oap.http.cors.CorsPolicy;
import oap.json.Binder;
import oap.util.Lists;
import oap.util.Stream;
import org.apache.http.entity.ContentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WebServices {
    static {
        HttpResponse.registerProducer( ContentType.APPLICATION_JSON.getMimeType(), Binder.json::marshal );
    }

    final HashMap<String, Integer> exceptionToHttpCode = new HashMap<>();
    private final List<WsConfig> wsConfigs;
    private final HttpServer server;
    private final SessionManager sessionManager;
    private final CorsPolicy globalCorsPolicy;
    private final Kernel kernel;
    //todo handle this case better
    public WsResponse defaultResponse = WsResponse.TEXT;

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy ) {
        this( kernel, server, sessionManager, globalCorsPolicy, WsConfig.CONFIGURATION.fromClassPath() );
    }

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy, WsConfig... wsConfigs ) {
        this( kernel, server, sessionManager, globalCorsPolicy, Lists.of( wsConfigs ) );
    }

    public WebServices( Kernel kernel, HttpServer server, SessionManager sessionManager, CorsPolicy globalCorsPolicy, List<WsConfig> wsConfigs ) {
        this.kernel = kernel;
        this.wsConfigs = wsConfigs;
        this.server = server;
        this.sessionManager = sessionManager;
        this.globalCorsPolicy = globalCorsPolicy;
    }

    public void start() {
        log.info( "binding web services..." );

        for( WsConfig config : wsConfigs ) {
            log.trace( "config = {}", config );

            final List<Interceptor> interceptors = Stream.of( config.interceptors )
                .map( kernel::service )
                .map( Interceptor.class::cast )
                .toList();

            for( Map.Entry<String, WsConfig.Service> entry : config.services.entrySet() ) {
                final WsConfig.Service serviceConfig = entry.getValue();

                log.trace( "service = {}", entry );

                if( !serviceConfig.profiles.isEmpty() && !KernelHelper.profileEnabled( serviceConfig.profiles, kernel.profiles ) ) {
                    log.debug( "skipping " + entry.getKey() + " web service initialization with "
                        + "service profiles " + serviceConfig.profiles );
                    continue;
                }

                final Object service = kernel.service( serviceConfig.service );

                Preconditions.checkState( service != null, "Unknown service " + serviceConfig.service );

                CorsPolicy corsPolicy = serviceConfig.corsPolicy != null ? serviceConfig.corsPolicy : globalCorsPolicy;
                bind( entry.getKey(), corsPolicy, service, serviceConfig.sessionAware,
                    sessionManager, interceptors, serviceConfig.protocol );
            }

            for( Map.Entry<String, WsConfig.Service> entry : config.handlers.entrySet() ) {
                final WsConfig.Service handlerConfig = entry.getValue();
                log.trace( "handler = {}", entry );

                CorsPolicy corsPolicy = handlerConfig.corsPolicy != null ? handlerConfig.corsPolicy : globalCorsPolicy;

                server.bind( entry.getKey(), corsPolicy, kernel.service( handlerConfig.service ), handlerConfig.protocol );
            }
        }
    }

    public void stop() {
        for( WsConfig config : wsConfigs ) {
            config.handlers.keySet().forEach( server::unbind );
            config.services.keySet().forEach( server::unbind );
        }
    }

    public void bind( String context, CorsPolicy corsPolicy, Object impl, boolean sessionAware, SessionManager sessionManager,
                      List<Interceptor> interceptors, Protocol protocol ) {
        server.bind( context, corsPolicy,
            new WebService( impl, sessionAware, sessionManager, interceptors, defaultResponse, exceptionToHttpCode ),
            protocol );
    }

}
