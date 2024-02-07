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

package oap.ws.openapi;

import lombok.extern.slf4j.Slf4j;
import oap.application.module.Module;
import oap.ws.WsConfig;

import java.net.URL;
import java.util.List;

@Slf4j
public class WebServicesWalker {

    public static void walk( WebServiceVisitor visitor ) {
        List<URL> urls = visitor.getWebServiceUrls();
        for( URL url : urls ) {
            log.info( "Reading config from " + url.getPath() );
            Module config = Module.CONFIGURATION.fromUrl( url );
            config.services.forEach( ( name, service ) -> {
                log.info( String.format( "Service %s", name ) );
                WsConfig wsService = ( WsConfig ) service.ext.get( "ws-service" );
                if( wsService == null ) {
                    log.debug( "Skipping bean: " + name + " as it's not a WS" );
                    return;
                }
                log.debug( "WS bean: " + name + " implementing " + service.implementation );
                try {
                    Class<?> clazz = visitor.loadClass( service );
                    String basePath = wsService.path.stream().findFirst().orElse( "" );
                    visitor.visit( wsService, clazz, basePath );
                } catch( Exception e ) {
                    log.warn( "Could not deal with module: " + name + " due to the implementation class '"
                        + service.implementation + "' is unavailable", e );
                }
            } );
        }
    }
}
