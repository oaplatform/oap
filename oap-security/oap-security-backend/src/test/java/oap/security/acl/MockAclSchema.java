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

import oap.storage.Storage;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by igor.petrenko on 02.01.2018.
 */
public class MockAclSchema implements AclSchema {
    private final Storage<AclObject> storage;

    public MockAclSchema( Storage<AclObject> storage ) {
        this.storage = storage;
        this.storage.store( new RootObject() );
    }

    @Override
    public void validateNewObject( AclObject parent, String newObjectType ) throws AclSecurityException {

    }

    @Override
    public Optional<? extends AclObject> getObject( String id ) {
        return storage.get( id );
    }

    @Override
    public Stream<AclObject> selectObjects() {
        return storage.select();
    }

    @Override
    public Iterable<AclObject> objects() {
        return () -> storage.select().iterator();
    }

    @Override
    public Optional<? extends AclObject> updateObject( String id, Consumer<AclObject> cons ) {
        return storage.update( id, ( o ) -> {
            cons.accept( o );
            return o;
        } );
    }

    @Override
    public void deleteObject( String id ) {
        storage.delete( id );
    }
}
