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
 * returns {@code host:port/}{@link #SHARE}, which is what {@code fs.smb.container[.<configurationId>]} must be set to
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
        return s( "${hostPort()}/${SHARE}" );
    }

    public FileSystemConfiguration getFileSystemConfiguration( String configurationId ) {
        return new FileSystemConfiguration( getFileSystemConfigurationMap( configurationId ) );
    }

    /**
     * Builds config for this share under `configurationId` — always registers `configurationId` via `container.<configurationId>`
     * (and identity/credential alongside it), whether it's the default target or a secondary one layered
     * onto an existing {@link FileSystemConfiguration} via {@link #updateWithSmb}.
     */
    public Map<String, Object> getFileSystemConfigurationMap( String configurationId ) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put( s( "fs.smb.identity.${configurationId}" ), USERNAME );
        map.put( s( "fs.smb.credential.${configurationId}" ), PASSWORD );
        map.put( s( "fs.smb.container.${configurationId}" ), container() );

        return map;
    }

    public FileSystemConfiguration updateWithSmb( FileSystemConfiguration fileSystemConfiguration, String configurationId ) {
        return fileSystemConfiguration.copyWith( getFileSystemConfigurationMap( configurationId ) );
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
        exec( "mkdir", "-p", s( "/storage/${relativePath}" ) );
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
