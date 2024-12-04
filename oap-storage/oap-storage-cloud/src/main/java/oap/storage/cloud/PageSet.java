package oap.storage.cloud;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class PageSet<T> implements Iterable<T> {
    public final String nextContinuationToken;
    private final List<T> items;

    public PageSet( String nextContinuationToken, List<T> items ) {
        this.nextContinuationToken = nextContinuationToken;
        this.items = items;
    }

    public int size() {
        return items.size();
    }

    public T get( int index ) {
        return items.get( index );
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    @Override
    public void forEach( Consumer<? super T> action ) {
        items.forEach( action );
    }

    @Nonnull
    @Override
    public Spliterator<T> spliterator() {
        return items.spliterator();
    }
}
