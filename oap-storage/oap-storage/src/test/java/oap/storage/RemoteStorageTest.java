package oap.storage;

import lombok.ToString;
import oap.http.server.nio.NioHttpServer;
import oap.id.Id;
import oap.id.Identifier;
import oap.remote.FST;
import oap.remote.Remote;
import oap.remote.RemoteInvocationHandler;
import oap.remote.RemoteLocation;
import oap.remote.RemoteServices;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.util.Dates;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteStorageTest extends Fixtures {
    @Test
    public void testUpdate() throws IOException, URISyntaxException {
        int port = Ports.getFreePort( getClass() );

        RemoteServices remoteServices = Mockito.mock( RemoteServices.class );
        MemoryStorage<String, TestRemoteStorage> serverStorage = new MemoryStorage<>( Identifier.forAnnotationFixed(), Storage.Lock.SERIALIZED );

        Mockito.doReturn( serverStorage ).when( remoteServices ).get( "module.service" );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            Remote remote = new Remote( FST.SerializationMethod.DEFAULT, "/remote", remoteServices, server );
            remote.start();

            server.start();

            URI url = new URI( "http://localhost:" + server.defaultPort.httpPort + "/remote" );
            RemoteLocation remoteLocation = new RemoteLocation( url, "module.service", Dates.s( 10 ), FST.SerializationMethod.DEFAULT, 1 );
            RemoteStorage<String, TestRemoteStorage> storage = ( RemoteStorage<String, TestRemoteStorage> ) RemoteInvocationHandler.proxy( "test", remoteLocation, RemoteStorage.class );

            assertThat( storage.store( new TestRemoteStorage( "id1", "v1" ), 0L ) ).isNotNull();
            assertThat( storage.store( new TestRemoteStorage( "id1", "v1" ), 0L ) ).isNull();

            assertThat( storage.findAndModify( "id1", t -> {
                t.v1 = "v2";
                return t;
            }, 10 ) ).isNotNull();

            assertThat( storage.findAndModify( "id2", tnull -> new TestRemoteStorage( "id2", "v2" ), 10 ) ).isNotNull();
        }
    }

    @ToString
    public static class TestRemoteStorage implements Serializable {
        @Id
        public String id;
        public String v1;

        public TestRemoteStorage( String id, String v1 ) {
            this.id = id;
            this.v1 = v1;
        }
    }
}
