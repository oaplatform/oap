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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.concurrent.AsyncCallbacks;
import oap.io.Closeables;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Stream;
import oap.util.Try;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.SSLContexts;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.io.ProgressInputStream.progress;
import static oap.util.Maps.Collectors.toMap;
import static oap.util.Pair.__;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

@Slf4j
public class Client implements Closeable {
    public static final Client DEFAULT = custom()
        .onError( ( c, e ) -> log.error( e.getMessage(), e ) )
        .onTimeout( ( c ) -> log.error( "timeout" ) )
        .build();
    private static final long FOREVER = Long.MAX_VALUE;
    private static final FutureCallback<org.apache.http.HttpResponse> FUTURE_CALLBACK = new FutureCallback<org.apache.http.HttpResponse>() {
        @Override
        public void completed( org.apache.http.HttpResponse result ) {
        }

        @Override
        public void failed( Exception e ) {
        }

        @Override
        public void cancelled() {

        }
    };
    private final BasicCookieStore basicCookieStore;
    private ClientBuilder builder;
    private CloseableHttpAsyncClient client;

    private Client( BasicCookieStore basicCookieStore, ClientBuilder builder ) {
        this.client = builder.client();

        this.basicCookieStore = basicCookieStore;
        this.builder = builder;
    }


    public static ClientBuilder custom( Path certificateLocation, String certificatePassword, int connectTimeout, int readTimeout ) {
        return new ClientBuilder( certificateLocation, certificatePassword, connectTimeout, readTimeout );
    }

    public static ClientBuilder custom() {
        return new ClientBuilder( null, null, 0, 0 );
    }

    private static Map<String, String> headers( org.apache.http.HttpResponse response ) {
        return Arrays.stream( response.getAllHeaders() )
            .map( h -> __( h.getName(), h.getValue() ) )
            .collect( toMap() );
    }

    @SneakyThrows
    private static SSLContext createSSLContext( Path certificateLocation, String certificatePassword ) {
        try( val inputStream = IoStreams.in( certificateLocation, PLAIN ) ) {
            KeyStore keyStore = KeyStore.getInstance( "JKS" );
            keyStore.load( inputStream, certificatePassword.toCharArray() );

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            trustManagerFactory.init( keyStore );

            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( null, trustManagerFactory.getTrustManagers(), null );

            return sslContext;
        }
    }

    public Response get( String uri ) {
        return get( uri, Maps.empty(), Maps.empty() );
    }

    public Response get( URI uri ) {
        return get( uri, Maps.empty() );
    }

    @SafeVarargs
    public final Response get( String uri, Pair<String, Object>... params ) {
        return get( uri, Maps.of( params ) );
    }

    public Response get( String uri, Map<String, Object> params ) {
        return get( uri, params, Maps.empty() );
    }

    public Response get( String uri, Map<String, Object> params, Map<String, Object> headers ) {
        return get( uri, params, headers, FOREVER )
            .orElseThrow( () -> new oap.concurrent.TimeoutException( "no response" ) );
    }

    public Response get( URI uri, Map<String, Object> headers ) {
        return get( uri, headers, FOREVER )
            .orElseThrow( () -> new oap.concurrent.TimeoutException( "no response" ) );
    }

    public Optional<Response> get( String uri, Map<String, Object> params, long timeout ) {
        return get( uri, params, Maps.empty(), timeout );
    }

    public Optional<Response> get( String uri, Map<String, Object> params, Map<String, Object> headers, long timeout ) {
        return get( Uri.uri( uri, params ), headers, timeout );
    }

    public Optional<Response> get( URI uri, Map<String, Object> headers, long timeout ) {
        return execute( new HttpGet( uri ), headers, timeout );
    }

    public Response post( String uri, Map<String, Object> params ) {
        return post( uri, params, Maps.empty() );
    }

    public Response post( String uri, Map<String, Object> params, Map<String, Object> headers ) {
        return post( uri, params, headers, FOREVER )
            .orElseThrow( () -> new RuntimeException( "no response" ) );
    }

    public Optional<Response> post( String uri, Map<String, Object> params, long timeout ) {
        return post( uri, params, Maps.empty(), timeout );
    }

    public Optional<Response> post( String uri, Map<String, Object> params, Map<String, Object> headers, long timeout ) {
        try {
            HttpPost request = new HttpPost( uri );
            request.setEntity( new UrlEncodedFormEntity( Stream.of( params.entrySet() )
                .<NameValuePair>map( e -> new BasicNameValuePair( e.getKey(),
                    e.getValue() == null ? "" : e.getValue().toString() ) )
                .toList()
            ) );
            return execute( request, headers, timeout );
        } catch( UnsupportedEncodingException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public Response post( String uri, String content, ContentType contentType ) {
        return post( uri, content, contentType, Maps.empty() );
    }

    public Response post( String uri, String content, ContentType contentType, Map<String, Object> headers ) {
        return post( uri, content, contentType, headers, FOREVER )
            .orElseThrow( () -> new RuntimeException( "no response" ) );
    }

    public Optional<Response> post( String uri, String content, ContentType contentType, long timeout ) {
        return post( uri, content, contentType, Maps.empty(), timeout );
    }

    public Optional<Response> post( String uri, String content, ContentType contentType, Map<String, Object> headers, long timeout ) {
        HttpPost request = new HttpPost( uri );
        request.setEntity( new StringEntity( content, contentType ) );
        return execute( request, headers, timeout );
    }

    public Optional<Response> post( String uri, byte[] content, long timeout ) {
        HttpPost request = new HttpPost( uri );
        request.setEntity( new ByteArrayEntity( content, APPLICATION_OCTET_STREAM ) );
        return execute( request, Maps.empty(), timeout );
    }

    @SneakyThrows
    public Response post( String uri, Consumer<OutputStream> outputStream, ContentType contentType ) {
        HttpPost request = new HttpPost( uri );

        return post( outputStream, contentType, request );
    }

    @SneakyThrows
    public Response post( URI uri, Consumer<OutputStream> outputStream, ContentType contentType ) {
        HttpPost request = new HttpPost( uri );

        return post( outputStream, contentType, request );
    }

    private Response post( Consumer<OutputStream> outputStream, ContentType contentType, HttpPost request ) throws IOException {
        val pos = new PipedOutputStream();
        val pis = new PipedInputStream( pos );
        request.setEntity( new InputStreamEntity( pis, contentType ) );

        return execute( request, Maps.empty(), FOREVER, Try.run( () -> {
            outputStream.accept( pos );
            pos.close();
        } ) )
            .orElseThrow( () -> new RuntimeException( "no response" ) );
    }


    public Response post( String uri, InputStream content, ContentType contentType ) {
        HttpPost request = new HttpPost( uri );
        request.setEntity( new InputStreamEntity( content, contentType ) );
        return execute( request, Maps.empty(), FOREVER )
            .orElseThrow( () -> new RuntimeException( "no response" ) );
    }

    public Response post( String uri, InputStream content, ContentType contentType, Map<String, Object> headers ) {
        HttpPost request = new HttpPost( uri );
        request.setEntity( new InputStreamEntity( content, contentType ) );
        return execute( request, headers, FOREVER )
            .orElseThrow( () -> new RuntimeException( "no response" ) );
    }

    public Response post( String uri, byte[] content, ContentType contentType, Map<String, Object> headers ) {
        HttpPost request = new HttpPost( uri );
        request.setEntity( new ByteArrayEntity( content, contentType ) );
        return execute( request, headers, FOREVER )
            .orElseThrow( () -> new RuntimeException( "no response" ) );
    }

    public Response put( String uri, String content, ContentType contentType ) {
        HttpPut request = new HttpPut( uri );
        request.setEntity( new StringEntity( content, contentType ) );
        return execute( request, Maps.empty(), FOREVER )
            .orElseThrow( () -> new RuntimeException( "no response" ) );
    }

    public Response delete( String uri ) {
        return delete( uri, FOREVER );
    }

    public Response delete( String uri, long timeout ) {
        return delete( uri, Maps.empty(), timeout );
    }

    public Response delete( String uri, Map<String, Object> headers ) {
        return delete( uri, headers, FOREVER );
    }

    public Response delete( String uri, Map<String, Object> headers, long timeout ) {
        HttpDelete request = new HttpDelete( uri );
        return execute( request, headers, timeout ).orElseThrow( () -> new RuntimeException( "no response" ) );
    }

    public List<Cookie> getCookies() {
        return basicCookieStore.getCookies();
    }

    public void clearCookies() {
        basicCookieStore.clear();
    }

    private Optional<Response> execute( HttpUriRequest request, Map<String, Object> headers, long timeout ) {
        return execute( request, headers, timeout );
    }

    @SneakyThrows
    private Optional<Response> execute( HttpUriRequest request, Map<String, Object> headers, long timeout, Runnable asyncRunnable ) {
        try {
            headers.forEach( ( name, value ) -> request.setHeader( name, value == null ? "" : value.toString() ) );

            Future<HttpResponse> future = client.execute( request, FUTURE_CALLBACK );
            asyncRunnable.run();
            HttpResponse response = timeout == FOREVER ? future.get()
                : future.get( timeout, MILLISECONDS );

            Map<String, String> responsHeaders = headers( response );
            Response result;
            if( response.getEntity() != null ) {
                HttpEntity entity = response.getEntity();
                result = new Response(
                    response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase(),
                    responsHeaders,
                    Optional.ofNullable( entity.getContentType() )
                        .map( ct -> ContentType.parse( entity.getContentType().getValue() ) ),
                    entity.getContent()
                );
            } else result = new Response(
                response.getStatusLine().getStatusCode(),
                response.getStatusLine().getReasonPhrase(),
                responsHeaders
            );
            builder.onSuccess.accept( this );
            return Optional.of( result );
        } catch( ExecutionException e ) {
            final ExecutionException newEx = new ExecutionException( request.getURI().toString(), e.getCause() );
            builder.onError.accept( this, newEx );
            throw newEx;
        } catch( IOException e ) {
            final ExecutionException newEx = new ExecutionException( request.getURI().toString(), e );
            builder.onError.accept( this, newEx );
            throw newEx;
        } catch( InterruptedException | TimeoutException e ) {
            builder.onTimeout.accept( this );
            return Optional.empty();
        }
    }

    @SneakyThrows
    public Optional<Path> download( String url, Optional<Long> modificationTime, Optional<Path> file, Consumer<Integer> progress ) {
        try {
            val response = resolve( url, modificationTime ).orElse( null );
            if( response == null ) return Optional.empty();

            val entity = response.getEntity();

            final Path path = file.orElseGet( Try.supply( () -> {
                final IoStreams.Encoding encoding = IoStreams.Encoding.from( url );

                final File tempFile = File.createTempFile( "file", "down" + encoding.extension );
                tempFile.deleteOnExit();
                return tempFile.toPath();
            } ) );

            try( InputStream in = new BufferedInputStream( entity.getContent() ) ) {
                IoStreams.write( path, PLAIN, in, false, file.isPresent(), progress( entity.getContentLength(), progress ) );
            }

            final Header lastModified = response.getLastHeader( "Last-Modified" );
            if( lastModified != null ) {
                final Date date = DateUtils.parseDate( lastModified.getValue() );

                Files.setLastModifiedTime( path, date.getTime() );
            }

            builder.onSuccess.accept( this );

            return Optional.of( path );
        } catch( ExecutionException | IOException e ) {
            builder.onError.accept( this, e );
            throw e;
        } catch( InterruptedException e ) {
            builder.onTimeout.accept( this );
            return Optional.empty();
        }
    }

    private Optional<HttpResponse> resolve( String url ) {
        return resolve( url );
    }

    private Optional<HttpResponse> resolve( String url, Optional<Long> ifModifiedSince ) throws InterruptedException, ExecutionException, IOException {
        HttpGet request = new HttpGet( url );
        ifModifiedSince.ifPresent( ims -> request.addHeader( "If-Modified-Since", DateUtils.formatDate( new Date( ims ) ) ) );
        Future<HttpResponse> future = client.execute( request, FUTURE_CALLBACK );
        HttpResponse response = future.get();
        if( response.getStatusLine().getStatusCode() == HTTP_OK && response.getEntity() != null )
            return Optional.of( response );
        else if( response.getStatusLine().getStatusCode() == HTTP_MOVED_TEMP ) {
            Header location = response.getFirstHeader( "Location" );
            if( location == null ) throw new IOException( "redirect w/o location!" );
            log.debug( "following {}", location.getValue() );
            return resolve( location.getValue() );
        } else if( response.getStatusLine().getStatusCode() == HTTP_NOT_MODIFIED ) {
            return Optional.empty();
        } else
            throw new IOException( response.getStatusLine().toString() );
    }

    public void reset() {
        Closeables.close( client );
        client = builder.client();
    }

    @Override
    public void close() {
        Closeables.close( client );
    }

    @SneakyThrows
    public Response uploadFile( String uri, String prefix, Path path ) {
        final ContentType contentType = ContentType.create( java.nio.file.Files.probeContentType( path ) );

        OkHttpClient client = new OkHttpClient();

        final MultipartBody body = new MultipartBody.Builder()
            .setType( MultipartBody.FORM )
            .addFormDataPart( "upfile", path.toFile().getName(), RequestBody.create( MediaType.parse( contentType.toString() ), path.toFile() ) )
            .addFormDataPart( "prefix", prefix )
            .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
            .url( uri )
            .post( body )
            .build();


        val response = client.newCall( request ).execute();

        val headers = response.headers();
        final java.util.stream.Stream<String> stream = headers.names().stream();
        final Map<String, String> h = stream.collect( Collectors.toMap( n -> n, headers::get ) );
        val responseBody = response.body();
        return new Response( response.code(), response.message(), h,
            Optional.ofNullable( responseBody.contentType() ).map( mt -> ContentType.create( mt.type() + "/" + mt.subtype(), mt.charset() ) ),
            responseBody.byteStream() );
    }

    @ToString( exclude = { "inputStream", "content" } )
    public static class Response implements Closeable {
        public final int code;
        public final String reasonPhrase;
        public final Optional<ContentType> contentType;
        public final Map<String, String> headers;
        private InputStream inputStream;
        private byte[] content = null;

        @SneakyThrows
        public Response( int code, String reasonPhrase, Map<String, String> headers, Optional<ContentType> contentType, InputStream inputStream ) {
            this.code = code;
            this.reasonPhrase = reasonPhrase;
            this.headers = headers;
            this.contentType = contentType;
            this.inputStream = inputStream;
        }

        public Response( int code, String reasonPhrase, Map<String, String> headers ) {
            this( code, reasonPhrase, headers, Optional.empty(), null );
        }

        @Nullable
        @SneakyThrows
        public byte[] content() {
            if( content == null && inputStream != null ) {
                synchronized( this ) {
                    if( content == null ) {
                        content = ByteStreams.toByteArray( inputStream );
                        close();
                    }
                }
            }
            return content;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String contentString() {
            val content = content();

            if( content == null ) return null;

            return new String( content, contentType.map( ContentType::getCharset ).orElse( StandardCharsets.UTF_8 ) );
        }

        public <T> Optional<T> unmarshal( Class<?> clazz ) {
            if( inputStream != null ) {
                synchronized( this ) {
                    if( inputStream != null ) {
                        return Binder.json.unmarshal( clazz, inputStream );
                    }
                }
            }

            val contentString = contentString();
            if( contentString == null ) return Optional.empty();

            return Optional.of( Binder.json.unmarshal( clazz, contentString ) );
        }

        public <T> Optional<T> unmarshal( TypeRef<T> ref ) {
            if( inputStream != null ) {
                synchronized( this ) {
                    if( inputStream != null ) {
                        return Optional.of( Binder.json.unmarshal( ref, inputStream ) );
                    }
                }
            }

            val contentString = contentString();
            if( contentString == null ) return Optional.empty();


            return Optional.of( Binder.json.unmarshal( ref, contentString ) );
        }

        @Override
        public void close() {
            Closeables.close( inputStream );
            inputStream = null;
        }
    }

    public static class ClientBuilder extends AsyncCallbacks<ClientBuilder, Client> {

        private final BasicCookieStore basicCookieStore;
        private Path certificateLocation;
        private String certificatePassword;
        private int connectTimeout;
        private int readTimeout;
        private int maxConnTotal = 10000;
        private int maxConnPerRoute = 1000;
        private boolean redirectsEnabled = false;
        private String cookieSpec = CookieSpecs.STANDARD;

        public ClientBuilder( Path certificateLocation, String certificatePassword, int connectTimeout, int readTimeout ) {
            basicCookieStore = new BasicCookieStore();

            this.certificateLocation = certificateLocation;
            this.certificatePassword = certificatePassword;
            this.connectTimeout = connectTimeout;
            this.readTimeout = readTimeout;
        }

        private HttpAsyncClientBuilder initialize() {
            try {
                final PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(
                    new DefaultConnectingIOReactor( IOReactorConfig.custom()
                        .setConnectTimeout( connectTimeout )
                        .setSoTimeout( readTimeout )
                        .build() ),
                    RegistryBuilder.<SchemeIOSessionStrategy>create()
                        .register( "http", NoopIOSessionStrategy.INSTANCE )
                        .register( "https",
                            new SSLIOSessionStrategy( certificateLocation != null
                                ? createSSLContext( certificateLocation, certificatePassword )
                                : SSLContexts.createDefault(),
                                split( System.getProperty( "https.protocols" ) ),
                                split( System.getProperty( "https.cipherSuites" ) ),
                                new DefaultHostnameVerifier( PublicSuffixMatcherLoader.getDefault() ) ) )
                        .build() );

                connManager.setMaxTotal( maxConnTotal );
                connManager.setDefaultMaxPerRoute( maxConnPerRoute );

                return ( certificateLocation != null
                    ? HttpAsyncClients.custom()
                    .setSSLContext( createSSLContext( certificateLocation, certificatePassword ) )
                    : HttpAsyncClients.custom() )
                    .setMaxConnPerRoute( maxConnPerRoute )
                    .setConnectionManager( connManager )
                    .setMaxConnTotal( maxConnTotal )
                    .setKeepAliveStrategy( DefaultConnectionKeepAliveStrategy.INSTANCE )
                    .setDefaultRequestConfig( RequestConfig
                        .custom()
                        .setRedirectsEnabled( redirectsEnabled )
                        .setCookieSpec( cookieSpec )
                        .build() )
                    .setDefaultCookieStore( basicCookieStore );
            } catch( IOReactorException e ) {
                throw new UncheckedIOException( e );
            }
        }

        public ClientBuilder setMaxConnTotal( int maxConnTotal ) {
            this.maxConnTotal = maxConnTotal;

            return this;
        }

        public ClientBuilder setMaxConnPerRoute( int maxConnPerRoute ) {
            this.maxConnPerRoute = maxConnPerRoute;

            return this;
        }

        public ClientBuilder setRedirectsEnabled( boolean redirectsEnabled ) {
            this.redirectsEnabled = redirectsEnabled;

            return this;
        }

        public ClientBuilder setCookieSpec( String cookieSpec ) {
            this.cookieSpec = cookieSpec;

            return this;
        }

        private CloseableHttpAsyncClient client() {
            final CloseableHttpAsyncClient build = initialize().build();
            build.start();
            return build;
        }

        public Client build() {
            return new Client( basicCookieStore, this );
        }
    }
}
