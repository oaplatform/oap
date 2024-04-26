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
import oap.message.client.MessageAvailabilityReport.State;
import oap.message.client.MessageSender;
import oap.message.server.MessageHttpHandler;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.io.content.ContentWriter.ofJson;
import static oap.io.content.ContentWriter.ofString;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testng.Assert.assertNotNull;

@Test
public class MessageServerTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public MessageServerTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        fixture( new SystemTimerFixture() );
    }

    @Test
    public void uniqueMessageTypeListener() throws IOException {
        int port = Ports.getFreePort( getClass() );

        var listener1 = new MessageListenerMock( "l1-", MessageListenerMock.MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( "l2-", MessageListenerMock.MESSAGE_TYPE );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", testDirectoryFixture.testPath( "controlStatePath.st" ), List.of( listener1, listener2 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            client.start();
            assertThatCode( messageHttpHandler::preStart )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "duplicate [l2-127, l1-127]" );
        }
    }

    @Test
    public void rejectedException() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

            try( var client1 = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 );
                 var client2 = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

                client1.poolSize = 1;
                client2.poolSize = 1;

                client1.start();
                client2.start();
                messageHttpHandler.preStart();
                server.start();

                client1.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "rejectedException", ofString() )
                    .syncMemory( Dates.s( 10 ) );

                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "rejectedException" ) );

                client2.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "rejectedException", ofString() );
            }

            try( var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
                client.poolSize = 1;

                client.start();

                client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "rejectedException 2", ofString() ).syncDisk()
                    .syncMemory( Dates.s( 10 ) );

                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "rejectedException" ), new TestMessage( 1, "rejectedException 2" ) );
                assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                assertThat( client.getRetryMessages() ).isEqualTo( 0L );
            }
        }
    }

    @Test
    public void sendAndReceive() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE2 );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1, listener2 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceive 1", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceive 2", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceive 2", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceive 1", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE2, ( short ) 1, "sendAndReceive 3", ofString() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "sendAndReceive 1" ),
                new TestMessage( 1, "sendAndReceive 2" ) );
            assertThat( listener2.getMessages() ).containsOnly( new TestMessage( 1, "sendAndReceive 3" ) );

            assertThat( client.getReadyMessages() ).isEqualTo( 0L );
            assertThat( client.getRetryMessages() ).isEqualTo( 0L );

            assertThat( controlStatePath ).doesNotExist();
        }
    }

    @Test
    public void sendAndReceiveJson() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerJsonMock( MessageListenerMock.MESSAGE_TYPE );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJson 1", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJson 2", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJson 2", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJson 1", ofJson() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( listener1.messages ).containsOnly(
                new TestMessage( 1, Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"sendAndReceiveJson 1\"".getBytes( UTF_8 ) ) ), "sendAndReceiveJson 1" ),
                new TestMessage( 1, Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"sendAndReceiveJson 2\"".getBytes( UTF_8 ) ) ), "sendAndReceiveJson 2" )
            );
        }
    }

    @Test
    public void sendAndReceiveJsonOneThread() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerJsonMock( MessageListenerMock.MESSAGE_TYPE );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.poolSize = 1;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJsonOneThread 1", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJsonOneThread 2", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJsonOneThread 2", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJsonOneThread 1", ofJson() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( listener1.messages ).containsOnly(
                new TestMessage( 1, Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"sendAndReceiveJsonOneThread 1\"".getBytes( UTF_8 ) ) ), "sendAndReceiveJsonOneThread 1" ),
                new TestMessage( 1, Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"sendAndReceiveJsonOneThread 2\"".getBytes( UTF_8 ) ) ), "sendAndReceiveJsonOneThread 2" )
            );
        }
    }

    @Test
    public void unknownErrorNoRetry() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.throwUnknownError( Integer.MAX_VALUE, true );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "unknownErrorNoRetry", ofString() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( client.getReadyMessages() ).isEqualTo( 0L );
            assertThat( client.getRetryMessages() ).isEqualTo( 0L );

            assertThat( listener1.getMessages() ).isEmpty();
        }
    }

    @Test
    public void unknownError() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        DateTimeUtils.setCurrentMillisFixed( 100 );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            client.retryTimeout = 100;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.throwUnknownError( 4, false );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "unknownError", ofString() );

            for( int i = 0; i < 5; i++ ) {
                client.syncMemory( Dates.s( 10 ) );
                Dates.incFixed( 100 + 1 );
            }
            assertThat( listener1.throwUnknownError ).isLessThanOrEqualTo( 0 );
            assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "unknownError" ) );

            assertThat( client.getReadyMessages() ).isEqualTo( 0L );
            assertThat( client.getRetryMessages() ).isEqualTo( 0L );
        }
    }

    @Test
    public void statusError() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        DateTimeUtils.setCurrentMillisFixed( 100 );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            client.retryTimeout = 100;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.setStatus( 567 );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "statusError", ofString() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( client.getRetryMessages() ).isEqualTo( 1 );
            assertThat( listener1.getMessages() ).isEmpty();

            listener1.setStatusOk();

            Dates.incFixed( 100 + 1 );

            client.syncMemory( Dates.s( 10 ) );

            assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "statusError" ) );
        }
    }

    @Test
    public void ttl() throws IOException {
        int hashTtl = 1000;
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        DateTimeUtils.setCurrentMillisFixed( 100 );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), hashTtl );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            client.retryTimeout = 100;
            client.globalIoRetryTimeout = 100;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "ttl" ) );

            Dates.incFixed( hashTtl + 1 );
            messageHttpHandler.updateHash();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( listener1.getMessages() ).containsExactly(
                new TestMessage( 1, "ttl" ),
                new TestMessage( 1, "ttl" )
            );
        }
    }

    @Test
    public void persistence() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;
            client.globalIoRetryTimeout = 100;
            client.start();

            try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
                 var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

                server.bind( "/messages", messageHttpHandler );
                messageHttpHandler.preStart();
                server.start();

                client
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .syncMemory( Dates.s( 10 ) );

                assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                assertThat( client.getRetryMessages() ).isEqualTo( 0L );

                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "persistence" ) );

            }

            listener1.reset();

            try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
                 var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

                server.bind( "/messages", messageHttpHandler );
                messageHttpHandler.preStart();
                server.start();

                client
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .syncMemory( Dates.s( 10 ) );

                assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                assertThat( client.getRetryMessages() ).isEqualTo( 0L );

                assertThat( listener1.getMessages() ).isEmpty();
            }
        }
    }

    @Test
    public void clientPersistence() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );
        var listener2 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE2 );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1, listener2 ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            messageHttpHandler.preStart();
            server.start();

            listener1.throwUnknownError( 1, false );

            Path persistenceDirectory = testDirectoryFixture.testPath( "tmp" );

            try( var client = new MessageSender( "localhost", port, "/messages", persistenceDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.start();

                client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 2, "clientPersistence 1", ofString() )
                    .syncMemory( Dates.s( 10 ) );
                client.send( MessageListenerMock.MESSAGE_TYPE2, ( short ) 2, "clientPersistence 2", ofString() );

                assertThat( listener1.getMessages() ).isEmpty();
                assertThat( client.getReadyMessages() ).isEqualTo( 1L );
                assertThat( client.getRetryMessages() ).isEqualTo( 1L );
            }

            assertThat( persistenceDirectory ).isNotEmptyDirectory();

            Files.write( persistenceDirectory.resolve( "a" ), "test", ofString() );
            Files.write( persistenceDirectory.resolve( "a1/b" ), "test", ofString() );
            persistenceDirectory.resolve( "f" ).toFile().mkdirs();
            persistenceDirectory.resolve( "f1/f2" ).toFile().mkdirs();
            persistenceDirectory.resolve( "f1/f2" ).toFile().mkdirs();

            var lockFile = persistenceDirectory
                .resolve( Long.toHexString( 1 ) )
                .resolve( String.valueOf( Byte.toUnsignedInt( MessageListenerMock.MESSAGE_TYPE ) ) )
                .resolve( Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"clientPersistence 1\"".getBytes( UTF_8 ) ) ) + "-2.lock" );

            Files.write( lockFile, "1", ofString() );

            try( var client = new MessageSender( "localhost", port, "/messages", persistenceDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.start();

                assertThat( listener1.getMessages() ).isEmpty();

                client.syncDisk();
                client.syncMemory( Dates.s( 10 ) );

                assertThat( persistenceDirectory ).isEmptyDirectory();

                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 2, "clientPersistence 1" ) );
                assertThat( listener2.getMessages() ).containsOnly( new TestMessage( 2, "clientPersistence 2" ) );
                assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                assertThat( client.getRetryMessages() ).isEqualTo( 0L );
            }
        }
    }

    @Test
    public void clientPersistenceLockExpiration() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        DateTimeUtils.setCurrentMillisFixed( 100 );

        var listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            messageHttpHandler.preStart();
            server.start();

            var msgDirectory = testDirectoryFixture.testPath( "tmp" );
            try( var client = new MessageSender( "localhost", port, "/messages", msgDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.poolSize = 2;
                client.start();

                listener1.throwUnknownError = 2;
                client
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "clientPersistenceLockExpiration 1", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "clientPersistenceLockExpiration 2", ofString() )
                    .syncMemory( Dates.s( 10 ) );
            }

            assertThat( Files.wildcard( msgDirectory, "**/*.bin" ) ).hasSize( 2 );
            var files = Files.wildcard( msgDirectory, "**/*.bin" );

            // lock
            assertNotNull( MessageSender.lock( "clientPersistenceLockExpiration1", files.get( 0 ), -1 ) );

            // lock expired
            var lockFile2 = MessageSender.lock( "clientPersistenceLockExpiration2", files.get( 1 ), -1 );
            assertNotNull( lockFile2 );

            Files.setLastModifiedTime( lockFile2, DateTimeUtils.currentTimeMillis() - ( Dates.m( 5 ) + Dates.m( 1 ) ) );

            try( var client = new MessageSender( "localhost", port, "/messages", msgDirectory, -1 ) ) {
                client.storageLockExpiration = Dates.m( 5 );
                client.start();

                assertThat( listener1.getMessages() ).isEmpty();

                client
                    .syncDisk()
                    .syncMemory( Dates.s( 10 ) );

                assertThat( listener1.getMessages() ).containsExactly( new TestMessage( 1, "clientPersistenceLockExpiration 2" ) );
                assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                assertThat( client.getRetryMessages() ).isEqualTo( 0L );
            }
        }
    }

    @Test
    public void availabilityReport() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        var listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             var messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             var client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.setStatus( 300 );

            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "availabilityReport", ofString() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( State.FAILED );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE2 ).state ).isEqualTo( State.OPERATIONAL );

            listener1.setStatus( MessageProtocol.STATUS_OK );

            client.syncMemory( Dates.s( 10 ) );

            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( State.OPERATIONAL );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE2 ).state ).isEqualTo( State.OPERATIONAL );
        }
    }

    @Test
    public void testKernel() {
        var kernelFixture = new KernelFixture(
            new TestDirectoryFixture(),
            urlOfTestResource( getClass(), "application-message.test.conf" )
        );
        var fixtures = fixtures( kernelFixture );
        try {
            fixtures.fixBeforeMethod();

            kernelFixture.service( "oap-message-client", MessageSender.class )
                .send( ( byte ) 12, ( short ) 1, "testKernel", ofString() )
                .syncMemory( Dates.s( 10 ) );

            assertThat( kernelFixture.service( "oap-message-test", MessageListenerMock.class ).getMessages() )
                .containsExactly( new TestMessage( 1, "testKernel" ) );

        } finally {
            fixtures.fixAfterMethod();
        }
    }
}
