package oap.statsdb;

import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.message.MessageListener;
import oap.message.MessageProtocol;

import java.io.ByteArrayInputStream;

import static oap.statsdb.StatsDBTransportMessage.MESSAGE_TYPE;

/**
 * Created by igor.petrenko on 2019-12-17.
 */
@Slf4j
public class StatsDBMessageListener implements MessageListener {
    private final StatsDBMaster master;

    public StatsDBMessageListener( StatsDBMaster master ) {
        this.master = master;
    }

    @Override
    public byte getId() {
        return MESSAGE_TYPE;
    }

    @Override
    public String getInfo() {
        return "stats-db";
    }

    @Override
    public short run( int version, String hostName, int size, byte[] data, String md5 ) {
        log.trace( "new stats version {} hostName {} size {} md5 {} data '{}'",
            version, hostName, size, md5, new String( data ) );

        var sync = Binder.json.unmarshal( RemoteStatsDB.Sync.class, new ByteArrayInputStream( data ) );
        master.update( sync, hostName );

        return MessageProtocol.STATUS_OK;
    }
}
