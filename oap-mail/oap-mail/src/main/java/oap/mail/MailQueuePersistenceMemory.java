package oap.mail;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class MailQueuePersistenceMemory implements MailQueuePersistence {
    protected final ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>();

    public MailQueuePersistenceMemory() {
        Metrics.gaugeCollectionSize( "mail_queue", Tags.empty(), queue );
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public void add( Message message ) {
        queue.add( message );
    }

    @Override
    public Iterator<Message> iterator() {
        return queue.iterator();
    }
}
