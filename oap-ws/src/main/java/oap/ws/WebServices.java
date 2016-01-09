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

import oap.application.Application;
import oap.http.Cors;
import oap.http.HttpResponse;
import oap.http.HttpServer;
import oap.json.Binder;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class WebServices {
    private static Logger logger = getLogger( WebServices.class );

    static {
        HttpResponse.registerProducer( ContentType.APPLICATION_JSON.getMimeType(), Binder.json::marshal );
    }

    private final List<WsConfig> wsConfigs;
    private final HttpServer server;

    public WebServices( HttpServer server ) {
        this( server, WsConfig.fromClassPath() );
    }

    public WebServices( HttpServer server, List<WsConfig> wsConfigs ) {
        this.wsConfigs = wsConfigs;
        this.server = server;
    }


    public void start() {
        logger.info( "binding web services..." );

        for( WsConfig config : wsConfigs ) {
            for( Map.Entry<String, WsConfig.Service> entry : config.services.entrySet() ) {
                final WsConfig.Service value = entry.getValue();
                bind( entry.getKey(), value.cors, Application.service( value.service ), value.localHostOnly );
            }
            for( Map.Entry<String, WsConfig.Service> entry : config.handlers.entrySet() ) {
                final WsConfig.Service value = entry.getValue();
                server.bind( entry.getKey(),value.cors, Application.service( value.service ), value.localHostOnly );
            }
        }
    }

    public void stop() {
        for( WsConfig config : wsConfigs ) {
            for( String context : config.handlers.keySet() ) server.unbind( context );
            for( String context : config.services.keySet() ) server.unbind( context );
        }

    }

    public void bind( String context, Cors cors, Object impl, boolean localHostOnly ) {
        server.bind( context, cors, new Service( impl ), localHostOnly );
    }

}
