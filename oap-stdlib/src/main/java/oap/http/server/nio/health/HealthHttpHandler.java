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

package oap.http.server.nio.health;

import lombok.extern.slf4j.Slf4j;
import oap.http.ContentTypes;
import oap.http.server.nio.HttpHandler;
import oap.http.server.nio.HttpServerExchange;
import oap.http.server.nio.NioHttpServer;
import oap.json.Binder;
import oap.util.Collections;
import oap.util.Lists;

import java.util.ArrayList;

@Slf4j
public class HealthHttpHandler implements HttpHandler {
    private final ArrayList<HealthDataProvider<?>> providers = new ArrayList<>();
    private final String secret;
    private final String prefix;

    public HealthHttpHandler( NioHttpServer server, String prefix, int port, String secret ) {
        this.secret = secret;
        this.prefix = prefix;
        server.bind( prefix, this, port );
    }

    public HealthHttpHandler( NioHttpServer server, String prefix, String secret ) {
        this.secret = secret;
        this.prefix = prefix;
        server.bind( prefix, this );
    }

    public HealthHttpHandler( NioHttpServer server, String prefix ) {
        this( server, prefix, null );
    }

    public void addProvider( HealthDataProvider<?> provider ) {
        this.providers.add( provider );
    }

    public void start() {
        log.debug( "prefix '{}' providers {}", prefix, Lists.map( providers, HealthDataProvider::name ) );
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        if( secret != null && secret.equals( exchange.getStringParameter( "secret" ) ) )
            exchange.responseOk( Binder.json.marshal( Collections.toLinkedHashMap( providers, HealthDataProvider::name, HealthDataProvider::data ) ), ContentTypes.APPLICATION_JSON );
        else exchange.responseNoContent();
    }
}
