package oap.logstream.disk;

import oap.logstream.Timestamp;
import oap.util.Pair;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static oap.util.Pair.__;

public class MockFinisher extends AbstractFinisher {
    public final ArrayList<Pair<Path, DateTime>> files = new ArrayList<>();

    protected MockFinisher( Path sourceDirectory, long safeInterval, List<String> mask, Timestamp timestamp ) {
        super( sourceDirectory, safeInterval, mask, timestamp );
    }

    @Override
    protected void cleanup() {
    }

    @Override
    protected void process( Path path, DateTime bucketTime ) {
        files.add( __( path, bucketTime ) );
    }
}
