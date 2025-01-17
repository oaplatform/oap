package oap.mail;

public interface MailQueuePersistence extends Iterable<Message> {
    void add( Message message );

    int size();
}
