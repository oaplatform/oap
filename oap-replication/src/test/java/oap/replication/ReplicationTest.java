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

import oap.application.Application;
import oap.application.Kernel;
import oap.application.Module;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import oap.util.Maps;
import oap.ws.http.SimpleHttpClient;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static oap.util.Pair.__;
import static oap.ws.testng.HttpAsserts.HTTP_PREFIX;
import static org.testng.Assert.assertEquals;

public class ReplicationTest extends AbstractTest {
    @AfterMethod
    @Override
    public void afterMethod() throws IOException {
        SimpleHttpClient.reset();

        super.afterMethod();
    }

    @Test
    public void testRemoteGet() {
        Kernel kernel = new Kernel( Module.fromClassPath() );
        kernel.start( Maps.of(
            __( "oap-ws-server", Maps.of( __( "port", Env.port() ) ) ),
            __( "test-client", Maps.of( __( "server", "@remote:" + HTTP_PREFIX + "/replication/@test-server" ) ) )
        ) );

        try {
            TestReplicationClient client = Application.service( "test-client" );
            TestReplicationServer server = Application.service( "test-server" );

            server.getRet = () -> Lists.of( "test" );

            assertEquals( client.get(), Lists.of( "test" ) );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testRemoteSet() {
        Kernel kernel = new Kernel( Module.fromClassPath() );
        kernel.start( Maps.of(
            __( "oap-ws-server", Maps.of( __( "port", Env.port() ) ) ),
            __( "test-client", Maps.of( __( "server", "@remote:" + HTTP_PREFIX + "/replication/@test-server" ) ) )
        ) );

        try {
            TestReplicationClient client = Application.service( "test-client" );
            TestReplicationServer server = Application.service( "test-server" );

            server.setRet = () -> "test";

            assertEquals( client.set( "test1" ), "test" );
            assertEquals( server.lastSetData, "test1" );
        } finally {
            kernel.stop();
        }
    }

    @Test
    public void testRemoteSet2() {
        Kernel kernel = new Kernel( Module.fromClassPath() );
        kernel.start( Maps.of(
            __( "oap-ws-server", Maps.of( __( "port", Env.port() ) ) ),
            __( "test-client2", Maps.of( __( "server", "@remote:" + HTTP_PREFIX + "/replication/@test-server" ) ) )
        ) );

        try {
            TestReplicationClient2 client = Application.service( "test-client2" );
            TestReplicationServer server = Application.service( "test-server" );

            server.setRet = () -> "test";

            assertEquals( client.set( "test1" ), "test" );
            assertEquals( server.lastSetData, "test1" );
        } finally {
            kernel.stop();
        }
    }
}
