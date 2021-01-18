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

import lombok.SneakyThrows;
import oap.concurrent.Threads;
import oap.io.Closeables;
import oap.io.Files;
import oap.message.MessageListenerMock.TestMessage;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.util.List;

import static oap.io.content.ContentWriter.ofJson;
import static oap.io.content.ContentWriter.ofString;
import static oap.message.MessageAvailabilityReport.State.FAILED;
import static oap.message.MessageAvailabilityReport.State.OPERATIONAL;
import static oap.message.MessageListenerMock.MESSAGE_TYPE;
import static oap.message.MessageListenerMock.MESSAGE_TYPE2;
import static oap.testng.Asserts.assertEventually;
import static oap.testng.TestDirectoryFixture.testPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testng.Assert.assertNotNull;

public class MessageServerTest extends Fixtures {

    {
        fixture( TestDirectoryFixture.FIXTURE );
        fixture( SystemTimerFixture.FIXTURE );
    }

    @Test
    public void uniqueMessageTypeListener() {
        var listener1 = new MessageListenerMock( "l1-", MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( "l2-", MESSAGE_TYPE );

        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener1, listener2 ), -1 ) ) {
            try( var client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) ) ) {
                client.start();

                assertThatCode( server::start )
                    .isInstanceOf( IllegalArgumentException.class )
                    .hasMessage( "duplicate [l2-127, l1-127]" );
            }
        }
    }

    @SneakyThrows
    @Test
    public void rejectedException() {
        var listener1 = new MessageListenerJsonMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener1 ), -1 ) ) {
            server.maximumPoolSize = 1;
            server.start();

            MessageSender client1, client2;
            client1 = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) );
            client2 = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) );
            try {
                client1.memorySyncPeriod = -1;
                client1.poolSize = 4;
                client2.memorySyncPeriod = -1;
                client2.poolSize = 4;

                client1.start();
                client2.start();

                client1.send( MESSAGE_TYPE, "123", ofJson() ).syncMemory();
                client2.send( MESSAGE_TYPE, "123", ofJson() );

                assertThat( listener1.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );
            } finally {
                client2.close();
                client1.close();
            }
            assertThat( client1.getMessagesMemorySize() ).isEqualTo( 0L );
            assertThat( client2.getMessagesMemorySize() ).isEqualTo( 0L );

            MessageSender client;
            client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) );
            try {
                client.poolSize = 4;

                client.start();

                client.send( MESSAGE_TYPE, "1234", ofJson() ).syncMemory();

                assertThat( listener1.messages ).isEqualTo( List.of( new TestMessage( 1, "123" ), new TestMessage( 1, "1234" ) ) );
            } finally {
                client.close();
            }
            assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );
        }
    }

    @SneakyThrows
    @Test
    public void sendAndReceive() {
        var listener1 = new MessageListenerMock( MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( MESSAGE_TYPE2 );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener1, listener2 ), -1 ) ) {
            server.start();

            var dir = testPath( "dir" );
            MessageSender client;
            client = new MessageSender( "localhost", server.getPort(), dir );
            try {
                client.start();

                client
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "124", ofString() )
                    .send( MESSAGE_TYPE, "124", ofString() )
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE2, "555", ofString() )
                    .syncMemory();

                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "123" ), new TestMessage( 1, "124" ) );
                assertThat( listener2.getMessages() ).containsOnly( new TestMessage( 1, "555" ) );
            } finally {
                client.close();
            }
            assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );

            assertThat( dir ).doesNotExist();
        }
    }

    @SneakyThrows
    @Test
    public void sendAndReceiveJson() {
        var listener1 = new MessageListenerJsonMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener1 ), -1 ) ) {
            server.start();

            try( var client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) ) ) {
                client.start();

                client
                    .send( MESSAGE_TYPE, "123", ofJson() )
                    .send( MESSAGE_TYPE, "124", ofJson() )
                    .send( MESSAGE_TYPE, "124", ofJson() )
                    .send( MESSAGE_TYPE, "123", ofJson() )
                    .syncMemory();

                assertThat( listener1.messages ).containsOnly( new TestMessage( 1, "123" ), new TestMessage( 1, "124" ) );
            }
        }
    }

    @SneakyThrows
    @Test
    public void sendAndReceiveJsonOneThread() {
        var listener1 = new MessageListenerJsonMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener1 ), -1 ) ) {
            server.start();

            try( var client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) ) ) {
                client.poolSize = 1;
                client.start();

                client
                    .send( MESSAGE_TYPE, "123", ofJson() )
                    .send( MESSAGE_TYPE, "124", ofJson() )
                    .send( MESSAGE_TYPE, "124", ofJson() )
                    .send( MESSAGE_TYPE, "123", ofJson() )
                    .syncMemory();

                assertThat( listener1.messages ).containsOnly( new TestMessage( 1, "123" ), new TestMessage( 1, "124" ) );
            }
        }
    }

    @Test
    public void unknownError() {
        var listener = new MessageListenerMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener ), -1 ) ) {
            server.start();

            MessageSender client;
            client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) );
            try {
                client.start();

                listener.throwUnknownError( 200000000 );
                client.send( MESSAGE_TYPE, "123", ofString() );

                while( listener.throwUnknownError > 200000000 - 10 )
                    Threads.sleepSafely( 10 );
                assertThat( listener.getMessages() ).isEmpty();

                listener.throwUnknownError( 2 );
                while( listener.throwUnknownError > 0 )
                    Threads.sleepSafely( 10 );

                assertEventually( 100, 10, () ->
                    assertThat( listener.getMessages() ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) ) );
            } finally {
                client.close();
            }
            assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );
        }
    }

    @Test
    public void statusError() {
        var listener = new MessageListenerMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener ), -1 ) ) {
            server.start();

            try( var client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) ) ) {
                client.start();

                listener.setStatus( 567 );
                client.send( MESSAGE_TYPE, "123", ofString() );

                while( listener.accessCount.get() > 4 )
                    Threads.sleepSafely( 10 );

                assertThat( listener.getMessages() ).isEmpty();

                listener.setStatusOk();
                assertEventually( 10, 100, () ->
                    assertThat( listener.getMessages() ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) ) );
            }
        }
    }

    @SneakyThrows
    @Test
    public void ttl() {
        var hashTtl = 1000;

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var listener = new MessageListenerMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener ), hashTtl ) ) {
            server.start();

            MessageSender client;
            client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) );
            try {
                client.start();

                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();
                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();
                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();

                assertThat( listener.getMessages() ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );
                assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );

                DateTimeUtils.setCurrentMillisFixed( DateTimeUtils.currentTimeMillis() + hashTtl + 1 );
                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();
                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();
                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();

                assertThat( listener.getMessages() ).isEqualTo( List.of( new TestMessage( 1, "123" ), new TestMessage( 1, "123" ) ) );
            } finally {
                client.close();
            }
            assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );
        }
    }

    @SneakyThrows
    @Test
    public void persistence() {
        var hashTtl = 1000;

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var listener = new MessageListenerMock( MESSAGE_TYPE );

        MessageServer server = null;
        MessageSender client = null;
        try {
            server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener ), hashTtl );
            server.soTimeout = 2000;
            server.start();

            client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) );
            client.start();

            client
                .send( MESSAGE_TYPE, "123", ofString() )
                .send( MESSAGE_TYPE, "123", ofString() )
                .send( MESSAGE_TYPE, "123", ofString() )
                .syncMemory();

            assertThat( listener.getMessages() ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );

            server.close();

            try( var server2 = new MessageServer( testPath( "controlStatePath.st" ), server.getPort(), List.of( listener ), hashTtl ) ) {
                server2.soTimeout = 2000;
                server2.start();

                client
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .syncMemory();

                assertThat( listener.getMessages() ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );
            }
        } finally {
            Closeables.close( server );
            Closeables.close( client );
        }
        assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );
    }

    @Test
    public void clientPersistence() {
        var listener = new MessageListenerMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener ), -1 ) ) {
            server.start();
            listener.throwUnknownError( 1 );

            MessageSender client;

            client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) );
            try {
                client.start();

                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();
                while( listener.throwUnknownError > 0 )
                    Threads.sleepSafely( 10 );
            } finally {
                client.close();
            }

            assertThat( listener.getMessages() ).isEmpty();
            assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );

            client = new MessageSender( "localhost", server.getPort(), testPath( "tmp" ) );
            try {
                client.start();

                assertThat( listener.getMessages() ).isEmpty();

                client.syncDisk();

                assertThat( listener.getMessages() ).isEqualTo( List.of( new TestMessage( 1, "123" ) ) );
            } finally {
                client.close();
            }

            assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );
        }
    }

    @Test
    public void clientPersistenceLockExpiration() {
        var listener = new MessageListenerMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( listener ), -1 ) ) {
            server.start();

            var msgDirectory = testPath( "tmp" );
            MessageSender client;
            client = new MessageSender( "localhost", server.getPort(), msgDirectory );
            try {
                client.poolSize = 2;
                client.start();

                listener.throwUnknownError = 2;
                client
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "124", ofString() )
                    .syncMemory();

                while( listener.throwUnknownError > 0 )
                    Threads.sleepSafely( 1 );
            } finally {
                client.close();
            }
            assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );

            var files = Files.wildcard( msgDirectory, "**/*.bin" );
            assertThat( files ).hasSize( 2 );
            // lock
            assertNotNull( MessageSender.lock( files.get( 0 ), -1 ) );

            // lock expired
            var lockFile2 = MessageSender.lock( files.get( 1 ), -1 );
            assertNotNull( lockFile2 );
            Files.setLastModifiedTime( lockFile2, DateTimeUtils.currentTimeMillis() - ( Dates.m( 5 ) + Dates.m( 1 ) ) );


            client = new MessageSender( "localhost", server.getPort(), msgDirectory );
            try {
                client.storageLockExpiration = Dates.m( 5 );
                client.start();

                assertThat( listener.getMessages() ).isEmpty();

                client.syncDisk();
                assertThat( listener.getMessages() ).containsExactly( new TestMessage( 1, "124" ) );
            } finally {
                client.close();
            }
            assertThat( client.getMessagesMemorySize() ).isEqualTo( 0L );
        }
    }

    @Test
    public void memoryLimit() {
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( new MessageListenerMock( MESSAGE_TYPE ) ), -1 ) ) {
            server.start();

            MessageSender client;
            client = new MessageSender( "localhost", server.getPort(), testPath( "testMemoryLimit" ) );
            try {
                client.messagesLimitBytes = 161;
                client.memorySyncPeriod = -1;
                client.start();

                client.send( MESSAGE_TYPE, "123", ofString() );
                assertThat( client.availabilityReport( MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

                client.send( MESSAGE_TYPE, "124", ofString() );
                assertThat( client.availabilityReport( MESSAGE_TYPE ).state ).isEqualTo( FAILED );

                client.syncMemory();

                assertThat( client.availabilityReport( MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );
            } finally {
                client.close();
            }
        }
    }

    @Test
    public void availabilityReport() {
        var messageListenerMock = new MessageListenerMock( MESSAGE_TYPE );
        try( var server = new MessageServer( testPath( "controlStatePath.st" ), 0, List.of( messageListenerMock ), -1 ) ) {
            server.start();

            MessageSender client;
            client = new MessageSender( "localhost", server.getPort(), testPath( "testMemoryLimit" ) );
            try {
                client.memorySyncPeriod = -1;
                client.start();

                messageListenerMock.status = 300;
                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();
                assertThat( client.availabilityReport( MESSAGE_TYPE ).state ).isEqualTo( FAILED );
                assertThat( client.availabilityReport( MESSAGE_TYPE2 ).state ).isEqualTo( OPERATIONAL );

                messageListenerMock.status = MessageProtocol.STATUS_OK;
                client.syncMemory();
                assertThat( client.availabilityReport( MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );
                assertThat( client.availabilityReport( MESSAGE_TYPE2 ).state ).isEqualTo( OPERATIONAL );
            } finally {
                client.close();
            }
        }
    }
}
