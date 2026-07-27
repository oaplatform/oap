package oap.storage.cloud;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.content.ContentReader;
import oap.io.content.ContentWriter;
import oap.testng.AbstractFixture;
import oap.testng.TestDirectoryFixture;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
public class FtpFixture extends AbstractFixture<FtpFixture> {
    public static final String USERNAME = "ftp-test-user";
    public static final String PASSWORD = "ftp-test-password";
    public static final String KEYSTORE_PASSWORD = "changeit";

    @Getter
    private final int port;
    private final TestDirectoryFixture testDirectoryFixture;
    private boolean tls = false;
    private boolean implicitTls = false;
    private FtpServer ftpServer;

    public FtpFixture( TestDirectoryFixture testDirectoryFixture ) {
        this.testDirectoryFixture = testDirectoryFixture;

        port = definePort( "FTP_PORT" );

        addChild( testDirectoryFixture );
    }

    public FtpFixture() {
        this( new TestDirectoryFixture( "-ftp" ) );
    }

    public FtpFixture withTls() {
        return withTls( false );
    }

    public FtpFixture withTls( boolean implicit ) {
        this.tls = true;
        this.implicitTls = implicit;

        return this;
    }

    @Override
    protected void before() {
        super.before();

        try {
            FtpServerFactory serverFactory = new FtpServerFactory();

            ListenerFactory listenerFactory = new ListenerFactory();
            listenerFactory.setPort( port );

            if( tls ) {
                URL keystoreUrl = FtpFixture.class.getResource( "/oap/storage/cloud/ftp-test-keystore.jks" );
                if( keystoreUrl == null ) {
                    throw new IOException( "ftp-test-keystore.jks not found on classpath" );
                }

                SslConfigurationFactory sslConfigurationFactory = new SslConfigurationFactory();
                sslConfigurationFactory.setKeystoreFile( new File( keystoreUrl.toURI() ) );
                sslConfigurationFactory.setKeystorePassword( KEYSTORE_PASSWORD );

                listenerFactory.setSslConfiguration( sslConfigurationFactory.createSslConfiguration() );
                listenerFactory.setImplicitSsl( implicitTls );
            }

            serverFactory.addListener( "default", listenerFactory.createListener() );

            BaseUser user = new BaseUser();
            user.setName( USERNAME );
            user.setPassword( PASSWORD );
            user.setHomeDirectory( testDirectoryFixture.testDirectory().toString() );
            user.setAuthorities( List.of( new WritePermission() ) );

            serverFactory.getUserManager().save( user );

            ftpServer = serverFactory.createServer();
            ftpServer.start();
        } catch( Exception e ) {
            throw new CloudException( e );
        }
    }

    @Override
    protected void after() {
        if( ftpServer != null ) {
            ftpServer.stop();
        }

        super.after();
    }

    public Path homeDirectory() {
        return testDirectoryFixture.testDirectory();
    }

    public FileSystemConfiguration getFileSystemConfiguration( String container ) {
        String scheme = tls ? "ftps" : "ftp";

        return new FileSystemConfiguration( Map.of(
            "fs." + scheme + ".clouds.host", "localhost",
            "fs." + scheme + ".clouds.port", port,
            "fs." + scheme + ".clouds.identity", USERNAME,
            "fs." + scheme + ".clouds.credential", PASSWORD,
            "fs." + scheme + ".clouds.trust-all", true,

            "fs.default.clouds.scheme", scheme,
            "fs.default.clouds.container", container
        ) );
    }

    public Path resolve( String relativePath ) {
        return homeDirectory().resolve( relativePath );
    }

    public void writeFile( String relativePath, String content ) {
        Files.write( resolve( relativePath ), content, ContentWriter.ofString() );
    }

    public void createDirectory( String relativePath ) {
        Files.ensureDirectory( resolve( relativePath ) );
    }

    public String readFile( String relativePath ) {
        return Files.read( resolve( relativePath ), ContentReader.ofString() );
    }
}
