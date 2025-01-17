package oap.mail;

import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.util.Lists;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class MailQueuePersistenceFile extends MailQueuePersistenceMemory {
    private final Path location;

    public MailQueuePersistenceFile( Path location ) {
        this.location = location.resolve( "mail.gz" );
        load();
    }

    private void load() {
        log.debug( "loading queue..." );
        queue.addAll( Binder.json.unmarshal( new TypeRef<List<Message>>() {}, location ).orElse( Lists.empty() ) );
        log.debug( "{} messages loaded", size() );
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
