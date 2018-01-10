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
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static oap.application.ApplicationUtils.service;
import static oap.security.acl.AclService.ROOT;
import static oap.storage.Storage.LockStrategy.Lock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by igor.petrenko on 21.12.2017.
 */
public class DefaultAclServiceTest {
    private Storage<SecurityContainer<TestAclObject>> objectStorage;
    private Storage<AclRole> roleStorage;
    private DefaultAclService aclService;
    private String rootId;
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
        objectStorage = service( new MemoryStorage<SecurityContainer<TestAclObject>>( IdentifierBuilder.annotationBuild(), Lock ) );
        roleStorage = service( new MemoryStorage<>( IdentifierBuilder.annotationBuild(), Lock ) );

        val gaRole = roleStorage.store( new AclRole( AclService.GLOBAL_ADMIN_ROLE, "ga", singletonList( "*" ) ) );

        val aclSchema = new MockAclSchema( objectStorage );
        aclService = service( new DefaultAclService( roleStorage, aclSchema ) );

        rootId = register( ROOT, "testObject1", ROOT );
        childId = register( rootId, "child", ROOT );
        childId2 = register( childId, "child", ROOT );
        subjectId = register( rootId, "subject", ROOT );
        subjectGroupId = register( rootId, "subject", ROOT );
        subjectId2 = register( subjectGroupId, "subject", ROOT );
        subjectId23 = register( subjectId2, "subject", ROOT );

        ga = register( ROOT, "user", ROOT );

        roleUknown = roleStorage.store( new AclRole( "roleIdUknown", "testRole1", singletonList( "testObjectUnknown.read" ) ) );
        role1 = roleStorage.store( new AclRole( "roleId1", "testRole1", singletonList( "testObject1.read" ) ) );
        role2 = roleStorage.store( new AclRole( "roleId2", "testRole2", singletonList( "testObject2.read" ) ) );

        aclService.add( ROOT, ga, gaRole.id, true );
    }

    private String register( String parent, String type, String owner ) {
        return objectStorage.store( aclService.addChild( parent, new TestAclObject(), type, owner ).get() ).id;
    }

    @Test
    public void testGlobalAdmin() {
        assertThat( aclService.checkOne( rootId, ga, "any permission" ) ).isTrue();
    }

    @Test
    public void testAddAcl() {
        assertThat( aclService.check( rootId, subjectId, "testObject1.read" ) ).containsExactly( false );

        aclService.add( rootId, subjectId, roleUknown.id, false );
        assertThat( aclService.check( rootId, subjectId, "testObject1.read" ) ).containsExactly( false );

        aclService.add( rootId, subjectId, role1.id, false );
        assertThat( aclService.check( rootId, subjectId, "testObject1.read" ) ).containsExactly( true );
        assertThat( aclService.check( rootId, subjectId2, "testObject1.read" ) ).containsExactly( false );

        assertThat( aclService.list( rootId, subjectId ) ).containsExactlyInAnyOrder( roleUknown, role1 );
    }

    @Test( dependsOnMethods = { "testAddAcl" } )
    public void testRemoveAcl() {
        testAddAcl();

        aclService.remove( rootId, subjectId, role1.id );
        assertThat( aclService.check( rootId, subjectId, "testObject1.read" ) ).containsExactly( false );

        assertThat( aclService.list( rootId, subjectId ) ).containsExactly( roleUknown );
    }

    @Test
    public void testInheritance() {
        aclService.add( rootId, subjectId, role1.id, true );
        assertThat( aclService.check( childId, subjectId, "testObject1.read" ) ).containsExactly( true );
        assertThat( aclService.check( childId2, subjectId, "testObject1.read" ) ).containsExactly( true );

        assertThat( aclService.list( childId, subjectId ) ).containsExactly( role1 );
        assertThat( aclService.list( childId2, subjectId ) ).containsExactly( role1 );

        aclService.remove( childId, subjectId, role1.id );
        assertThat( aclService.check( childId, subjectId, "testObject1.read" ) ).containsExactly( true );
        assertThat( aclService.check( childId2, subjectId, "testObject1.read" ) ).containsExactly( true );

        aclService.remove( rootId, subjectId, role1.id );
        assertThat( aclService.check( childId, subjectId, "testObject1.read" ) ).containsExactly( false );
        assertThat( aclService.check( childId2, subjectId, "testObject1.read" ) ).containsExactly( false );
    }

    @Test
    public void testSubjectGroup() {
        aclService.add( rootId, subjectGroupId, role1.id, true );
        assertThat( aclService.check( rootId, subjectId2, "testObject1.read" ) ).containsExactly( true );
        assertThat( aclService.check( childId, subjectId2, "testObject1.read" ) ).containsExactly( true );
    }

    @Test
    public void testGetChildren() {
        assertThat( aclService.getChildren( rootId, "test", true ) ).isEmpty();
        assertThat( aclService.getChildren( rootId, "child", false ) )
            .containsExactlyInAnyOrder( childId );
        assertThat( aclService.getChildren( rootId, "child", true ) )
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

        aclService.add( rootId, childId2, role1.id, true );

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

        assertThat( objectStorage
            .select()
            .filter( aclService.getAclFilter( subjectGroupId, subjectId, "testObject1.read" ) ) ).isEmpty();

        assertThat( objectStorage
            .select()
            .filter( aclService.getAclFilter( subjectGroupId, childId, "testObject1.read" ) )
            .map( obj -> obj.id )
        ).containsExactlyInAnyOrder( subjectId2, subjectId23 );

        assertThat( objectStorage
            .select()
            .filter( aclService.getAclFilter( subjectGroupId, childId, "unknown" ) ) ).isEmpty();
    }

    @Test
    public void testGetSubjectRoles() {
        aclService.add( childId, subjectId, role1.id, false );
        aclService.add( childId, subjectId, role2.id, false );
        aclService.add( rootId, subjectId, role2.id, true );
        aclService.add( childId, subjectId2, role2.id, false );

        assertThat( aclService.getSubjectRoles( childId, false ) ).containsExactlyInAnyOrder(
            new AclService.SubjectRole( subjectId, asList( role2, role1 ) ),
            new AclService.SubjectRole( subjectId2, singletonList( role2 ) ) );

        assertThat( aclService.getSubjectRoles( childId, true ) ).hasSize( 3 );
    }

    @Test
    public void testGetRoles() {
        aclService.add( rootId, subjectId, role1.id, false );
        aclService.add( rootId, subjectId, role2.id, true );
        aclService.add( childId, subjectId, role2.id, false );

        final List<AclService.ObjectRole> roles = aclService.getRoles( subjectId, false );
        assertThat( roles ).containsExactlyInAnyOrder(
            new AclService.ObjectRole( rootId, asList( role2, role1 ) ),
            new AclService.ObjectRole( childId, singletonList( role2 ) ) );

        assertThat( aclService.getRoles( subjectId, true ) ).hasSize( 7 );
    }

    @Test
    public void testAddChild() {
        assertThat(aclService.getChildren( rootId, "child", false )).hasSize( 1 );
        assertThat(aclService.getChildren( childId, "child", false )).hasSize( 1 );

        aclService.addChild( rootId, childId2 );

        assertThat(aclService.getChildren( rootId, "child", false )).hasSize( 2 );
        assertThat(aclService.getChildren( childId, "child", false )).hasSize( 1 );
    }

    @AfterMethod
    public void afterMethod() throws IOException {
        objectStorage.close();
        roleStorage.close();
    }
}