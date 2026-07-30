package oap.storage.cloud.ftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;

@Slf4j
class FtpClientPooledObjectFactory extends BasePooledObjectFactory<FTPClient> {
    private final AbstractFileSystemCloudApiFtp owner;

    FtpClientPooledObjectFactory( AbstractFileSystemCloudApiFtp owner ) {
        this.owner = owner;
    }

    @Override
    public FTPClient create() {
        return owner.createAndLoginClient();
    }

    @Override
    public PooledObject<FTPClient> wrap( FTPClient client ) {
        return new DefaultPooledObject<>( client );
    }

    @Override
    public void destroyObject( PooledObject<FTPClient> pooledObject ) {
        AbstractFileSystemCloudApiFtp.disconnect( pooledObject.getObject() );
    }

    @Override
    public boolean validateObject( PooledObject<FTPClient> pooledObject ) {
        FTPClient client = pooledObject.getObject();
        try {
            return client.isConnected() && client.sendNoOp();
        } catch( IOException e ) {
            return false;
        }
    }
}
