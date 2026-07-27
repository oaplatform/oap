package oap.storage.cloud.ftp;

import oap.storage.cloud.FileSystemConfiguration;
import org.apache.commons.net.ftp.FTPClient;

public class FileSystemCloudApiFtp extends AbstractFileSystemCloudApiFtp {
    public FileSystemCloudApiFtp( FileSystemConfiguration fileSystemConfiguration, String container ) {
        super( fileSystemConfiguration, "ftp", container );
    }

    @Override
    protected FTPClient createClient() {
        return new FTPClient();
    }
}
