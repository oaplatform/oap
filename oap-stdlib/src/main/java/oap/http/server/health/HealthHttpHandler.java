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

package oap.http.server.health;

import lombok.extern.slf4j.Slf4j;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.http.Response;
import oap.http.server.Handler;
import oap.util.Collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static oap.http.HttpResponse.NO_CONTENT;

@Slf4j
public class HealthHttpHandler implements Handler {
    private final List<HealthDataProvider<?>> providers = new ArrayList<>();
    private final String secret;

    public HealthHttpHandler( String secret ) {
        this.secret = secret;
    }

    public HealthHttpHandler() {
        this( null );
    }

    @Override
    public void handle( Request request, Response response ) {
        log.trace( "providers: {}", providers );
        if( secret != null && request.parameter( "secret" ).map( s -> Objects.equals( s, secret ) ).orElse( false ) )
            response
                .respond( HttpResponse.ok( Collections.toLinkedHashMap( providers, HealthDataProvider::name, HealthDataProvider::data ) )
                    .response() );
        else response.respond( NO_CONTENT );
    }

    public void addProvider( HealthDataProvider<?> provider ) {
        this.providers.add( provider );
    }
}
