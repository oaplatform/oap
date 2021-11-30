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

package oap.http.server.undertow.health;

import lombok.extern.slf4j.Slf4j;
import oap.http.ContentTypes;
import oap.http.server.undertow.HttpHandler;
import oap.http.server.undertow.HttpServerExchange;
import oap.json.Binder;
import oap.util.Collections;

import java.util.ArrayList;

@Slf4j
public class HealthHttpHandler implements HttpHandler {
    private final ArrayList<HealthDataProvider<?>> providers = new ArrayList<>();
    private final String secret;

    public HealthHttpHandler( String secret ) {
        this.secret = secret;
    }

    public HealthHttpHandler() {
        this( null );
    }

    public void addProvider( HealthDataProvider<?> provider ) {
        this.providers.add( provider );
    }

    @Override
    public void handleRequest( HttpServerExchange exchange ) throws Exception {
        log.trace( "providers: {}", providers );
        if( secret != null && secret.equals( exchange.getStringParameter( "secret" ) ) )
            exchange.ok( Binder.json.marshal( Collections.toLinkedHashMap( providers, HealthDataProvider::name, HealthDataProvider::data ) ), ContentTypes.APPLICATION_JSON.getMimeType() );
        else exchange.responseNoContent();
    }
}
