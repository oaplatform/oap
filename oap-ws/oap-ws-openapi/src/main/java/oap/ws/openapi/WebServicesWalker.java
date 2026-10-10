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
import oap.application.module.Service;
import oap.ws.WsConfig;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WebServicesWalker {

    public static void walk( WebServiceVisitor visitor ) {
        List<Module> modules = new ArrayList<>();
        for( URL url : visitor.getWebServiceUrls() ) {
            log.info( "Reading config from " + url.getPath() );
            modules.add( Module.CONFIGURATION.fromUrl( url ) );
        }

        Map<String, Service> index = serviceIndex( modules );
        for( Module config : modules ) {
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
                    visitor.visit( wsService, clazz, basePath, interceptorClasses( visitor, wsService, index ) );
                } catch( Exception e ) {
                    log.warn( "Could not deal with module: " + name + " due to the implementation class '"
                        + service.implementation + "' is unavailable", e );
                }
            } );
        }
    }

    /**
     * Services by {@code module.service} and by plain service name (first module wins for plain names).
     */
    private static Map<String, Service> serviceIndex( List<Module> modules ) {
        Map<String, Service> index = new HashMap<>();
        for( Module module : modules ) {
            module.services.forEach( ( name, service ) -> {
                index.putIfAbsent( name, service );
                if( module.name != null ) index.put( module.name + "." + name, service );
            } );
        }
        return index;
    }

    private static List<Class<?>> interceptorClasses( WebServiceVisitor visitor, WsConfig wsService, Map<String, Service> index ) {
        List<Class<?>> classes = new ArrayList<>();
        for( String reference : wsService.interceptors ) {
            String name = reference.replaceAll( "^<modules\\.|>$", "" );
            Service interceptor = index.get( name );
            if( interceptor == null ) {
                log.warn( "Interceptor '{}' not found in any module config, skipped", reference );
                continue;
            }
            try {
                classes.add( visitor.loadClass( interceptor ) );
            } catch( ClassNotFoundException e ) {
                log.warn( "Interceptor '{}' implementation '{}' is unavailable, skipped", reference, interceptor.implementation, e );
            }
        }
        return classes;
    }
}
