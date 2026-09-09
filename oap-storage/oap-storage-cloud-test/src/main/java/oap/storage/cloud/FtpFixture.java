package oap.storage.cloud;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.IoStreams;
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

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;

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

    public FileSystemConfiguration getFileSystemConfiguration( String configurationId ) {
        return getFileSystemConfiguration( configurationId, false );
    }

    public FileSystemConfiguration getFileSystemConfiguration( String configurationId, boolean removeEmptyFolders ) {
        return getFileSystemConfiguration( configurationId, removeEmptyFolders, null );
    }

    public FileSystemConfiguration getFileSystemConfiguration( String configurationId, boolean removeEmptyFolders, @Nullable Integer poolMaxSize ) {
        return new FileSystemConfiguration( getFileSystemConfigurationMap( configurationId, removeEmptyFolders, poolMaxSize ) );
    }

    /**
     * Builds config for this server under `configurationId` — always registers `configurationId` via `container.<configurationId>`
     * (and identity/credential/etc alongside it), whether it's the default target or a secondary one
     * layered onto an existing {@link FileSystemConfiguration} via {@link #updateWithFtp}.
     */
    public Map<String, Object> getFileSystemConfigurationMap( String configurationId, boolean removeEmptyFolders, @Nullable Integer poolMaxSize ) {
        String scheme = tls ? "ftps" : "ftp";

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put( s( "fs.${scheme}.identity.${configurationId}" ), USERNAME );
        map.put( s( "fs.${scheme}.credential.${configurationId}" ), PASSWORD );
        map.put( s( "fs.${scheme}.trust_all.${configurationId}" ), true );
        map.put( s( "fs.${scheme}.container.${configurationId}" ), hostPort() );
        map.put( s( "fs.${scheme}.filesystem.basedir.${configurationId}" ), testDirectoryFixture.testPath( configurationId ) );

        if( removeEmptyFolders ) {
            map.put( s( "fs.${scheme}.remove_empty_folders.${configurationId}" ), true );
        }

        if( poolMaxSize != null ) {
            map.put( s( "fs.${scheme}.pool_max_size.${configurationId}" ), poolMaxSize );
        }

        return map;
    }

    public FileSystemConfiguration updateWithFtp( FileSystemConfiguration fileSystemConfiguration, String configurationId, boolean removeEmptyFolders, @Nullable Integer poolMaxSize ) {
        return fileSystemConfiguration.copyWith( getFileSystemConfigurationMap( configurationId, removeEmptyFolders, poolMaxSize ) );
    }

    public String hostPort() {
        return s( "localhost:${port}" );
    }

    public Path resolve( String configurationId, String relativePath ) {
        return homeDirectory().resolve( configurationId ).resolve( relativePath );
    }

    public <T> void writeFile( String configurationId, String relativePath, T content, ContentWriter<T> contentWriter ) {
        Files.write( resolve( configurationId, relativePath ), content, contentWriter );
    }

    public <T> void copyFileTo( String configurationId, Path file, String relativePath ) {
        Files.copy( file, IoStreams.Encoding.PLAIN, resolve( configurationId, relativePath ), IoStreams.Encoding.PLAIN );
    }

    public void createDirectory( String configurationId, String relativePath ) {
        Files.ensureDirectory( resolve( configurationId, relativePath ) );
    }

    public <T> T readFile( String configurationId, String relativePath, ContentReader<T> contentReader ) {
        return Files.read( resolve( configurationId, relativePath ), contentReader );
    }

    public <T> T readFile( String configurationId, String relativePath, IoStreams.Encoding encoding, ContentReader<T> contentReader ) {
        return Files.read( resolve( configurationId, relativePath ), encoding, contentReader );
    }
}
