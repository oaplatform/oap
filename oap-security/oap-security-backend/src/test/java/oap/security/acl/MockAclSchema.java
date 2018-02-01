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
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static oap.security.acl.AclService.ROOT;

/**
 * Created by igor.petrenko on 02.01.2018.
 */
public class MockAclSchema implements AclSchema {
    private final AclObject ROOT_ACL_OBJECT;
    private final Storage<? extends SecurityContainer<?>> storage;

    public MockAclSchema( Storage<? extends SecurityContainer<?>> storage ) {
        this.storage = storage;

        ROOT_ACL_OBJECT = new AclObject( ROOT, "root", emptyList(), emptyList(), emptyList(), ROOT );
    }

    @Override
    public void validateNewObject( AclObject parent, String newObjectType ) throws AclSecurityException {

    }

    @Override
    public Optional<AclObject> getObject( String id ) {
        if( ROOT.equals( id ) ) return Optional.of( ROOT_ACL_OBJECT );

        return storage.get( id ).map( cs -> cs.acl );
    }

    @Override
    public Stream<AclObject> selectObjects() {
        return storage.select().map( cs -> cs.acl ).concat( ROOT_ACL_OBJECT );
    }

    @Override
    public Stream<AclObject> selectLocalObjects() {
        return selectObjects();
    }

    @Override
    public Iterable<AclObject> objects() {
        return () -> selectObjects().iterator();
    }

    @Override
    public Iterable<AclObject> localObjects() {
        return objects();
    }

    @Override
    public Optional<AclObject> updateLocalObject( String id, Consumer<AclObject> cons ) {
        if( AclService.ROOT.equals( id ) ) {
            cons.accept( ROOT_ACL_OBJECT );

            return Optional.of( ROOT_ACL_OBJECT );
        }
        return storage.update( id, cs -> {
            cons.accept( cs.acl );
            return cs;
        } ).map( cs -> cs.acl );
    }

    @Override
    public void deleteObject( String id ) {
        storage.delete( id );
    }

    @Override
    public List<String> getPermissions( String objectId ) {
        return emptyList();
    }

    @Override
    public AclSchemaBean getSchema() {
        throw new NotImplementedException( "" );
    }
}
