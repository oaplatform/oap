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

import oap.util.Result;
import oap.ws.Uri;
import oap.ws.apache.SimpleHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.joda.time.DateTimeUtils;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.util.Pair.__;
import static oap.util.Result.success;

/**
 * Created by Igor Petrenko on 06.10.2015.
 */
public abstract class ReplicationGet extends ReplicationClient {
    private long lastSyncTime = -1;

    public ReplicationGet( String master, String replicationUrl ) {
        super( master, replicationUrl );
    }

    public final long getLastSyncTime() {
        return lastSyncTime;
    }

    public final synchronized void run() {
        run2( lastSyncTime );
    }

    public final synchronized void resync() {
        run2( -1 );
    }

    private void run2( long syncTime ) {
        try {
            if( getReplicationUrl() == null ) throw new IllegalAccessError( "master is not configured" );

            final long now = DateTimeUtils.currentTimeMillis();

            SimpleHttpClient.Response response = SimpleHttpClient.execute(
                new HttpGet( Uri.uri( getReplicationUrl(), __( "lastSyncTime", syncTime ),
                    __( "service", getMaster() ) ) ) );

            switch( response.code ) {
                case HTTP_NOT_MODIFIED:
                    break;
                case HTTP_OK:
                    process( success( response.body ) );
                    lastSyncTime = now;
                    break;
                default:
                    process( Result.failure( response.reasonPhrase ) );
                    logger.error( "code: {}, message: {}", response.code, response.body );

            }
        } catch( Exception e ) {
            process( Result.failure( e.getMessage() ) );
        }

    }

    protected abstract void process( Result<String, String> result );
}
