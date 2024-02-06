/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.storage;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.ToString;
import org.openjdk.jol.info.GraphLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@ToString
public class StorageMetrics<I, T> implements Storage.DataListener<I, T> {
    private final String storageName;
    private final Storage<I, T> storage;
    private final Map<String, Meter<I, T>> metrics = new HashMap<>();

    public StorageMetrics( Storage<I, T> storage, String name ) {
        this.storage = storage;
        this.storageName = name;
        this.metrics.put( "oap_storage_total", new Count<>() );
        this.metrics.put( "oap_storage_memory", new Memory<>() );
    }

    public void start() {
        refresh();

        storage.addDataListener( this );

        this.metrics.forEach( ( mname, metric ) ->
            Metrics.gauge( mname, Tags.of( "storage", storageName ), metric, m -> ( double ) m.value() ) );
    }

    @Override
    public void added( List<IdObject<I, T>> idObjects ) {
        refresh();
    }

    private void refresh() {
        metrics.forEach( ( name, metric ) -> metric.accept( storage ) );
    }

    @Override
    public void updated( List<IdObject<I, T>> idObjects ) {
        refresh();
    }

    @Override
    public void deleted( List<IdObject<I, T>> idObjects ) {
        refresh();
    }

    public interface Meter<I, T> extends Consumer<Storage<I, T>> {
        long value();
    }

    public static class Count<I, T> implements Meter<I, T> {
        private final AtomicLong count = new AtomicLong();

        @Override
        public void accept( Storage<I, T> storage ) {
            count.set( storage.size() );
        }

        @Override
        public long value() {
            return count.get();
        }
    }

    public static class Memory<I, T> implements Meter<I, T> {
        private final AtomicLong size = new AtomicLong();

        @Override
        public void accept( Storage<I, T> storage ) {
            if( storage instanceof MemoryStorage<?, ?> ) {
                size.set( GraphLayout.parseInstance( ( ( MemoryStorage<I, T> ) storage ).memory.data ).totalSize() );
            } else size.set( 0 );
        }

        @Override
        public long value() {
            return size.get();
        }
    }
}
