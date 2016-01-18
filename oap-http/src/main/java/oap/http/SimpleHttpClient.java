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

package oap.http;

import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import oap.io.Closeables;
import oap.util.Result;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static oap.json.Binder.json;
import static oap.util.Maps.Collectors.toMap;
import static oap.util.Pair.__;

public final class SimpleHttpClient {
    private static CloseableHttpClient client = initialize();
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    private static CloseableHttpClient initialize() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute( 1000 );
        cm.setMaxTotal( 10000 );

        return HttpClients
            .custom()
            .setMaxConnPerRoute( 1000 )
            .setMaxConnTotal( 10000 )
            .setConnectionManager( cm )
            .setKeepAliveStrategy( DefaultConnectionKeepAliveStrategy.INSTANCE )
            .disableRedirectHandling()
            .setRetryHandler( new DefaultHttpRequestRetryHandler( 0, false ) )
            .build();
    }

    public static void reset() {
        Closeables.close( client );
        client = initialize();
    }

    public static Response execute( HttpUriRequest request ) throws IOException, TimeoutException {
        return execute( request, Long.MAX_VALUE );
    }

    public static Response execute( HttpUriRequest request, long timeout ) throws IOException, TimeoutException {
        try( CloseableHttpResponse response = executorService.submit( () -> client.execute( request ) ).get( timeout, TimeUnit.MILLISECONDS ) ) {
            final Map<String, String> headers = Arrays.stream( response.getAllHeaders() )
                .map( h -> __( h.getName(), h.getValue() ) )
                .collect( toMap() );

            if( response.getEntity() != null ) {
                HttpEntity entity = response.getEntity();
                try( InputStream is = entity.getContent() ) {
                    return new Response(
                        response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase(),
                        entity.getContentType() != null ?
                            ContentType.parse( entity.getContentType().getValue() ) : null,
                        headers,
                        ByteStreams.toByteArray( is ) );
                }
            } else return new Response(
                response.getStatusLine().getStatusCode(),
                response.getStatusLine().getReasonPhrase(),
                headers
            );
        } catch( InterruptedException | ExecutionException e ) {
            throw new IOException( e );
        }
    }

    @SneakyThrows
    public static <T> Result<T, Throwable> get( Class<T> clazz, String url, long timeout ) {
        HttpUriRequest uri = new HttpGet( Uri.uri( url ) );

        return Result.trying( () -> {
            Response r = execute( uri, timeout );
            return json.unmarshal( clazz, r.body );
        } );
    }

    public static class Response {
        public final int code;
        public final byte[] raw;
        public final String body;
        public final String contentType;
        public final String reasonPhrase;
        private final Map<String, String> headers;

        public Response( int code, String reasonPhrase, ContentType contentType, Map<String, String> headers,
                         byte[] body ) {
            this.code = code;
            this.reasonPhrase = reasonPhrase;
            this.headers = headers;
            this.contentType = contentType != null ? contentType.toString() : null;
            this.raw = body;
            this.body = raw != null ? new String( raw ) : null;
        }

        public Response( int code, String reasonPhrase, Map<String, String> headers ) {
            this( code, reasonPhrase, null, headers, null );
        }

        public Optional<String> getHeader( String name ) {
            return Optional.ofNullable( headers.get( name ) );
        }

        @Override
        public String toString() {
            return code + " " + reasonPhrase;
        }
    }
}
