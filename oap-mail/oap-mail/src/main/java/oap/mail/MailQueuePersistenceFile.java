package oap.mail;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.util.Lists;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class MailQueuePersistenceFile implements MailQueuePersistence {
    private final ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>();
    private final Path location;

    public MailQueuePersistenceFile( Path location ) {
        this.location = location.resolve( "mail.gz" );
        load();

        Metrics.gaugeCollectionSize( "mail_queue", Tags.empty(), queue );
    }

    private void load() {
        log.debug( "loading queue..." );
        queue.addAll( Binder.json.unmarshal( new TypeRef<List<Message>>() {}, location ).orElse( Lists.empty() ) );
        log.debug( "{} messages loaded", size() );
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public void add( Message message ) {
        queue.add( message );
    }

    private synchronized void persist() {
        Binder.json.marshal( location, this.queue );
    }

    @Override
    public Iterator<Message> iterator() {
        Iterator<Message> iterator = queue.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Message next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();

                persist();
            }
        };
    }
}
