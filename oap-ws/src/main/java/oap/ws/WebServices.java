/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
import oap.ws.http.HttpServer;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class WebServices {
    private static Logger logger = getLogger( WebServices.class );
    private final List<WsConfig> wsConfigs;

    private HttpServer server;

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
            for( WsConfig.Service service : config.services )
                bind( service.context, Application.service( service.service ) );
            for( WsConfig.Service service : config.handlers )
                server.bind( service.context, Application.service( service.service ) );
        }
    }

    public void stop() {
        for( WsConfig config : wsConfigs ) {
            for( WsConfig.Service service : config.handlers ) server.unbind( service.context );
            for( WsConfig.Service service : config.services ) server.unbind( service.context );
        }

    }

    public void bind( String context, Object impl ) {
        server.bind( context, new Service( impl ) );
    }

}
