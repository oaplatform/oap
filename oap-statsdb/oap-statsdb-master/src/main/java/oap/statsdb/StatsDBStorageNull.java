package oap.statsdb;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Created by igor.petrenko on 26.03.2019.
 */
public class StatsDBStorageNull implements StatsDBStorage {
    @Override
    public Map<String, Node> load( NodeSchema schema ) {
        return emptyMap();
    }

    @Override
    public void store( NodeSchema schema, Map<String, Node> db ) {

    }

    @Override
    public void removeAll() {

    }
}
