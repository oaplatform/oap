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

import lombok.val;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.storage.IdentifierBuilder;
import oap.storage.MemoryStorage;
import org.testng.annotations.Test;

import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.storage.Storage.LockStrategy.NoLock;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by igor.petrenko on 29.12.2017.
 */
public class DefaultAclSchemaTest {
    @Test
    public void testValidateNewObject() {
        val storage = new MemoryStorage<AclObject>( IdentifierBuilder.<AclObject>identify( obj -> obj.id ).build(), NoLock );
        val schema = new DefaultAclSchema( storage,
            Binder.hoconWithoutSystemProperties.unmarshal( new TypeRef<Map<String, Object>>() {}, "{root{user{},organization{user{}}}}" ) );


        schema.validateNewObject( null, "root" );

        val root = storage.store( new AclObject( "root", "root", emptyList(), emptyList(), emptyList(), "owner" ) );
        schema.validateNewObject( root, "user" );
        schema.validateNewObject( root, "organization" );
        assertThatThrownBy( () -> schema.validateNewObject( root, "unknown" ) ).hasMessageStartingWith( "unknown is not" );

        val organization = storage.store( new AclObject( "org1", "organization", singletonList( root.id ), singletonList( root.id ), emptyList(), "owner" ) );
        schema.validateNewObject( organization, "user" );
        assertThatThrownBy( () -> schema.validateNewObject( organization, "root" ) ).hasMessageStartingWith( "root is not" );
    }
}