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

import lombok.extern.slf4j.Slf4j;
import oap.application.testng.KernelFixture;
import oap.http.server.nio.NioHttpServer;
import oap.io.Files;
import oap.message.MessageListenerMock.TestMessage;
import oap.message.client.MessageAvailabilityReport;
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
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.io.content.ContentWriter.ofJson;
import static oap.io.content.ContentWriter.ofString;
import static oap.message.client.MessageAvailabilityReport.State.FAILED;
import static oap.message.client.MessageAvailabilityReport.State.OPERATIONAL;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.entry;
import static org.testng.Assert.assertNotNull;

@Slf4j
public class MessageServerTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public MessageServerTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
        fixture( new SystemTimerFixture( true ) );
    }

    @Test
    public void uniqueMessageTypeListener() throws IOException {
        int port = Ports.getFreePort( getClass() );

        MessageListenerMock listener1 = new MessageListenerMock( "l1-", MessageListenerMock.MESSAGE_TYPE );
        MessageListenerMock listener2 = new MessageListenerMock( "l2-", MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", testDirectoryFixture.testPath( "controlStatePath.st" ), List.of( listener1, listener2 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

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

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

            try( MessageSender client1 = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 );
                 MessageSender client2 = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

                client1.start();
                client2.start();
                messageHttpHandler.preStart();
                server.start();

                client1.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "rejectedException", ofString() ).syncMemory();

                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "rejectedException" ) );

                client2.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "rejectedException", ofString() );
            }

            try( MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
                client.start();

                client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "rejectedException 2", ofString() ).syncDisk().syncMemory();

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

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );
        MessageListenerMock listener2 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE2 );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1, listener2 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

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
                .syncMemory();

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

        MessageListenerJsonMock listener1 = new MessageListenerJsonMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJson 1", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJson 2", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJson 2", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJson 1", ofJson() )
                .syncMemory();

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

        MessageListenerJsonMock listener1 = new MessageListenerJsonMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJsonOneThread 1", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJsonOneThread 2", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJsonOneThread 2", ofJson() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendAndReceiveJsonOneThread 1", ofJson() )
                .syncMemory();

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

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.throwUnknownError( Integer.MAX_VALUE, true );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "unknownErrorNoRetry", ofString() )
                .syncMemory();

            assertThat( client.getReadyMessages() ).isEqualTo( 0L );
            assertThat( client.getRetryMessages() ).isEqualTo( 0L );

            assertThat( listener1.getMessages() ).isEmpty();
        }
    }

    @Test
    public void unknownError() throws IOException {
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

            listener1.throwUnknownError( 4, false );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "unknownError", ofString() );

            for( int i = 0; i < 5; i++ ) {
                client.syncMemory();
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
                .syncMemory();

            assertThat( client.getRetryMessages() ).isEqualTo( 1 );
            assertThat( listener1.getMessages() ).isEmpty();

            listener1.setStatusOk();

            Dates.incFixed( 100 + 1 );

            client.syncMemory();

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
                .syncMemory();

            assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "ttl" ) );

            Dates.incFixed( hashTtl + 1 );
            messageHttpHandler.updateHash();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "ttl", ofString() )
                .syncMemory();

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

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;
            client.globalIoRetryTimeout = 100;
            client.start();

            try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
                 MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

                server.bind( "/messages", messageHttpHandler );
                messageHttpHandler.preStart();
                server.start();

                client
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .syncMemory();

                assertThat( client.getReadyMessages() ).isEqualTo( 0L );
                assertThat( client.getRetryMessages() ).isEqualTo( 0L );

                assertThat( listener1.getMessages() ).containsOnly( new TestMessage( 1, "persistence" ) );

            }

            listener1.reset();

            try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
                 MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

                server.bind( "/messages", messageHttpHandler );
                messageHttpHandler.preStart();
                server.start();

                client
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "persistence", ofString() )
                    .syncMemory();

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

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );
        MessageListenerMock listener2 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE2 );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1, listener2 ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            messageHttpHandler.preStart();
            server.start();

            listener1.throwUnknownError( 1, false );

            Path persistenceDirectory = testDirectoryFixture.testPath( "tmp" );

            try( MessageSender client = new MessageSender( "localhost", port, "/messages", persistenceDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.start();

                client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 2, "clientPersistence 1", ofString() )
                    .syncMemory();
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

            Path lockFile = persistenceDirectory
                .resolve( Long.toHexString( 1 ) )
                .resolve( String.valueOf( Byte.toUnsignedInt( MessageListenerMock.MESSAGE_TYPE ) ) )
                .resolve( Hex.encodeHexString( DigestUtils.getMd5Digest().digest( "\"clientPersistence 1\"".getBytes( UTF_8 ) ) ) + "-2.lock" );

            Files.write( lockFile, "1", ofString() );

            try( MessageSender client = new MessageSender( "localhost", port, "/messages", persistenceDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.start();

                assertThat( listener1.getMessages() ).isEmpty();

                client.syncDisk();
                client.syncMemory();

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

        DateTimeUtils.setCurrentMillisFixed( DateTimeUtils.currentTimeMillis() );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            messageHttpHandler.preStart();
            server.start();

            Path msgDirectory = testDirectoryFixture.testPath( "tmp" );
            try( MessageSender client = new MessageSender( "localhost", port, "/messages", msgDirectory, -1 ) ) {
                client.retryTimeout = 100;
                client.start();

                listener1.throwUnknownError = 2;
                client
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "clientPersistenceLockExpiration 1", ofString() )
                    .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "clientPersistenceLockExpiration 2", ofString() )
                    .syncMemory();
            }

            assertThat( Files.wildcard( msgDirectory, "**/*.bin" ) ).hasSize( 2 );
            ArrayList<Path> files = Files.wildcard( msgDirectory, "**/*.bin" );

            // lock
            Path lockFile1 = MessageSender.lock( "clientPersistenceLockExpiration1", files.get( 0 ), -1 );
            assertNotNull( lockFile1 );

            // lock expired
            Path lockFile2 = MessageSender.lock( "clientPersistenceLockExpiration2", files.get( 1 ), -1 );
            assertNotNull( lockFile2 );

            Files.setLastModifiedTime( lockFile1, DateTimeUtils.currentTimeMillis() );
            Files.setLastModifiedTime( lockFile2, DateTimeUtils.currentTimeMillis() - Dates.m( 5 ) - Dates.m( 1 ) );

            try( MessageSender client = new MessageSender( "localhost", port, "/messages", msgDirectory, -1 ) ) {
                client.storageLockExpiration = Dates.m( 5 );
                client.start();

                assertThat( listener1.getMessages() ).isEmpty();

                client
                    .syncDisk()
                    .syncMemory();

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

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;
            client.timeout = Dates.s( 30 );
            client.connectionTimeout = Dates.s( 60 );

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.setStatus( 300 );

            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "availabilityReport", ofString() )
                .syncMemory();

            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( FAILED );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE2 ).state ).isEqualTo( OPERATIONAL );

            listener1.setStatus( MessageProtocol.STATUS_OK );

            Dates.incFixed( 100 + 1 );
            client.syncMemory();

            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE2 ).state ).isEqualTo( OPERATIONAL );
        }
    }

    @Test
    public void availabilityReportNetwork() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;
            client.timeout = Dates.s( 10 );
            client.connectionTimeout = Dates.s( 10 );
            client.networkAvailableMaxErrors = 5;

            server.bind( "/messages", messageHttpHandler );
            listener1.setStatus( MessageProtocol.STATUS_OK );

            messageHttpHandler.preStart();
            server.start();
            client.start();

            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "availabilityReport", ofString() )
                .syncMemory();
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            server.preStop();
            messageHttpHandler.close();

            log.trace( "server -> stop" );

            Dates.incFixed( 100 + 1 );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "availabilityReport2", ofString() )
                .syncMemory();

            for( int i = 0; i < 5; i++ ) {
                assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).describedAs( "retry " + i ).isEqualTo( OPERATIONAL );

                Dates.incFixed( 100 + 1 );
                client.syncMemory();

                log.debug( "errors {}", client.networkAvailable.get() );
            }

            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( FAILED );

            messageHttpHandler.preStart();
            server.start();

            Dates.incFixed( 100 + 1 );
            client.syncMemory();
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );
        }
    }

    @Test
    public void availabilityReportMaxMessages() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;
            client.maxMessages = 2;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "maxMessages 1", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "maxMessages 2", ofString() );

            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "maxMessages 3", ofString() );

            MessageAvailabilityReport report = client.availabilityReport( MessageListenerMock.MESSAGE_TYPE );
            assertThat( report.state ).isEqualTo( FAILED );
            assertThat( report.subsystemStates ).containsOnly(
                entry( MessageSender.FAILURE_IO_STATE, OPERATIONAL ),
                entry( MessageSender.FAILURE_MESSAGES_STATE, FAILED ),
                entry( MessageSender.FAILURE_SHUTDOWN_STATE, OPERATIONAL ) );

            listener1.setStatus( 300 );
            client.syncMemory();

            assertThat( client.getRetryMessages() ).isEqualTo( 3 );
            report = client.availabilityReport( MessageListenerMock.MESSAGE_TYPE );
            assertThat( report.state ).isEqualTo( FAILED );
            assertThat( report.subsystemStates ).containsOnly(
                entry( MessageSender.FAILURE_IO_STATE, FAILED ),
                entry( MessageSender.FAILURE_MESSAGES_STATE, FAILED ),
                entry( MessageSender.FAILURE_SHUTDOWN_STATE, OPERATIONAL ) );

            listener1.setStatus( MessageProtocol.STATUS_OK );
            Dates.incFixed( 100 + 1 );
            client.syncMemory();

            assertThat( listener1.getMessages() ).hasSize( 3 );
            report = client.availabilityReport( MessageListenerMock.MESSAGE_TYPE );
            assertThat( report.state ).isEqualTo( OPERATIONAL );
            assertThat( report.subsystemStates ).isEmpty();
        }
    }

    @Test
    public void sendConcurrent() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.concurrency = 4;

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            List<TestMessage> expected = new ArrayList<>();
            for( int i = 0; i < 100; i++ ) {
                client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "sendConcurrent " + i, ofString() );
                expected.add( new TestMessage( 1, "sendConcurrent " + i ) );
            }

            assertThat( client.syncMemory() ).isTrue();

            assertThat( listener1.getMessages() ).containsExactlyInAnyOrderElementsOf( expected );
            assertThat( client.getReadyMessages() ).isEqualTo( 0 );
            assertThat( client.getRetryMessages() ).isEqualTo( 0 );
            assertThat( client.getInProgressMessages() ).isEqualTo( 0 );
        }
    }

    @Test
    public void priority() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "p0-a", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 5, "p5-a", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 0, "p0-b", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 10, "p10", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 5, "p5-b", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, -1, "p-1", ofString() )
                .syncMemory();

            assertThat( listener1.getMessages() ).containsExactly(
                new TestMessage( 1, "p10" ),
                new TestMessage( 1, "p5-a" ),
                new TestMessage( 1, "p5-b" ),
                new TestMessage( 1, "p0-a" ),
                new TestMessage( 1, "p0-b" ),
                new TestMessage( 1, "p-1" ) );
            assertThat( client.getReadyMessages() ).isEqualTo( 0 );
        }
    }

    @Test
    public void priorityPersistence() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );
        Path directory = testDirectoryFixture.testPath( "tmp" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( MessageSender client = new MessageSender( "localhost", port, "/messages", directory, -1 ) ) {
            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, -3, "p-3", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 7, "p7", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 2, 2, "p2", ofString() );
        } // not sent -> persisted

        // legacy file name, without priority
        byte[] legacy = "legacy".getBytes( UTF_8 );
        Files.write( directory.resolve( "1" ).resolve( String.valueOf( Byte.toUnsignedInt( MessageListenerMock.MESSAGE_TYPE ) ) )
            .resolve( DigestUtils.md5Hex( legacy ) + "-1.bin" ), legacy );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", directory, -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client.syncDisk().syncMemory();

            assertThat( listener1.getMessages() ).containsExactly(
                new TestMessage( 1, "p7" ),
                new TestMessage( 2, "p2" ),
                new TestMessage( 1, "legacy" ),
                new TestMessage( 1, "p-3" ) );
            assertThat( client.getReadyMessages() ).isEqualTo( 0 );
        }
    }

    @Test
    public void spoolNetwork() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;
            client.setSpool( true );

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            server.preStop();
            messageHttpHandler.close();

            // networkAvailableMaxErrors = 2 -> offline after the 3rd error
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "m0", ofString() ).syncMemory();
            for( int i = 0; i < 2; i++ ) {
                Dates.incFixed( 100 + 1 );
                client.syncMemory();
            }
            assertThat( client.networkAvailable.get() ).isEqualTo( 3 );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            // offline -> to the spool
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "m1", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 1 );

            // m0 is spilled, m1 is replayed as the probe
            Dates.incFixed( 100 + 1 );
            client.syncMemory();
            assertThat( client.getSpoolMessages() ).isEqualTo( 2 );
            assertThat( client.getReadyMessages() + client.getRetryMessages() ).isEqualTo( 1 );

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "m2", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "m3", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "m4", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 5 );
            assertThat( client.getReadyMessages() + client.getRetryMessages() ).isEqualTo( 1 );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            messageHttpHandler.preStart();
            server.start();

            // the probe, then the rest
            Dates.incFixed( 100 + 1 );
            client.syncMemory();
            client.syncMemory();

            assertThat( listener1.getMessages() ).containsExactlyInAnyOrder(
                new TestMessage( 1, "m0" ),
                new TestMessage( 1, "m1" ),
                new TestMessage( 1, "m2" ),
                new TestMessage( 1, "m3" ),
                new TestMessage( 1, "m4" ) );
            assertThat( client.getSpoolMessages() ).isEqualTo( 0 );
            assertThat( client.getSpoolSegments() ).isEqualTo( 0 );
            assertThat( client.getSpoolBytes() ).isEqualTo( 0 );
            assertThat( client.getReadyMessages() + client.getRetryMessages() + client.getInProgressMessages() ).isEqualTo( 0 );
            assertThat( segments( testDirectoryFixture.testPath( "tmp" ) ) ).isEmpty();
        }
    }

    @Test
    public void spoolStatusError() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.retryTimeout = 100;
            client.setSpool( true );

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            listener1.setStatus( 300 );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "s1", ofString() ).syncMemory();
            assertThat( client.getRetryMessages() ).isEqualTo( 1 );

            // the last status is an error -> to the spool
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "s2", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 1 );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            Dates.incFixed( 100 + 1 );
            client.syncMemory();
            assertThat( client.getSpoolMessages() ).isEqualTo( 2 );

            listener1.setStatusOk();
            Dates.incFixed( 100 + 1 );
            client.syncMemory();
            client.syncMemory();

            assertThat( listener1.getMessages() ).containsExactlyInAnyOrder( new TestMessage( 1, "s1" ), new TestMessage( 1, "s2" ) );
            assertThat( client.getSpoolMessages() ).isEqualTo( 0 );
            assertThat( client.getSpoolSegments() ).isEqualTo( 0 );
        }
    }

    @Test
    public void spoolMaxMessages() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.maxMessages = 2;
            client.setSpool( true );

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            for( int i = 0; i < 5; i++ ) client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "mm" + i, ofString() );

            assertThat( client.getReadyMessages() ).isEqualTo( 2 );
            assertThat( client.getSpoolMessages() ).isEqualTo( 3 );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            client.syncMemory();

            assertThat( listener1.getMessages() ).hasSize( 5 );
            assertThat( client.getSpoolMessages() ).isEqualTo( 0 );
        }
    }

    @Test
    public void spoolPriority() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.setSpool( true );

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client.networkAvailable.set( 10 ); // offline

            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "p0-a", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 5, "p5-a", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 0, "p0-b", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 10, "p10", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 5, "p5-b", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, -1, "p-1", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 6 );
            assertThat( client.getSpoolSegments() ).isEqualTo( 4 );

            client.syncMemory(); // the probe
            client.syncMemory();

            assertThat( listener1.getMessages() ).containsExactly(
                new TestMessage( 1, "p10" ),
                new TestMessage( 1, "p5-a" ),
                new TestMessage( 1, "p5-b" ),
                new TestMessage( 1, "p0-a" ),
                new TestMessage( 1, "p0-b" ),
                new TestMessage( 1, "p-1" ) );
            assertThat( client.getSpoolMessages() ).isEqualTo( 0 );
        }
    }

    @Test
    public void spoolLimitSize() throws IOException {
        int port = Ports.getFreePort( getClass() );

        try( MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.setSpool( true );
            client.setSpoolMaxSize( 1 );
            client.networkAvailable.set( 10 );

            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "l1", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 1 );
            assertThat( client.getSpoolLimit() ).isEqualTo( MessageSender.SpoolLimit.SIZE );

            // nothing is dropped: the old behaviour
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "l2", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 1 );
            assertThat( client.getReadyMessages() ).isEqualTo( 1 );

            MessageAvailabilityReport report = client.availabilityReport( MessageListenerMock.MESSAGE_TYPE );
            assertThat( report.state ).isEqualTo( FAILED );
            assertThat( report.subsystemStates ).containsOnly(
                entry( MessageSender.FAILURE_IO_STATE, FAILED ),
                entry( MessageSender.FAILURE_MESSAGES_STATE, OPERATIONAL ),
                entry( MessageSender.FAILURE_SHUTDOWN_STATE, OPERATIONAL ),
                entry( MessageSender.FAILURE_SPOOL_STATE, FAILED ) );

            // runtime change
            client.setSpoolMaxSize( 0 );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );
        }
    }

    @Test
    public void spoolLimitAge() throws IOException {
        int port = Ports.getFreePort( getClass() );

        try( MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.setSpool( true );
            client.setSpoolMaxAge( Dates.s( 10 ) );
            client.networkAvailable.set( 10 );

            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "a1", ofString() );
            assertThat( client.getSpoolLimit() ).isEqualTo( MessageSender.SpoolLimit.NONE );

            Dates.incFixed( Dates.s( 5 ) );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "a2", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 2 );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            Dates.incFixed( Dates.s( 5 ) );
            assertThat( client.getSpoolLimit() ).isEqualTo( MessageSender.SpoolLimit.AGE );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).subsystemStates )
                .contains( entry( MessageSender.FAILURE_SPOOL_STATE, FAILED ) );

            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "a3", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 2 );
            assertThat( client.getReadyMessages() ).isEqualTo( 1 );
        }
    }

    @Test
    public void spoolLimitFreeSpace() throws IOException {
        int port = Ports.getFreePort( getClass() );

        try( MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.setSpool( true );
            client.setSpoolMinFreeSpace( Long.MAX_VALUE );
            client.networkAvailable.set( 10 );

            assertThat( client.getSpoolLimit() ).isEqualTo( MessageSender.SpoolLimit.FREE_SPACE );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "f1", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 0 );
            assertThat( client.getReadyMessages() ).isEqualTo( 1 );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( FAILED );

            client.setSpoolMinFreeSpace( 1 );
            assertThat( client.getSpoolLimit() ).isEqualTo( MessageSender.SpoolLimit.NONE );
        }
    }

    @Test
    public void spoolNoLimits() throws IOException {
        int port = Ports.getFreePort( getClass() );

        try( MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.maxMessages = 2;
            client.setSpool( true );
            client.networkAvailable.set( 10 );

            for( int i = 0; i < 100; i++ ) client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "n" + i, ofString() );

            assertThat( client.getSpoolMessages() ).isEqualTo( 100 );
            assertThat( client.getReadyMessages() ).isEqualTo( 0 );
            assertThat( client.availabilityReport( MessageListenerMock.MESSAGE_TYPE ).state ).isEqualTo( OPERATIONAL );

            // runtime switch: the old rules
            client.setSpool( false );
            MessageAvailabilityReport report = client.availabilityReport( MessageListenerMock.MESSAGE_TYPE );
            assertThat( report.state ).isEqualTo( FAILED );
            assertThat( report.subsystemStates ).containsOnly(
                entry( MessageSender.FAILURE_IO_STATE, FAILED ),
                entry( MessageSender.FAILURE_MESSAGES_STATE, OPERATIONAL ),
                entry( MessageSender.FAILURE_SHUTDOWN_STATE, OPERATIONAL ) );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "memory", ofString() );
            assertThat( client.getReadyMessages() ).isEqualTo( 1 );
        }
    }

    @Test
    public void spoolRuntimeSwitch() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", testDirectoryFixture.testPath( "tmp" ), -1 ) ) {
            client.setSpool( true );

            server.bind( "/messages", messageHttpHandler );
            client.start();
            messageHttpHandler.preStart();
            server.start();

            client.networkAvailable.set( 10 );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "r1", ofString() );
            assertThat( client.getSpoolMessages() ).isEqualTo( 1 );

            client.setSpool( false );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "r2", ofString() );
            assertThat( client.getReadyMessages() ).isEqualTo( 1 );

            // the backlog is replayed with the spool disabled
            client.syncMemory();
            client.syncMemory();

            assertThat( listener1.getMessages() ).containsExactlyInAnyOrder( new TestMessage( 1, "r1" ), new TestMessage( 1, "r2" ) );
            assertThat( client.getSpoolMessages() ).isEqualTo( 0 );
        }
    }

    @Test
    public void spoolRestart() throws IOException {
        int port = Ports.getFreePort( getClass() );
        Path controlStatePath = testDirectoryFixture.testPath( "controlStatePath.st" );
        Path directory = testDirectoryFixture.testPath( "tmp" );

        MessageListenerMock listener1 = new MessageListenerMock( MessageListenerMock.MESSAGE_TYPE );

        try( MessageSender client = new MessageSender( "localhost", port, "/messages", directory, -1 ) ) {
            client.setSpool( true );
            client.networkAvailable.set( 10 );
            client
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "x1", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 2, "x2", ofString() )
                .send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, 3, "x3", ofString() );
            client.networkAvailable.set( 0 );
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "x4", ofString() ); // memory -> spool on close
            client.send( MessageListenerMock.MESSAGE_TYPE, ( short ) 1, "torn", ofString() );
        }

        List<Path> segments = segments( directory );
        assertThat( segments ).hasSize( 2 );

        // a torn tail: the last record of priority 0 is cut
        Path p0 = segments.stream().filter( p -> p.getParent().getFileName().toString().equals( "p0" ) ).findFirst().orElseThrow();
        try( var channel = java.nio.channels.FileChannel.open( p0, java.nio.file.StandardOpenOption.WRITE ) ) {
            channel.truncate( channel.size() - 3 );
        }

        try( NioHttpServer server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) );
             MessageHttpHandler messageHttpHandler = new MessageHttpHandler( server, "/messages", controlStatePath, List.of( listener1 ), -1 );
             MessageSender client = new MessageSender( "localhost", port, "/messages", directory, -1 ) ) {

            server.bind( "/messages", messageHttpHandler );
            client.start(); // adopts the spool of the previous sender
            messageHttpHandler.preStart();
            server.start();

            // the header scan does not count the torn record
            assertThat( client.getSpoolMessages() ).isEqualTo( 4 );

            client.syncMemory();

            assertThat( listener1.getMessages() ).containsExactly(
                new TestMessage( 1, "x3" ),
                new TestMessage( 1, "x1" ),
                new TestMessage( 2, "x2" ),
                new TestMessage( 1, "x4" ) );
            assertThat( client.getSpoolMessages() ).isEqualTo( 0 );
            assertThat( client.getSpoolSegments() ).isEqualTo( 0 );
        }

        assertThat( segments( directory ) ).isEmpty();
    }

    private static List<Path> segments( Path directory ) throws IOException {
        Path spool = directory.resolve( MessageSender.SPOOL_DIRECTORY );
        if( !java.nio.file.Files.exists( spool ) ) return List.of();
        try( var stream = java.nio.file.Files.walk( spool ) ) {
            return stream.filter( p -> p.toString().endsWith( ".seg" ) ).sorted().toList();
        }
    }

    @Test
    public void testKernel() {
        KernelFixture kernelFixture = new KernelFixture(
            new TestDirectoryFixture(),
            urlOfTestResource( getClass(), "application-message.test.conf" )
        );
        Fixtures fixtures = fixtures( kernelFixture );
        try {
            fixtures.fixBeforeMethod();

            kernelFixture.service( "oap-message-client", MessageSender.class )
                .send( ( byte ) 12, ( short ) 1, "testKernel", ofString() )
                .syncMemory();

            assertThat( kernelFixture.service( "oap-message-test", MessageListenerMock.class ).getMessages() )
                .containsExactly( new TestMessage( 1, "testKernel" ) );

        } finally {
            fixtures.fixAfterMethod();
        }
    }
}
