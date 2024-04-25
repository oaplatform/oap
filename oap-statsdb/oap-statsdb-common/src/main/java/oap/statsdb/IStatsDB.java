package oap.statsdb;

import java.util.function.Consumer;

/**
 * Created by igor.petrenko on 2021-02-22.
 */
@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class IStatsDB {
    public abstract void removeAll();

    protected <V extends Node.Value<V>> void update( String key1, Consumer<V> update ) {
        update( new String[] { key1 }, update );
    }

    protected <V extends Node.Value<V>> void update( String key1, String key2, Consumer<V> update ) {
        update( new String[] { key1, key2 }, update );
    }

    protected <V extends Node.Value<V>> void update( String key1, String key2, String key3, Consumer<V> update ) {
        update( new String[] { key1, key2, key3 }, update );
    }

    protected <V extends Node.Value<V>> void update( String key1, String key2, String key3, String key4, Consumer<V> update ) {
        update( new String[] { key1, key2, key3, key4 }, update );
    }

    protected <V extends Node.Value<V>> void update( String key1, String key2, String key3, String key4, String key5, Consumer<V> update ) {
        update( new String[] { key1, key2, key3, key4, key5 }, update );
    }

    protected <V extends Node.Value<V>> void update( String key1, String key2, String key3, String key4, String key5, String key6, Consumer<V> update ) {
        update( new String[] { key1, key2, key3, key4, key5, key6 }, update );
    }

    protected abstract <V extends Node.Value<V>> void update( String[] keys, Consumer<V> update );

    public abstract <V extends Node.Value<V>> V get( String... key );
}
