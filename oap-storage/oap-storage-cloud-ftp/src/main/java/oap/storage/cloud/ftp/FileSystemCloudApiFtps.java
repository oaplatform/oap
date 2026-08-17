package oap.storage.cloud.ftp;

import oap.storage.cloud.FileSystemConfiguration;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;

import java.io.IOException;

public class FileSystemCloudApiFtps extends AbstractFileSystemCloudApiFtp {
    private final boolean implicitTls;
    private final boolean trustAll;

    public FileSystemCloudApiFtps( FileSystemConfiguration fileSystemConfiguration, String container ) {
        super( fileSystemConfiguration, "ftps", container );

        Object tlsMode = fileSystemConfiguration.get( "ftps", container, "clouds.tls-mode" );
        implicitTls = tlsMode != null && "implicit".equalsIgnoreCase( tlsMode.toString() );

        Object trustAllObj = fileSystemConfiguration.get( "ftps", container, "clouds.trust-all" );
        trustAll = trustAllObj != null && Boolean.parseBoolean( trustAllObj.toString() );
    }

    @Override
    protected FTPClient createClient() {
        FTPSClient client = new FTPSClient( implicitTls );
        if( trustAll ) {
            client.setTrustManager( TrustManagerUtils.getAcceptAllTrustManager() );
        }
        return client;
    }

    @Override
    protected void afterLogin( FTPClient client ) throws IOException {
        FTPSClient ftpsClient = ( FTPSClient ) client;
        ftpsClient.execPBSZ( 0 );
        ftpsClient.execPROT( "P" );
    }
}
