package oap.statsdb.node;

import oap.io.content.ContentWriter;
import oap.message.client.MessageSender;

/**
 * Created by igor.petrenko on 2019-12-17.
 */
public class StatsDBTransportMessage implements StatsDBTransport {
    public static final byte MESSAGE_TYPE = 10;

    private final MessageSender sender;

    public StatsDBTransportMessage( MessageSender sender ) {
        this.sender = sender;
    }

    @Override
    public void sendAsync( RemoteStatsDB.Sync sync ) {
        sender.send( MESSAGE_TYPE, sync, ContentWriter.ofJson() );
    }
}
