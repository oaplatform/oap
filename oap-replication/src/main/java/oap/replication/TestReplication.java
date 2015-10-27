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

import com.fasterxml.jackson.core.type.TypeReference;
import oap.json.Binder;
import oap.ws.testng.HttpAsserts;

import java.util.Collections;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static oap.util.Pair.__;
import static oap.ws.testng.HttpAsserts.HTTP_PREFIX;
import static org.testng.Assert.assertEquals;

/**
 * Created by Igor Petrenko on 05.10.2015.
 */
public class TestReplication {
    public static <T> List<T> sync( TypeReference<List<T>> ref, long lastSyncTime, String service ) {
        final HttpAsserts.Response response =
            HttpAsserts.get( HTTP_PREFIX + "/replication/", __( "lastSyncTime", lastSyncTime ),
                __( "service", service ) );

        if( response.code == HTTP_NOT_MODIFIED ) return Collections.emptyList();

        assertEquals( HTTP_OK, response.code, response.body );

        return Binder.unmarshal( ref, response.body );
    }
}
