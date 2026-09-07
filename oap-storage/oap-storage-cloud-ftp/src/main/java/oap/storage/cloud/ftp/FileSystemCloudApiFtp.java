package oap.storage.cloud.ftp;

import oap.storage.cloud.FileSystemConfiguration;
import org.apache.commons.net.ftp.FTPClient;

public class FileSystemCloudApiFtp extends AbstractFileSystemCloudApiFtp {
    public FileSystemCloudApiFtp( FileSystemConfiguration fileSystemConfiguration, String alias ) {
        super( fileSystemConfiguration, "ftp", alias );
    }

    @Override
    protected FTPClient createClient() {
        return new FTPClient();
    }
}
