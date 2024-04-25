package oap.statsdb.node;

import oap.io.content.ContentWriter;
import oap.message.client.MessageSender;
import oap.statsdb.RemoteStatsDB;

import static oap.statsdb.MessageType.MESSAGE_TYPE;

/**
 * Created by igor.petrenko on 2019-12-17.
 */
public class StatsDBTransportMessage implements StatsDBTransport {
    private final MessageSender sender;

    public StatsDBTransportMessage( MessageSender sender ) {
        this.sender = sender;
    }

    @Override
    public void sendAsync( RemoteStatsDB.Sync sync ) {
        sender.send( MESSAGE_TYPE, sync, ContentWriter.ofJson() );
    }
}
