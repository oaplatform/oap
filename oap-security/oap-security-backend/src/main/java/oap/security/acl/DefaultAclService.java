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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.storage.Storage;
import oap.util.Lists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Created by igor.petrenko on 21.12.2017.
 */
@Slf4j
public class DefaultAclService implements AclService {
    private final Storage<AclObject> objectStorage;
    private final Storage<AclRole> roleStorage;

    public DefaultAclService( Storage<AclObject> objectStorage, Storage<AclRole> roleStorage ) {
        this.objectStorage = objectStorage;
        this.roleStorage = roleStorage;
    }

    @Override
    public void validate( String objectId, String subjectId, String... permissions ) throws AclSecurityException {
        if( check( objectId, subjectId, asList( permissions ) ).indexOf( false ) >= 0 )
            throw new AclSecurityException();
    }

    @Override
    public List<Boolean> check( String objectId, String subjectId, List<String> permissions ) {
        log.debug( "check object = {}, subject = {}, permissions = {}", objectId, subjectId, permissions );

        val aclObject = objectStorage.get( objectId ).orElse( null );
        val aclSubject = objectStorage.get( subjectId ).orElse( null );
        if( aclObject == null ) {
            log.debug( "object {} not found.", objectId );
            return Lists.map( permissions, ( p ) -> false );
        }
        if( aclSubject == null ) {
            log.debug( "subject {} not found.", subjectId );
            return Lists.map( permissions, ( p ) -> false );
        }

        log.trace( "object = {}, subject = {}", aclObject, aclSubject );

        val subjects = new HashSet<String>();
        subjects.add( subjectId );
        subjects.addAll( aclSubject.ancestors );

        return Lists.map( permissions,
            ( p ) -> aclObject.acls
                .stream()
                .anyMatch( acl -> subjects.contains( acl.subjectId ) && acl.role.permissions.contains( p ) )
        );
    }

    @Override
    public boolean add( String objectId, String subjectId, String roleId, boolean inherit ) {
        log.debug( "add object = {}, subject = {}, role = {}, inherit = {}", objectId, subjectId, roleId, inherit );

        final AclRole aclRole = roleStorage.get( roleId ).orElse( null );
        if( aclRole == null ) return false;

        return objectStorage.update( objectId, aclObject -> {
            aclObject.acls.add( new AclObject.Acl( aclRole, subjectId, null, inherit ) );
            if( inherit ) {
                objectStorage
                    .select()
                    .filter( child -> child.ancestors.contains( objectId ) )
                    .forEach( childs ->
                        objectStorage.update(
                            childs.id,
                            child -> child.acls.add( new AclObject.Acl( aclRole, subjectId, objectId, false ) )
                        )
                    );
            }
        } ).isPresent();
    }

    @Override
    public boolean remove( String objectId, String subjectId, String roleId ) {
        log.debug( "remove object = {}, subject = {}, role = {}", objectId, subjectId, roleId );

        return objectStorage.update( objectId, aclObject -> {
            aclObject.acls.removeIf( acl -> {
                if( acl.subjectId.equals( subjectId ) && acl.role.id.equals( roleId ) && acl.parent == null ) {
                    if( acl.inheritance ) {
                        for( val ao : objectStorage ) {
                            if( ao.ancestors.contains( objectId ) ) {
                                objectStorage.update(
                                    ao.id,
                                    aos -> aos.acls.removeIf( aclc -> aclc.subjectId.equals( subjectId ) && aclc.role.id.equals( roleId ) )
                                );
                            }

                        }
                    }
                    return true;

                }
                return false;
            } );
        } ).isPresent();
    }

    @Override
    public List<AclRole> list( String objectId, String subjectId ) {
        log.debug( "list object = {}, subject = {}", objectId, subjectId );

        val aclObject = objectStorage.get( objectId ).orElse( null );
        if( aclObject == null ) return emptyList();

        return aclObject.acls
            .stream()
            .filter( acl -> acl.subjectId.equals( subjectId ) )
            .map( acl -> acl.role )
            .collect( toList() );
    }

    @Override
    public Optional<String> registerObject( String parentId, String type ) {
        val ancestors = new ArrayList<String>();
        val parents = new ArrayList<String>();

        if( parentId != null ) {
            val parent = objectStorage.get( parentId ).orElse( null );
            if( parent == null ) return Optional.empty();
            ancestors.add( parentId );
            parents.add( parentId );
            ancestors.addAll( parent.ancestors );
        }

        val ao = new AclObject( null, type, parents, ancestors, emptyList() );
        objectStorage.store( ao );

        return Optional.of( ao.id );
    }
}
