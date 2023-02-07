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

import oap.application.testng.KernelFixture;
import oap.http.server.nio.NioHttpServer;
import oap.io.Files;
import oap.message.MessageListenerMock.TestMessage;
import oap.testng.EnvFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.io.content.ContentWriter.ofJson;
import static oap.io.content.ContentWriter.ofString;
import static oap.message.MessageAvailabilityReport.State.FAILED;
import static oap.message.MessageAvailabilityReport.State.OPERATIONAL;
import static oap.message.MessageListenerMock.MESSAGE_TYPE;
import static oap.message.MessageListenerMock.MESSAGE_TYPE2;
import static oap.message.MessageListenerMock.MESSAGE_TYPE3;

import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.urlOfTestResource;
import static oap.testng.TestDirectoryFixture.testPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testng.Assert.assertNotNull;

public class MessageServerTest extends Fixtures {
    private final EnvFixture envFixture;

    public MessageServerTest() {
        fixture( TestDirectoryFixture.FIXTURE );
        envFixture = fixture( new EnvFixture() );
    }

    @BeforeMethod
    public void beforeMethod() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void uniqueMessageTypeListener() throws IOException {
        int port = envFixture.portFor( getClass() );

        var listener1 = new MessageListenerMock( "l1-", MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( "l2-", MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", testPath( "controlStatePath.st" ), List.of( listener1, listener2 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {

            client.start();
            assertThatCode( messageHttpHandler::preStart )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "duplicate [l2-127, l1-127]" );
        }
    }

    @Test
    public void rejectedException() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerJsonMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

            try( var client1 = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 );
                 var client2 = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {

                client1.poolSize = 1;
                client2.poolSize = 1;

                client1.start();
                client2.start();
                messageHttpHandler.preStart();
                server.start();

                client1.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();
                client2.send( MESSAGE_TYPE, "123", ofString() );

                assertEventually( 50, 100, () -> {
                    assertThat( listener1.messages ).containsOnly( new TestMessage( 1, "123" ) );
                } );
            }

            try( var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {
                client.poolSize = 1;

                client.start();

                client.send( MESSAGE_TYPE, "1234", ofString() ).syncDisk().syncMemory();

                assertEventually( 50, 100, () -> {
                    assertThat( listener1.messages ).containsOnly( new TestMessage( 1, "123" ), new TestMessage( 1, "1234" ) );
                    assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                    assertThat( client.getRetryMessages() ).isEqualTo( 0L );
                } );
            }
        }
    }

    @Test
    public void sendAndReceive() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( MESSAGE_TYPE2 );
        var listener3 = new MessageListenerMock( MESSAGE_TYPE3 );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1, listener2, listener3 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            server.bind( "/logs", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MESSAGE_TYPE, "123", ofString() )
                .send( MESSAGE_TYPE, "124", ofString() )
                .send( MESSAGE_TYPE, "124", ofString() )
                .send( MESSAGE_TYPE, "123", ofString() )
                .send( MESSAGE_TYPE2, "555", ofString() )
                .send( MESSAGE_TYPE3, "666", ofString() )
                .syncMemory();

            assertEventually( 100, 50, () -> {
                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "123" ), new TestMessage( 1, "124" ) );
                assertThat( listener2.getMessages() ).containsOnly( new TestMessage( 1, "555" ) );
                assertThat( listener3.getMessages() ).containsOnly( new TestMessage( 1, "666" ) );
            } );
            assertThat( client.getReadyMessages() ).isEqualTo( 0L );
            assertThat( client.getRetryMessages() ).isEqualTo( 0L );

            assertThat( controlStatePath ).doesNotExist();
        }
    }

    @Test
    public void sendAndReceiveJson() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerJsonMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MESSAGE_TYPE, "123", ofJson() )
                .send( MESSAGE_TYPE, "124", ofJson() )
                .send( MESSAGE_TYPE, "124", ofJson() )
                .send( MESSAGE_TYPE, "123", ofJson() )
                .syncMemory();

            assertEventually( 100, 50, () ->
                assertThat( listener1.messages ).containsOnly(
                    new TestMessage( 1, Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"123\"".getBytes( UTF_8 ) ) ), "123" ),
                    new TestMessage( 1, Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"124\"".getBytes( UTF_8 ) ) ), "124" )
                )
            );
        }
    }

    @Test
    public void sendAndReceiveJsonOneThread() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerJsonMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {
            client.poolSize = 1;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MESSAGE_TYPE, "123", ofJson() )
                .send( MESSAGE_TYPE, "124", ofJson() )
                .send( MESSAGE_TYPE, "124", ofJson() )
                .send( MESSAGE_TYPE, "123", ofJson() )
                .syncMemory();

            assertEventually( 50, 100, () -> {
                assertThat( listener1.messages ).containsOnly(
                    new TestMessage( 1, Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"123\"".getBytes( UTF_8 ) ) ), "123" ),
                    new TestMessage( 1, Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"124\"".getBytes( UTF_8 ) ) ), "124" )
                );
            } );
        }
    }

    @Test
    public void unknownErrorNoRetry() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.throwUnknownError( Integer.MAX_VALUE, true );
            client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();

            assertEventually( 100, 50, () -> {
                assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                assertThat( client.getRetryMessages() ).isEqualTo( 0L );
            } );

            assertThat( listener1.getMessages() ).isEmpty();
        }
    }

    @Test
    public void unknownError() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {

            client.retryTimeout = 100;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.throwUnknownError( 4, false );
            client.send( MESSAGE_TYPE, "123", ofString() );

            assertEventually( 100, 50, () -> {
                client.syncMemory();
                assertThat( listener1.throwUnknownError ).isLessThanOrEqualTo( 0 );
                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "123" ) );
            } );

            assertThat( client.getReadyMessages() ).isEqualTo( 0L );
            assertThat( client.getRetryMessages() ).isEqualTo( 0L );
        }
    }

    @Test
    public void statusError() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {

            client.retryTimeout = 100;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.setStatus( 567 );
            client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();

            assertEventually( 100, 50, () -> {
                assertThat( client.getRetryMessages() ).isEqualTo( 1 );
                assertThat( listener1.getMessages() ).isEmpty();
            } );

            listener1.setStatusOk();

            assertEventually( 10, 50, () -> {
                client.syncMemory();

                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "123" ) );
            } );
        }
    }

    @Test
    public void ttl() throws IOException {
        var hashTtl = 1000;
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), hashTtl );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {

            client.retryTimeout = 100;
            client.globalIoRetryTimeout = 100;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MESSAGE_TYPE, "123", ofString() )
                .send( MESSAGE_TYPE, "123", ofString() )
                .send( MESSAGE_TYPE, "123", ofString() )
                .syncMemory();

            MessageSenderUtils.waitSendAll( client, Dates.s( 10 ), 10 );
            assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "123" ) );

            DateTimeUtils.setCurrentMillisFixed( DateTimeUtils.currentTimeMillis() + hashTtl + 1 );
            messageHttpHandler.updateHash();

            client
                .send( MESSAGE_TYPE, "123", ofString() )
                .send( MESSAGE_TYPE, "123", ofString() )
                .send( MESSAGE_TYPE, "123", ofString() )
                .syncMemory();

            MessageSenderUtils.waitSendAll( client, Dates.s( 10 ), 10 );
            assertThat( listener1.getMessages() ).containsExactly(
                new TestMessage( 1, "123" ),
                new TestMessage( 1, "123" )
            );
        }
    }

    @Test( enabled = false ) //flaky test
    public void persistence() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );

        try( var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;
            client.globalIoRetryTimeout = 100;
            client.start();

            try( var server = new NioHttpServer( port );
                 var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

                server.bind( "/messages", messageHttpHandler );
                messageHttpHandler.preStart();
                server.start();

                client
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .syncMemory();

                assertEventually( 100, 50, () -> {
                    assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                    assertThat( client.getRetryMessages() ).isEqualTo( 0L );

                    assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "123" ) );
                } );

            }

            listener1.reset();

            try( var server = new NioHttpServer( port );
                 var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

                server.bind( "/messages", messageHttpHandler );
                messageHttpHandler.preStart();
                server.start();

                client
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .syncMemory();

                assertEventually( 100, 50, () -> {
                    assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                    assertThat( client.getRetryMessages() ).isEqualTo( 0L );

                    assertThat( listener1.getMessages() ).isEmpty();
                } );
            }
        }
    }

    @Test
    public void clientPersistence() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( MESSAGE_TYPE2 );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1, listener2 ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            messageHttpHandler.preStart();
            server.start();

            listener1.throwUnknownError( 1, false );

            Path persistenceDirectory = testPath( "tmp" );

            try( var client = new MessageSender( "localhost", port, "/messages", persistenceDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.start();

                client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();
                client.send( MESSAGE_TYPE2, "1234", ofString() );

                assertEventually( 100, 50, () -> {
                    assertThat( listener1.getMessages() ).isEmpty();
                    assertThat( client.getReadyMessages() ).isEqualTo( 1L );
                    assertThat( client.getRetryMessages() ).isEqualTo( 1L );
                } );
            }

            assertThat( persistenceDirectory ).isNotEmptyDirectory();

            Files.write( persistenceDirectory.resolve( "a" ), "test", ofString() );
            Files.write( persistenceDirectory.resolve( "a1/b" ), "test", ofString() );
            persistenceDirectory.resolve( "f" ).toFile().mkdirs();
            persistenceDirectory.resolve( "f1/f2" ).toFile().mkdirs();
            persistenceDirectory.resolve( "f1/f2" ).toFile().mkdirs();

            var lockFile = persistenceDirectory
                .resolve( Long.toHexString( 1 ) )
                .resolve( String.valueOf( Byte.toUnsignedInt( MESSAGE_TYPE ) ) )
                .resolve( Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"123\"".getBytes( UTF_8 ) ) ) + ".lock" );

            Files.write( lockFile, "1", ofString() );

            try( var client = new MessageSender( "localhost", port, "/messages", persistenceDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.start();

                assertThat( listener1.getMessages() ).isEmpty();

                client.syncDisk();
                client.syncMemory();

                assertThat( persistenceDirectory ).isEmptyDirectory();

                assertEventually( 100, 50, () -> {
                    assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "123" ) );
                    assertThat( listener2.getMessages() ).containsOnly( new TestMessage( 1, "1234" ) );
                    assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                    assertThat( client.getRetryMessages() ).isEqualTo( 0L );
                } );
            }
        }
    }

    @Test
    public void clientPersistenceLockExpiration() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            messageHttpHandler.preStart();
            server.start();

            var msgDirectory = testPath( "tmp" );
            try( var client = new MessageSender( "localhost", port, "/messages", msgDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.poolSize = 2;
                client.start();

                listener1.throwUnknownError = 2;
                client
                    .send( MESSAGE_TYPE, "123", ofString() )
                    .send( MESSAGE_TYPE, "124", ofString() )
                    .syncMemory();
            }

            assertThat( Files.wildcard( msgDirectory, "**/*.bin" ) ).hasSize( 2 );
            var files = Files.wildcard( msgDirectory, "**/*.bin" );

            // lock
            assertNotNull( MessageSender.lock( files.get( 0 ), -1 ) );

            // lock expired
            var lockFile2 = MessageSender.lock( files.get( 1 ), -1 );
            assertNotNull( lockFile2 );

            Files.setLastModifiedTime( lockFile2, DateTimeUtils.currentTimeMillis() - ( Dates.m( 5 ) + Dates.m( 1 ) ) );

            try( var client = new MessageSender( "localhost", port, "/messages", msgDirectory, -1 ) ) {
                client.storageLockExpiration = Dates.m( 5 );
                client.start();

                assertThat( listener1.getMessages() ).isEmpty();

                client
                    .syncDisk()
                    .syncMemory();

                assertEventually( 50, 100, () -> {
                    assertThat( listener1.getMessages() ).containsExactly( new TestMessage( 1, "124" ) );
                    assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                    assertThat( client.getRetryMessages() ).isEqualTo( 0L );
                } );
            }
        }
    }

    @Test
    public void availabilityReport() throws IOException {
        int port = envFixture.portFor( getClass() );
        Path controlStatePath = testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MESSAGE_TYPE );

        try( var server = new NioHttpServer( port );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.setStatus( 300 );

            client.send( MESSAGE_TYPE, "123", ofString() ).syncMemory();

            assertEventually( 50, 100, () -> {
                assertThat( client.availabilityReport( MESSAGE_TYPE ).state ).isEqualTo( FAILED );
                assertThat( client.availabilityReport( MESSAGE_TYPE2 ).state ).isEqualTo( OPERATIONAL );
            } );

            listener1.setStatus( MessageProtocol.STATUS_OK );

            assertEventually( 50, 100, () -> {
                client.syncMemory();

                assertThat( client.availabilityReport( MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );
                assertThat( client.availabilityReport( MESSAGE_TYPE2 ).state ).isEqualTo( OPERATIONAL );
            } );
        }
    }

    @Test
    public void testKernel() {
        var kernelFixture = new KernelFixture(
            urlOfTestResource( getClass(), "application.test.conf" ),
            List.of( urlOfTestResource( getClass(), "oap-module.conf" ) )
        );
        try {
            kernelFixture.beforeMethod();

            kernelFixture.service( "oap", MessageSender.class ).send( ( byte ) 12, "123", ofString() );

            assertEventually( 50, 100, () -> {
                assertThat( kernelFixture.service( "oap-message-test", MessageListenerMock.class ).getMessages() )
                    .containsExactly( new TestMessage( 1, "123" ) );
            } );

        } finally {
            kernelFixture.afterMethod();
        }
    }
}
