package oap.storage.cloud.ftp;

import oap.storage.cloud.FileSystemConfiguration;
import org.apache.commons.net.ftp.FTPClient;

public class FileSystemCloudApiFtp extends AbstractFileSystemCloudApiFtp {
    public FileSystemCloudApiFtp( FileSystemConfiguration fileSystemConfiguration, String configurationId ) {
        super( fileSystemConfiguration, "ftp", configurationId );
    }

    @Override
    protected FTPClient createClient() {
        return new FTPClient();
    }
}
