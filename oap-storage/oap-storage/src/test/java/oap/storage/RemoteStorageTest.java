package oap.storage;

import lombok.ToString;
import oap.application.Kernel;
import oap.application.remote.FST;
import oap.application.remote.Remote;
import oap.application.remote.RemoteInvocationHandler;
import oap.application.remote.RemoteLocation;
import oap.http.server.nio.NioHttpServer;
import oap.id.Id;
import oap.id.Identifier;
import oap.testng.Fixtures;
import oap.testng.Ports;
import oap.util.Dates;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteStorageTest extends Fixtures {
    @Test
    public void testUpdate() throws IOException, URISyntaxException {
        int port = Ports.getFreePort();

        Kernel kernel = Mockito.mock( Kernel.class );
        MemoryStorage<String, TestRemoteStorage> serverStorage = new MemoryStorage<>( Identifier.forAnnotationFixed(), Storage.Lock.SERIALIZED );

        Mockito.doReturn( Optional.of( serverStorage ) ).when( kernel ).service( "module.service" );

        try( var server = new NioHttpServer( new NioHttpServer.DefaultPort( port ) ) ) {
            Remote remote = new Remote( FST.SerializationMethod.DEFAULT, "/remote", kernel, server );

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
