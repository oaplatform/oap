package oap.mail;

import java.util.ArrayList;

/**
 * Created by igor.petrenko on 2019-12-09.
 */
public class TransportMock implements Transport {
    public final ArrayList<Message> messages = new ArrayList<>();

    @Override
    public void send( Message message ) {
        messages.add( message );
    }
}
