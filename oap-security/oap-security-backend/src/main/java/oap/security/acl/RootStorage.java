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

package oap.security.acl;

import oap.storage.Constraint;
import oap.storage.IdentifierBuilder;
import oap.storage.MemoryStorage;
import oap.storage.Storage;
import oap.util.Pair;
import oap.util.Stream;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static oap.security.acl.AclService.ROOT;
import static oap.storage.Storage.LockStrategy.NoLock;

/**
 * Created by igor.petrenko on 02.01.2018.
 */
class RootStorage implements Storage<RootObject> {
    private final MemoryStorage<RootObject> storage;

    public RootStorage() {
        this.storage = new MemoryStorage<>( IdentifierBuilder.identify( root -> ROOT ).build(), NoLock );

        storage.store( new RootObject(), new AclObject( ROOT, "root", emptyList(), emptyList(), emptyList(), ROOT ) );
    }

    @Override
    public <M> M updateMetadata( String id, Function<M, M> func ) {
        return storage.updateMetadata( id, func );
    }

    @Override
    public <M> M getMetadata( String id ) {
        return storage.getMetadata( id );
    }

    @Override
    public <M> Stream<M> selectMetadata() {
        return storage.selectMetadata();
    }

    @Override
    public <TMetadata> Stream<RootObject> select( Predicate<TMetadata> metadataFilter ) {
        return storage.select( metadataFilter );
    }

    @Override
    public <TMetadata> RootObject store( RootObject object, TMetadata metadata ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <TMetadata> void store( Collection<RootObject> objects, Collection<TMetadata> metadata ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <TMetadata> Optional<RootObject> update( String id, RootObject object, TMetadata metadata ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <TMetadata> Optional<RootObject> update( String id, BiPredicate<RootObject, TMetadata> predicate, BiFunction<RootObject, TMetadata, RootObject> update, Supplier<Pair<RootObject, TMetadata>> init ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update( Collection<String> ids, Predicate<RootObject> predicate, Function<RootObject, RootObject> update, Supplier<RootObject> init ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RootObject> get( String id ) {
        return storage.get( id );
    }

    @Override
    public Optional<RootObject> delete( String id ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long size() {
        return storage.size();
    }

    @Override
    public Storage<RootObject> copyAndClean() {
        return storage.copyAndClean();
    }

    @Override
    public void fsync() {
        storage.fsync();
    }

    @Override
    public Map<String, RootObject> toMap() {
        return storage.toMap();
    }

    @Override
    public void addDataListener( DataListener<RootObject> dataListener ) {
        storage.addDataListener( dataListener );
    }

    @Override
    public void removeDataListener( DataListener<RootObject> dataListener ) {
        storage.removeDataListener( dataListener );
    }

    @Override
    public void addConstraint( Constraint<RootObject, ?> constraint ) {
        storage.addConstraint( constraint );
    }

    @Override
    public void close() {
        storage.close();
    }

    @Override
    public Iterator<RootObject> iterator() {
        return storage.iterator();
    }
}
