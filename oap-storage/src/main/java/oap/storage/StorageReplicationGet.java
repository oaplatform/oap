/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

import com.fasterxml.jackson.core.type.TypeReference;
import oap.json.Binder;
import oap.replication.ReplicationGet;
import oap.util.Result;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by Igor Petrenko on 06.10.2015.
 */
public class StorageReplicationGet<T> extends ReplicationGet {
    private Storage<T> storage;

    public StorageReplicationGet( String master, String replicationUrl, Storage<T> storage ) {
        super( master, replicationUrl );
        this.storage = storage;
    }

    public final Storage<T> getStorage() {
        return storage;
    }

    @Override
    protected void process( Result<String, String> result ) {
        result.ifSuccess( r -> {
            if( result.isSuccess() ) {
                final List<Metadata<T>> objects = Binder.unmarshal( new TypeReference<List<Metadata<T>>>() {
                }, r );
                if( !objects.isEmpty() )
                    storage.store( objects.stream().map( m -> m.object ).collect( toList() ) );
            }
        } );
    }
}
