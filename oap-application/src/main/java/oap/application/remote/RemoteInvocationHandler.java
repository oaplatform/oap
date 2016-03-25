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
package oap.application.remote;

import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import oap.http.SimpleAsyncHttpClient;
import oap.http.SimpleClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

@Slf4j
public class RemoteInvocationHandler implements InvocationHandler {

    private final URI uri;
    private final FST fst;
    private final String service;
    private final CloseableHttpAsyncClient closeableHttpAsyncClient;

    private RemoteInvocationHandler( final URI uri, final String service,
                                     final Path certificateLocation, final String certificatePassword ) {
        this.uri = uri;
        this.service = service;
        this.fst = new FST();
        this.closeableHttpAsyncClient = HttpAsyncClients
            .custom()
            .setSSLContext( createSSLContext( certificateLocation, certificatePassword ) )
            .setMaxConnPerRoute( 1000 )
            .setMaxConnTotal( 10000 )
            .setKeepAliveStrategy( DefaultConnectionKeepAliveStrategy.INSTANCE )
            .setDefaultRequestConfig( RequestConfig.custom().setRedirectsEnabled( false ).build() )
            .build();

        closeableHttpAsyncClient.start();
    }

    @Override
    public Object invoke( final Object proxy, final Method method, final Object[] args ) throws Throwable {
        final Parameter[] parameters = method.getParameters();
        final List<RemoteInvocation.Argument> arguments = new ArrayList<>();

        for( int i = 0; i < parameters.length; i++ ) {
            arguments.add( new RemoteInvocation.Argument( parameters[i].getName(),
                parameters[i].getType(), args[i] ) );
        }

        try {
            final HttpPost post = new HttpPost( uri );

            post.setEntity( new ByteArrayEntity(
                fst.conf.asByteArray( new RemoteInvocation( service, method.getName(), arguments ) ),
                APPLICATION_OCTET_STREAM
            ) );

            final SimpleClient.Response response = SimpleAsyncHttpClient.execute( closeableHttpAsyncClient, post );
            switch( response.code ) {
                case HTTP_OK:
                    return method.getReturnType().equals( void.class ) ? null :
                        fst.conf.asObject( response.raw );
                default:
                    throw new RemoteInvocationException( "code: " + response.code + ", message: " +
                        response.reasonPhrase + "\n" + response.body );
            }
        } catch( final Exception e ) {
            if( log.isTraceEnabled() ) {
                log.trace( e.getMessage(), e );
            } else {
                log.error( e.getMessage() );
            }
            throw Throwables.propagate( e );
        }
    }

    public static Object proxy( final URI uri, final String service, final Class<?> clazz,
                                final Path certificateLocation, final String certificatePassword ) {
        return Proxy.newProxyInstance( clazz.getClassLoader(), new Class[]{ clazz },
            new RemoteInvocationHandler( uri, service, certificateLocation, certificatePassword ) );
    }

    private static SSLContext createSSLContext( final Path certificateLocation, final String certificatePassword ) {
        try {
            final KeyStore keyStore = KeyStore.getInstance( "JKS" );
            keyStore.load( Resources.getResource( certificateLocation.toString() ).openStream(),
                certificatePassword.toCharArray() );

            final TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance( TrustManagerFactory
                    .getDefaultAlgorithm() );
            trustManagerFactory.init( keyStore );

            final SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( null, trustManagerFactory.getTrustManagers(), null );

            return sslContext;
        } catch( final Exception e ) {
            log.error( "An error occurred while setting up SSL Context for certificate [{}]",
                certificateLocation == null ? null : certificateLocation.toString(), e );
            throw new RuntimeException( e );
        }
    }
}
