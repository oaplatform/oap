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
import oap.http.SimpleHttpClient;
import oap.json.Binder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

public class RemoteInvocationHandler implements InvocationHandler {
    protected static org.slf4j.Logger logger = LoggerFactory.getLogger( RemoteInvocationHandler.class );
    private URI uri;
    private String service;

    public RemoteInvocationHandler( URI uri, String service ) {
        this.uri = uri;
        this.service = service;
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        Parameter[] parameters = method.getParameters();
        List<RemoteInvocation.Argument> arguments = new ArrayList<>();
        for( int i = 0; i < parameters.length; i++ ) {
            Parameter parameter = parameters[i];
            arguments.add( new RemoteInvocation.Argument( parameter.getName(), parameter.getType(), args[i] ) );
        }

        try {
            HttpPost post = new HttpPost( uri );
            post.setEntity( new StringEntity(
                Binder.marshal( new RemoteInvocation( service, method.getName(), arguments ) ),
                ContentType.APPLICATION_JSON ) );
            SimpleHttpClient.Response response = SimpleHttpClient.execute( post );
            switch( response.code ) {
                case HTTP_OK:
                    return method.getReturnType().equals( Void.class ) ? null :
                        Binder.unmarshal( method.getReturnType(), response.body, true );
                default:
                    throw new RemoteInvocationException( "code: " + response.code + ", message: " + response.body );
            }
        } catch( Exception e ) {
            if( logger.isTraceEnabled() ) logger.trace( e.getMessage(), e );
            else logger.error( e.getMessage() );
            throw Throwables.propagate( e );
        }
    }

    public static Object proxy( URI uri, String service, Class<?> clazz ) {
        return Proxy.newProxyInstance( clazz.getClassLoader(), new Class[]{ clazz },
            new RemoteInvocationHandler( uri, service ) );
    }
}
