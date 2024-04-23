package oap.statsdb.node;

import oap.statsdb.RemoteStatsDB;

/**
 * Created by igor.petrenko on 2019-12-17.
 */
public interface StatsDBTransport {
    void sendAsync( RemoteStatsDB.Sync sync );
}
