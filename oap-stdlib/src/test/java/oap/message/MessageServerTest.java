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

package oap.message;

import oap.message.MessageListenerMock.TestMessage;
import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.ResetSystemTimer;
import oap.testng.TestDirectory;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static oap.message.MessageListenerMock.MESSAGE_TYPE;
import static oap.message.MessageListenerMock.MESSAGE_TYPE2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Created by igor.petrenko on 2019-12-17.
 */
public class MessageServerTest extends Fixtures {
    {
        fixture( TestDirectory.FIXTURE );
        fixture( ResetSystemTimer.FIXTURE );
    }

    @Test
    public void testUniqueMessageTypeListener() throws IOException {
        var listener1 = new MessageListenerMock( "l1-", MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( "l2-", MESSAGE_TYPE );

        try( var server = new MessageServer( Env.tmpPath( "controlStatePath.st" ), 0, List.of( listener1, listener2 ), -1 ) ) {
            try( var client = new MessageSender( "localhost", server.getPort() ) ) {
                assertThatCode( server::start )
                    .isInstanceOf( IllegalArgumentException.class )
                    .hasMessage( "duplicate [l2--1, l1--1]" );
            }
        }
    }

    @Test
    public void testSendAndReceive() throws IOException {
        var listener1 = new MessageListenerMock( MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( MESSAGE_TYPE2 );
        try( var server = new MessageServer( Env.tmpPath( "controlStatePath.st" ), 0, List.of( listener1, listener2 ), -1 ) ) {
            server.start();

            try( var client = new MessageSender( "localhost", server.getPort() ) ) {
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "124".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "124".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE2, "555".getBytes() ) );

                assertThat( listener1.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ), new TestMessage( 1, "124" ) ) );
                assertThat( listener2.messages ).isEqualTo( List.of( new TestMessage( 1, "555" ) ) );
            }
        }
    }

    @Test
    public void testSendAndReceiveJson() throws IOException {
        var listener1 = new MessageListenerJsonMock( MESSAGE_TYPE );
        try( var server = new MessageServer( Env.tmpPath( "controlStatePath.st" ), 0, List.of( listener1 ), -1 ) ) {
            server.start();

            try( var client = new MessageSender( "localhost", server.getPort() ) ) {
                assertTrue( client.sendJson( MESSAGE_TYPE, "123" ) );
                assertTrue( client.sendJson( MESSAGE_TYPE, "124" ) );
                assertTrue( client.sendJson( MESSAGE_TYPE, "124" ) );
                assertTrue( client.sendJson( MESSAGE_TYPE, "123" ) );

                assertThat( listener1.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ), new TestMessage( 1, "124" ) ) );
            }
        }
    }

    @Test
    public void testUnknownError() throws IOException {
        var listener = new MessageListenerMock( MESSAGE_TYPE );
        try( var server = new MessageServer( Env.tmpPath( "controlStatePath.st" ), 0, List.of( listener ), -1 ) ) {
            server.start();

            try( var client = new MessageSender( "localhost", server.getPort() ) ) {
                listener.throwUnknownError( 1 );
                assertFalse( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );

                assertThat( listener.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );
            }
        }
    }

    @Test
    public void testTtl() throws IOException {
        var hashTtl = 1000;

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var listener = new MessageListenerMock( MESSAGE_TYPE );
        try( var server = new MessageServer( Env.tmpPath( "controlStatePath.st" ), 0, List.of( listener ), hashTtl ) ) {
            server.start();

            try( var client = new MessageSender( "localhost", server.getPort() ) ) {
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );

                assertThat( listener.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );

                DateTimeUtils.setCurrentMillisFixed( DateTimeUtils.currentTimeMillis() + hashTtl + 1 );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );

                assertThat( listener.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ), new TestMessage( 1, "123" ) ) );
            }
        }
    }

    @Test
    public void testPersistence() throws IOException {
        var hashTtl = 1000;

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var listener = new MessageListenerMock( MESSAGE_TYPE );
        try( var serverSocket = Env.serverSocket();
             var client = new MessageSender( "localhost", serverSocket.getLocalPort() ) ) {
            var port = serverSocket.getLocalPort();

            try( var server = new MessageServer( Env.tmpPath( "controlStatePath.st" ), serverSocket, List.of( listener ), hashTtl ) ) {
                server.soTimeout = 2000;
                server.start();

                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );

                assertThat( listener.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );
            }

            try( var server = new MessageServer( Env.tmpPath( "controlStatePath.st" ), port, List.of( listener ), hashTtl ) ) {
                server.soTimeout = 2000;
                server.start();

                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );
                assertTrue( client.sendObject( MESSAGE_TYPE, "123".getBytes() ) );

                assertThat( listener.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );
            }
        }
    }

}
