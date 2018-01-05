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
import oap.storage.IdentifierBuilder;
import oap.storage.MemoryStorage;
import oap.storage.Storage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.application.ApplicationUtils.service;
import static oap.security.acl.AclService.ROOT;
import static oap.storage.Storage.LockStrategy.Lock;
import static oap.util.Strings.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by igor.petrenko on 21.12.2017.
 */
public class DefaultAclServiceTest {
    private Storage<TestAclObject> objectStorage;
    private Storage<AclRole> roleStorage;
    private DefaultAclService aclService;
    private String objectId;
    private String childId;
    private String childId2;
    private String subjectId;
    private AclRole roleUknown;
    private AclRole role1;
    private AclRole role2;
    private String subjectGroupId;
    private String subjectId2;
    private String subjectId23;
    private String ga;

    @BeforeMethod
    public void beforeMethod() {
        objectStorage = service( new MemoryStorage<TestAclObject>( IdentifierBuilder.annotationBuild(), Lock ) {
            @Override
            public Object getDefaultMetadata( TestAclObject object ) {
                return new AclObject( object.id, object.type, emptyList(), emptyList(), emptyList(), UNKNOWN );
            }
        } );
        roleStorage = service( new MemoryStorage<>( IdentifierBuilder.annotationBuild(), Lock ) );

        val gaRole = roleStorage.store( new AclRole( AclService.GLOBAL_ADMIN_ROLE, "ga", singletonList( "*" ) ) );

        val aclSchema = new MockAclSchema( objectStorage );
        aclService = service( new DefaultAclService( roleStorage, aclSchema ) );

        objectId = aclService.addChild( ROOT, objectStorage.store( new TestAclObject( "testObject1" ) ).id ).get().id;
        childId = aclService.addChild( objectId, objectStorage.store( new TestAclObject( "child" ) ).id ).get().id;
        childId2 = aclService.addChild( childId, objectStorage.store( new TestAclObject( "child" ) ).id ).get().id;
        subjectId = aclService.addChild( objectId, objectStorage.store( new TestAclObject( "subject" ) ).id ).get().id;
        subjectGroupId = aclService.addChild( objectId, objectStorage.store( new TestAclObject( "subject" ) ).id ).get().id;
        subjectId2 = aclService.addChild( subjectGroupId, objectStorage.store( new TestAclObject( "subject" ) ).id ).get().id;
        subjectId23 = aclService.addChild( subjectId2, objectStorage.store( new TestAclObject( "subject" ) ).id ).get().id;

        ga = aclService.addChild( ROOT, objectStorage.store( new TestAclObject( "user" ) ).id ).get().id;

        roleUknown = roleStorage.store( new AclRole( "roleIdUknown", "testRole1", singletonList( "testObjectUnknown.read" ) ) );
        role1 = roleStorage.store( new AclRole( "roleId1", "testRole1", singletonList( "testObject1.read" ) ) );
        role2 = roleStorage.store( new AclRole( "roleId2", "testRole2", singletonList( "testObject2.read" ) ) );

        aclService.add( ROOT, ga, gaRole.id, true );
    }

    @Test
    public void testGlobalAdmin() {
        assertThat( aclService.checkOne( objectId, ga, "any permission" ) ).isTrue();
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
        assertThat( aclService.findChildren( subjectGroupId, childId, "subject", "unknown" ) ).isEmpty();
    }

    @Test
    public void testGetAclFilter() {
        aclService.add( subjectGroupId, childId, role1.id, true );

        assertThat(objectStorage
            .select(aclService.getAclFilter( subjectGroupId, subjectId, "testObject1.read" ))).isEmpty();

        assertThat(objectStorage
            .select(aclService.getAclFilter( subjectGroupId, childId, "testObject1.read" ))
            .map(obj -> obj.id)
        ).containsExactlyInAnyOrder( subjectId2, subjectId23 );

        assertThat(objectStorage
            .select(aclService.getAclFilter( subjectGroupId, childId, "unknown" ))).isEmpty();
    }

    @Test
    public void testGetSubjectRoles() {
        aclService.add( childId, subjectId, role1.id, false );
        aclService.add( childId, subjectId, role2.id, false );
        aclService.add( objectId, subjectId, role2.id, true );
        aclService.add( childId, subjectId2, role2.id, false );

        assertThat( aclService.getSubjectRoles( childId, false ) ).containsExactlyInAnyOrder(
            new AclService.SubjectRole( subjectId, asList( role1, role2 ) ),
            new AclService.SubjectRole( subjectId2, singletonList( role2 ) ) );

        assertThat( aclService.getSubjectRoles( childId, true ) ).hasSize( 3 );
    }

    @Test
    public void testGetRoles() {
        aclService.add( objectId, subjectId, role1.id, false );
        aclService.add( objectId, subjectId, role2.id, true );
        aclService.add( childId, subjectId, role2.id, false );

        assertThat( aclService.getRoles( subjectId, false ) ).containsExactlyInAnyOrder(
            new AclService.ObjectRole( objectId, asList( role1, role2 ) ),
            new AclService.ObjectRole( childId, singletonList( role2 ) ) );

        assertThat( aclService.getRoles( subjectId, true ) ).hasSize( 7 );
    }

    @AfterMethod
    public void afterMethod() throws IOException {
        objectStorage.close();
        roleStorage.close();
    }
}