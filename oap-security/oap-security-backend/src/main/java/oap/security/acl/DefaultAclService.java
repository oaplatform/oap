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

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.storage.Storage;
import oap.util.Lists;
import org.testng.collections.SetMultiMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * Created by igor.petrenko on 21.12.2017.
 */
@Slf4j
public class DefaultAclService implements AclService {
    private final Storage<AclRole> roleStorage;
    private final AclSchema schema;

    public DefaultAclService( Storage<AclRole> roleStorage, AclSchema schema ) {
        this.roleStorage = roleStorage;
        this.schema = schema;
    }

    public void start() {
        if( !roleStorage.get( GLOBAL_ADMIN_ROLE ).isPresent() ) {
            roleStorage.store( new AclRole( GLOBAL_ADMIN_ROLE, "ALL", singletonList( "*" ) ) );
        }
    }

    @Override
    public void validate( String objectId, String subjectId, String... permissions ) throws AclSecurityException {
        if( check( objectId, subjectId, asList( permissions ) ).indexOf( false ) >= 0 )
            throw new AclSecurityException();
    }

    @Override
    public List<String> checkAll( String objectId, String subjectId ) {
        log.debug( "checkAll object = {}, subject = {}", objectId, subjectId );

        val permissions = schema.getPermissions( objectId );
        val res = check( objectId, subjectId, permissions );

        val ret = new ArrayList<String>();

        for( int i = 0; i < permissions.size(); i++ ) {
            if( res.get( i ) ) ret.add( permissions.get( i ) );
        }

        return ret;
    }

    @Override
    public List<Boolean> check( String objectId, String subjectId, List<String> permissions ) {
        log.debug( "check object = {}, subject = {}, permissions = {}", objectId, subjectId, permissions );

        val aclObject = schema.getObject( objectId ).orElse( null );
        val aclSubject = schema.getObject( subjectId ).orElse( null );
        if( aclObject == null ) {
            log.debug( "object {} not found.", objectId );
            return Lists.map( permissions, ( p ) -> false );
        }
        if( aclSubject == null ) {
            log.debug( "subject {} not found.", subjectId );
            return Lists.map( permissions, ( p ) -> false );
        }

        log.trace( "object = {}, subject = {}", aclObject, aclSubject );

        if( objectId.equals( subjectId ) ) return Lists.map( permissions, ( p ) -> true );

        val subjects = new HashSet<String>();
        subjects.add( subjectId );
        subjects.addAll( aclSubject.acl.ancestors );

        return Lists.map( permissions,
            ( p ) -> aclObject.acl.acls
                .stream()
                .anyMatch( acl -> subjects.contains( acl.subjectId )
                    && ( acl.role.permissions.contains( "*" ) || acl.role.permissions.contains( p ) ) )
        );
    }

    @Override
    public boolean add( String objectId, String subjectId, String roleId, boolean inherit ) {
        log.debug( "add object = {}, subject = {}, role = {}, inherit = {}", objectId, subjectId, roleId, inherit );

        final AclRole aclRole = roleStorage.get( roleId ).orElse( null );
        if( aclRole == null ) return false;

        return schema.updateObject( objectId, aclObject -> {
            aclObject.acl.acls.add( new AclObject.Acl( aclRole, subjectId, null, inherit ) );
            if( inherit ) {
                schema
                    .selectObjects()
                    .filter( child -> child.acl.ancestors.contains( objectId ) )
                    .forEach( childs ->
                        schema.updateObject(
                            childs.id,
                            child -> {
                                child.acl.acls.add( new AclObject.Acl( aclRole, subjectId, objectId, false ) );
                            }
                        )
                    );
            }
        } ).isPresent();
    }

    @Override
    public boolean remove( String objectId, String subjectId, String roleId ) {
        log.debug( "remove object = {}, subject = {}, role = {}", objectId, subjectId, roleId );

        return schema.updateObject( objectId, aclObject -> {
            aclObject.acl.acls.removeIf( acl -> {
                if( acl.subjectId.equals( subjectId ) && acl.role.id.equals( roleId ) && acl.parent == null ) {
                    if( acl.inheritance ) {
                        for( val ao : schema.objects() ) {
                            if( ao.acl.ancestors.contains( objectId ) ) {
                                schema.updateObject(
                                    ao.id,
                                    aos -> {
                                        aos.acl.acls.removeIf( aclc -> aclc.subjectId.equals( subjectId ) && aclc.role.id.equals( roleId ) );
                                    }
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

        val aclObject = schema.getObject( objectId ).orElse( null );
        if( aclObject == null ) return emptyList();

        return aclObject.acl.acls
            .stream()
            .filter( acl -> acl.subjectId.equals( subjectId ) )
            .map( acl -> acl.role )
            .collect( toList() );
    }

    @Override
    public List<String> getChildren( String parentId, String type, boolean recursive ) {
        return schema
            .selectObjects()
            .filter( obj ->
                ( recursive ? obj.acl.ancestors.contains( parentId ) : obj.parents.contains( parentId ) )
                    && obj.type.equals( type ) )
            .map( obj -> obj.id )
            .collect( toList() );
    }

    @Override
    public List<String> findChildren( String parentId, String subjectId, String type, String permission ) {
        val aclSubject = schema.getObject( subjectId ).orElse( null );
        if( aclSubject == null ) {
            log.debug( "subject {} not found.", subjectId );
            return emptyList();
        }
        val subjects = new HashSet<String>();
        subjects.add( subjectId );
        subjects.addAll( aclSubject.acl.ancestors );

        return schema
            .selectObjects()
            .filter( obj ->
                obj.acl.ancestors.contains( parentId )
                    && obj.type.equals( type )
                    && obj.acl.acls.stream().anyMatch( acl -> subjects.contains( acl.subjectId ) ) )
            .map( obj -> obj.id )
            .collect( toList() );
    }

    @Override
    public <T extends AclObject> Optional<T> registerObject( String parentId, T obj ) {
        Preconditions.checkNotNull( parentId );

        val parent = schema.getObject( parentId ).orElse( null );
        if( parent == null ) return Optional.empty();

        schema.validateNewObject( parent, obj.type );

        val parents = new ArrayList<String>();
        parents.add( parentId );

        val ancestors = new ArrayList<String>( parent.acl.ancestors );
        ancestors.add( parentId );

        val acls = parent.acl.acls
            .stream()
            .filter( acl -> acl.inheritance )
            .map( acl -> acl.parent == null ? acl.cloneWithParent( parent.id ) : acl )
            .collect( toList() );

        obj.parents.clear();
        obj.parents.addAll( parents );

        obj.acl.ancestors.clear();
        obj.acl.ancestors.addAll( ancestors );

        obj.acl.acls.clear();
        obj.acl.acls.addAll( acls );

        return Optional.of( obj );
    }

    @Override
    public void unregisterObject( String objectId ) {
        if( !schema.getObject( objectId ).isPresent() )
            throw new AclSecurityException( "Object '" + objectId + "' not found" );

        if( schema.selectObjects().anyMatch( obj -> obj.parents.contains( objectId ) ) )
            throw new AclSecurityException( "Group '" + objectId + "' not empty" );

        for( val obj : schema.objects() ) {
            if( obj.acl.acls.stream().anyMatch( acl -> acl.subjectId.equals( objectId ) ) ) {
                schema.updateObject( obj.id, o -> {
                    o.acl.acls.removeIf( acl -> acl.subjectId.equals( objectId ) );
                } );
            }
        }

        schema.deleteObject( objectId );
    }

    @Override
    public List<SubjectRole> getSubjectRoles( String objectId, boolean inherited ) {
        val obj = schema.getObject( objectId ).orElse( null );
        if( obj == null ) return emptyList();

        val map = new SetMultiMap<String, AclRole>( false );

        for( val acl : obj.acl.acls ) {
            if( inherited || acl.parent == null )
                map.put( acl.subjectId, acl.role );
        }

        return map
            .entrySet()
            .stream()
            .map( e -> new SubjectRole( e.getKey(), new ArrayList<>( e.getValue() ) ) )
            .collect( toList() );
    }

    @Override
    public List<ObjectRole> getRoles( String userId, boolean inherited ) {
        val map = new SetMultiMap<String, AclRole>( false );

        for( val obj : schema.objects() ) {
            for( val acl : obj.acl.acls ) {
                if( acl.subjectId.equals( userId ) )
                    if( inherited || acl.parent == null )
                        map.put( obj.id, acl.role );
            }
        }

        return map
            .entrySet()
            .stream()
            .map( e -> new ObjectRole( e.getKey(), new ArrayList<>( e.getValue() ) ) )
            .collect( toList() );
    }
}
