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

import oap.storage.IdentifierBuilder;
import oap.storage.MemoryStorage;
import oap.storage.Storage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static oap.application.ApplicationUtils.service;
import static oap.security.acl.AclService.GLOBAL_ADMIN;
import static oap.security.acl.AclService.ROOT;
import static oap.storage.Storage.LockStrategy.Lock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by igor.petrenko on 21.12.2017.
 */
public class AclServiceTest {
    private Storage<AclObject> objectStorage;
    private Storage<AclRole> roleStorage;
    private DefaultAclService aclService;
    private String objectId;
    private String childId;
    private String childId2;
    private String subjectId;
    private AclRole roleUknown;
    private AclRole role1;
    private String subjectGroupId;
    private String subjectId2;
    private String subjectId23;

    @BeforeMethod
    public void beforeMethod() {
        objectStorage = service( new MemoryStorage<>( IdentifierBuilder.<AclObject>identify( ao -> ao.id, ( ao, id ) -> ao.id = id ).build(), Lock ) );
        roleStorage = service( new MemoryStorage<>( IdentifierBuilder.<AclRole>identify( ar -> ar.id, ( ar, id ) -> ar.id = id ).build(), Lock ) );
        aclService = service( new DefaultAclService( objectStorage, roleStorage ) );

        objectId = aclService.registerObject( ROOT, "testObject1", "own" ).get();
        childId = aclService.registerObject( objectId, "child", "own" ).get();
        childId2 = aclService.registerObject( childId, "child", "own" ).get();
        subjectId = aclService.registerObject( objectId, "subject", "own" ).get();
        subjectGroupId = aclService.registerObject( objectId, "subject", "own" ).get();
        subjectId2 = aclService.registerObject( subjectGroupId, "subject", "own" ).get();
        subjectId23 = aclService.registerObject( subjectId2, "subject", "own" ).get();

        roleUknown = roleStorage.store( new AclRole( "roleIdUknown", "testRole1", singletonList( "testObjectUnknown.read" ) ) );
        role1 = roleStorage.store( new AclRole( "roleId1", "testRole1", singletonList( "testObject1.read" ) ) );
    }

    @Test
    public void testGlobalAdmin() {
        assertThat( aclService.checkOne( objectId, GLOBAL_ADMIN, "any permission" ) ).isTrue();
    }

    @Test
    public void testAddAcl() {
        assertThat( aclService.check( objectId, subjectId, "testObject1.read" ) ).containsExactly( false );

        aclService.add( objectId, subjectId, roleUknown.id, false );
        assertThat( aclService.check( objectId, subjectId, "testObject1.read" ) ).containsExactly( false );

        aclService.add( objectId, subjectId, role1.id, false );
        assertThat( aclService.check( objectId, subjectId, "testObject1.read" ) ).containsExactly( true );
        assertThat( aclService.check( objectId, subjectId2, "testObject1.read" ) ).containsExactly( false );

        assertThat( aclService.list( objectId, subjectId ) ).containsExactlyInAnyOrder( roleUknown, role1 );
    }

    @Test( dependsOnMethods = { "testAddAcl" } )
    public void testRemoveAcl() {
        testAddAcl();

        aclService.remove( objectId, subjectId, role1.id );
        assertThat( aclService.check( objectId, subjectId, "testObject1.read" ) ).containsExactly( false );

        assertThat( aclService.list( objectId, subjectId ) ).containsExactly( roleUknown );
    }

    @Test
    public void testInheritance() {
        aclService.add( objectId, subjectId, role1.id, true );
        assertThat( aclService.check( childId, subjectId, "testObject1.read" ) ).containsExactly( true );
        assertThat( aclService.check( childId2, subjectId, "testObject1.read" ) ).containsExactly( true );

        assertThat( aclService.list( childId, subjectId ) ).containsExactly( role1 );
        assertThat( aclService.list( childId2, subjectId ) ).containsExactly( role1 );

        aclService.remove( childId, subjectId, role1.id );
        assertThat( aclService.check( childId, subjectId, "testObject1.read" ) ).containsExactly( true );
        assertThat( aclService.check( childId2, subjectId, "testObject1.read" ) ).containsExactly( true );

        aclService.remove( objectId, subjectId, role1.id );
        assertThat( aclService.check( childId, subjectId, "testObject1.read" ) ).containsExactly( false );
        assertThat( aclService.check( childId2, subjectId, "testObject1.read" ) ).containsExactly( false );
    }

    @Test
    public void testSubjectGroup() {
        aclService.add( objectId, subjectGroupId, role1.id, true );
        assertThat( aclService.check( objectId, subjectId2, "testObject1.read" ) ).containsExactly( true );
        assertThat( aclService.check( childId, subjectId2, "testObject1.read" ) ).containsExactly( true );
    }

    @Test
    public void testGetChildren() {
        assertThat( aclService.getChildren( objectId, "test", true ) ).isEmpty();
        assertThat( aclService.getChildren( objectId, "child", false ) )
            .containsExactlyInAnyOrder( childId );
        assertThat( aclService.getChildren( objectId, "child", true ) )
            .containsExactlyInAnyOrder( childId, childId2 );
    }

    @Test
    public void testSelfPermission() {
        assertThat( aclService.checkOne( subjectId, subjectId, "any permissions" ) ).isTrue();
    }

    @Test
    public void testUnregisterObject() {
        assertThatThrownBy( () -> aclService.unregisterObject( childId ) )
            .hasMessage( "Group '" + childId + "' not empty" )
            .isInstanceOf( AclSecurityException.class );
        assertThatThrownBy( () -> aclService.unregisterObject( "unknown" ) )
            .hasMessage( "Object 'unknown' not found" )
            .isInstanceOf( AclSecurityException.class );

        aclService.add( objectId, childId2, role1.id, true );

        assertThat( objectStorage.get( childId2 ) ).isPresent();
        assertThat( aclService.list( subjectId2, childId2 ) ).isNotEmpty();
        aclService.unregisterObject( childId2 );
        assertThat( objectStorage.get( childId2 ) ).isEmpty();
        assertThat( aclService.list( subjectId2, childId2 ) ).isEmpty();
    }

    @Test
    public void testFindChildren() {
        aclService.add( subjectGroupId, childId, role1.id, true );

        assertThat( aclService.findChildren( subjectGroupId, subjectId, "subject", "testObject1.read" ) ).isEmpty();
        assertThat( aclService.findChildren( subjectGroupId, childId, "subject", "testObject1.read" ) )
            .containsExactlyInAnyOrder( subjectId2, subjectId23 );
    }

    @AfterMethod
    public void afterMethod() throws IOException {
        objectStorage.close();
        roleStorage.close();
    }
}