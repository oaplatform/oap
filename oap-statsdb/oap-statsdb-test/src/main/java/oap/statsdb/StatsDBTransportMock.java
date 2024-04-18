package oap.statsdb;

import oap.net.Inet;
import oap.statsdb.node.StatsDBTransport;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * Created by igor.petrenko on 2019-12-18.
 */
public class StatsDBTransportMock implements StatsDBTransport {
    public final ArrayList<RemoteStatsDB.Sync> syncs = new ArrayList<>();
    private final StatsDBMaster master;
    private Function<RemoteStatsDB.Sync, RuntimeException> exceptionFunc;

    public StatsDBTransportMock() {
        this( null );
    }

    public StatsDBTransportMock( StatsDBMaster master ) {
        this.master = master;
    }

    @Override
    public void sendAsync( RemoteStatsDB.Sync sync ) {
        if( exceptionFunc != null ) throw exceptionFunc.apply( sync );

        syncs.add( sync );

        if( master != null ) master.update( sync, Inet.HOSTNAME );
    }

    public void syncWithException( Function<RemoteStatsDB.Sync, RuntimeException> exceptionFunc ) {
        this.exceptionFunc = exceptionFunc;
    }

    public void syncWithoutException() {
        this.exceptionFunc = null;
    }

    public void reset() {
        syncs.clear();
        exceptionFunc = null;
    }
}
