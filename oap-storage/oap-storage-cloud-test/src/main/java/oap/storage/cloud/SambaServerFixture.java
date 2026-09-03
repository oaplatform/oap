package oap.storage.cloud;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.IoStreams;
import oap.io.content.ContentReader;
import oap.io.content.ContentWriter;
import oap.testng.AbstractFixture;
import oap.testng.TestDirectoryFixture;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;

/**
 * Starts a <a href="https://hub.docker.com/r/dockurr/samba">dockurr/samba</a> container and shares
 * a fixture-managed host directory. {@link #container()} returns {@code host:port/}{@link #SHARE},
 * which is what {@code CloudURI}'s {@code container} field must be for this fixture's share.
 */
@Slf4j
public class SambaServerFixture extends AbstractFixture<SambaServerFixture> {
    private static final String VERSION = "4.22.6";
    public static final String SHARE = "shared";
    public static final String USERNAME = "smb-test-user";
    public static final String PASSWORD = "smb-test-password";

    @Getter
    private final int port;
    private final TestDirectoryFixture testDirectoryFixture;
    private GenericContainer<?> container;

    public SambaServerFixture( TestDirectoryFixture testDirectoryFixture ) {
        this.testDirectoryFixture = testDirectoryFixture;

        port = definePort( "SMB_PORT" );

        addChild( testDirectoryFixture );
    }

    public SambaServerFixture() {
        this( new TestDirectoryFixture( "-smb" ) );
    }

    @Override
    protected void before() {
        super.before();

        PortBinding portBinding = new PortBinding(
            Ports.Binding.bindPort( port ),
            new ExposedPort( 445 ) );

        container = new GenericContainer<>( DockerImageName.parse( "dockurr/samba:" + VERSION ) )
            .withExposedPorts( 445 )
            .withEnv( "NAME", SHARE )
            .withEnv( "USER", USERNAME )
            .withEnv( "PASS", PASSWORD )
            // dockurr/samba only uses /shared to auto-detect UID/GID at startup; smbd actually
            // serves /storage (see its smb.conf `[shared] path = /storage`), so that's what must
            // be bind-mounted for the share content to be visible on the host and vice versa.
            .withFileSystemBind( testDirectoryFixture.testDirectory().toAbsolutePath().toString(), "/storage", BindMode.READ_WRITE )
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

    public Path homeDirectory() {
        return testDirectoryFixture.testDirectory();
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

    public Path resolve( String relativePath ) {
        return homeDirectory().resolve( relativePath );
    }

    public <T> void writeFile( String relativePath, T content, ContentWriter<T> contentWriter ) {
        Files.write( resolve( relativePath ), content, contentWriter );
    }

    public void createDirectory( String relativePath ) {
        Files.ensureDirectory( resolve( relativePath ) );
    }

    public <T> T readFile( String relativePath, ContentReader<T> contentReader ) {
        return Files.read( resolve( relativePath ), contentReader );
    }

    public <T> T readFile( String relativePath, IoStreams.Encoding encoding, ContentReader<T> contentReader ) {
        return Files.read( resolve( relativePath ), encoding, contentReader );
    }
}
