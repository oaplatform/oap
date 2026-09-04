package oap.storage.cloud;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.io.IoStreams;
import oap.io.content.ContentReader;
import oap.io.content.ContentWriter;
import oap.testng.AbstractFixture;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ThrowingFunction;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;

/**
 * Starts a <a href="https://hub.docker.com/r/dockurr/samba">dockurr/samba</a> container. {@link #container()}
 * returns {@code host:port/}{@link #SHARE}, which is what {@code CloudURI}'s {@code container} field must be
 * for this fixture's share. All fixture I/O (seeding/reading/reset) goes through the Docker Engine API
 * (copy/exec), not a host bind mount — a bind mount's host path must resolve on the Docker daemon's side too,
 * which breaks when the test JVM and daemon don't share a filesystem (e.g. Docker-in-Docker on Linux CI).
 */
@Slf4j
public class SambaServerFixture extends AbstractFixture<SambaServerFixture> {
    public static final String SHARE = "shared";
    public static final String USERNAME = "smb-test-user";
    public static final String PASSWORD = "smb-test-password";
    private static final String VERSION = "4.22.6";
    @Getter
    private final int port;
    private GenericContainer<?> container;

    public SambaServerFixture() {
        port = definePort( "SMB_PORT" );
    }

    @Override
    protected void before() {
        super.before();

        PortBinding portBinding = new PortBinding(
            Ports.Binding.bindPort( port ),
            new ExposedPort( 445 ) );

        container = new GenericContainer<>( DockerImageName.parse( s( "dockurr/samba:${VERSION}" ) ) )
            .withExposedPorts( 445 )
            .withEnv( "NAME", SHARE )
            .withEnv( "USER", USERNAME )
            .withEnv( "PASS", PASSWORD )
            .withEnv( "RW", "true" )
            .withCreateContainerCmdModifier( cmd -> cmd.getHostConfig().withPortBindings( portBinding ) )
            .withLogConsumer( new Slf4jLogConsumer( log ) );
        container.start();
    }

    @Override
    protected void after() {
        if( container != null ) {
            container.stop();
        }

        super.after();
    }

    public String hostPort() {
        return s( "localhost:${port}" );
    }

    public String container() {
        return hostPort() + "/" + SHARE;
    }

    public FileSystemConfiguration getFileSystemConfiguration() {
        return new FileSystemConfiguration( getFileSystemConfigurationMap( true ) );
    }

    public Map<String, Object> getFileSystemConfigurationMap( boolean addDefaults ) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put( "fs.smb.clouds.identity", USERNAME );
        map.put( "fs.smb.clouds.credential", PASSWORD );

        if( addDefaults ) {
            map.put( "fs.default.clouds.scheme", "smb" );
            map.put( "fs.default.clouds.container", container() );
        } else {
            map.put( "fs.smb.clouds.container", container() );
        }

        return map;
    }

    public FileSystemConfiguration updateWithSmb( FileSystemConfiguration fileSystemConfiguration, boolean addDefaults ) {
        return fileSystemConfiguration.copyWith( getFileSystemConfigurationMap( addDefaults ) );
    }

    /**
     * Wipes the share's content. Call between tests to isolate state.
     */
    public void reset() {
        exec( "sh", "-c", "rm -rf /storage/*" );
    }

    public <T> void writeFile( String relativePath, T content, ContentWriter<T> contentWriter ) {
        container.copyFileToContainer( Transferable.of( contentWriter.write( content ) ), "/storage/" + relativePath );
        openUpPermissions();
    }

    public void createDirectory( String relativePath ) {
        exec( "mkdir", "-p", "/storage/" + relativePath );
        openUpPermissions();
    }

    /**
     * docker cp / exec run as root, so anything they create is root-owned; smbd operates as
     * {@link #USERNAME} (via smb.conf's `force user`), which then can't write/delete inside a
     * root-owned directory. Reopen permissions after every host-side mutation.
     */
    private void openUpPermissions() {
        exec( "chmod", "-R", "0777", "/storage" );
    }

    public <T> T readFile( String relativePath, ContentReader<T> contentReader ) {
        return copyFrom( relativePath, contentReader::read );
    }

    public <T> T readFile( String relativePath, IoStreams.Encoding encoding, ContentReader<T> contentReader ) {
        return copyFrom( relativePath, is -> contentReader.read( IoStreams.in( is, encoding ) ) );
    }

    private <T> T copyFrom( String relativePath, ThrowingFunction<InputStream, T> function ) {
        return container.copyFileFromContainer( "/storage/" + relativePath, function );
    }

    private void exec( String... command ) {
        try {
            container.execInContainer( command );
        } catch( IOException | InterruptedException e ) {
            throw new CloudException( e );
        }
    }
}
