package oap.mail.mongo;

import oap.mail.Message;

public class MessageData {
    public final Message message;
    public String id;

    public MessageData( String id, Message message ) {
        this.id = id;
        this.message = message;
    }
}
