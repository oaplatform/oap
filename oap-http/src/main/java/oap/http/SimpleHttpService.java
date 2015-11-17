/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Toğrul Məhərrəmov cmeisters@gmail.com
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
package oap.http;

import oap.util.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

public class SimpleHttpService {

    public static <T> T execute(Object request, String url, Class<? extends HttpRequestBase> httpMethod,
                               Function<Object, Pair<String, Object>[]> queryMapper,
                               Function<Object, HttpEntity> entityMapper,
                               Function<SimpleHttpClient.Response, T> responseMapper){
        T response = null;
        try {
            URI uri = Uri.uri(url, queryMapper.apply( request ) );
            final HttpUriRequest httpRequest = httpMethod.getConstructor( URI.class ).newInstance( uri );
            Optional.ofNullable( entityMapper.apply( request ) ).ifPresent( e -> ( ( HttpEntityEnclosingRequestBase ) httpRequest ).setEntity( e ) );
            response = responseMapper.apply( SimpleHttpClient.execute( httpRequest ) );

        } catch( ReflectiveOperationException e ) {
            new RuntimeException( e );
        }

        return response;
    }

}
