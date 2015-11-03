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

import oap.util.Pair;
import oap.util.Result;
import oap.ws.Uri;
import oap.ws.apache.SimpleHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.LoggerFactory;

import static java.net.HttpURLConnection.HTTP_OK;
import static oap.util.Pair.__;
import static oap.util.Result.success;

/**
 * Created by Igor Petrenko on 06.10.2015.
 */
public abstract class ReplicationPostClient<T> implements Runnable {
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger( ReplicationPostClient.class );

    public String replicationUrl;

    protected abstract String getMasterServiceName();

    public final synchronized void run() {
        T data = getData();

        try {
            if( replicationUrl == null ) throw new IllegalAccessError( "master is not configured" );

            final HttpPost post = new HttpPost( Uri.uri( replicationUrl,
                __( "service", getMasterServiceName() ) ) );

            post.setEntity( new StringEntity( data.toString(), ContentType.APPLICATION_JSON ) );

            SimpleHttpClient.Response response = SimpleHttpClient.execute( post );

            switch( response.code ) {
                case HTTP_OK:
                    process( success( __( response.body, data ) ) );
                    break;
                default:
                    process( Result.failure( __( response.body, data ) ) );
                    logger.error( "code: {}, message: {}", response.code, response.body );

            }
        } catch( Exception e ) {
            if( logger.isTraceEnabled() ) logger.trace( e.getMessage(), e );
            else logger.error( e.getMessage() );
            process( Result.failure( __( e.getMessage(), data ) ) );
        }
    }

    protected abstract T getData();

    protected abstract void process( Result<Pair<String, T>, Pair<String, T>> result );
}
