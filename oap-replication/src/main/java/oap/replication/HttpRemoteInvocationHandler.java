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

package oap.replication;

import oap.json.Binder;
import oap.ws.Uri;
import oap.ws.apache.SimpleHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.util.Pair.__;

public class HttpRemoteInvocationHandler implements InvocationHandler {
    public static final String SERVICE_PARAM = "service";
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger( "replication" );
    private String master;
    private String replicationUrl;

    public HttpRemoteInvocationHandler( String master, String replicationUrl ) {
        this.master = master;
        this.replicationUrl = replicationUrl;
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        final RpcData rpcData = new RpcData( master, method.getName() );
        final Parameter[] parameters = method.getParameters();
        for( int i = 0; i < parameters.length; i++ ) {
            final Parameter parameter = parameters[i];
            final Object arg = args[i];

            final String name = parameter.getName();

            rpcData.arguments.add( new RpcData.RpcArgumentData( name, parameter.getType(), arg ) );
        }

        final String marshal = Binder.marshal( rpcData );

        try {
            final HttpPost post = new HttpPost( Uri.uri( replicationUrl, __( SERVICE_PARAM, master ) ) );

            post.setEntity( new StringEntity( marshal, ContentType.APPLICATION_JSON ) );

            SimpleHttpClient.Response response = SimpleHttpClient.execute( post );

            switch( response.code ) {
                case HTTP_OK:
                    return Binder.unmarshal( method.getReturnType(), response.body );

                default:
                    logger.error( "code: {}, message: {}", response.code, response.body );

                    throw new RuntimeException( "code: " + response.code + ", message: " + response.body );
            }
        } catch( Exception e ) {
            if( logger.isTraceEnabled() ) logger.trace( e.getMessage(), e );
            else logger.error( e.getMessage() );
            throw new RuntimeException( e );
        }
    }
}
