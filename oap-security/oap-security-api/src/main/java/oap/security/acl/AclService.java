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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * Created by igor.petrenko on 20.12.2017.
 */
public interface AclService {
    String ROOT = "5a4200fe97684103f0d6bb17";
    String GLOBAL_ADMIN_ROLE = "5a420121976841048ccd59bd";

    List<SubjectRole> getSubjectRoles( String objectId, boolean inherited );

    List<ObjectRole> getRoles( String userId, boolean inherited );

    void validate( String objectId, String subjectId, String... permissions ) throws AclSecurityException;

    default List<Boolean> check( String objectId, String subjectId, String... permissions ) {
        return check( objectId, subjectId, asList( permissions ) );
    }

    default boolean checkOne( String objectId, String subjectId, String permission ) {
        return check( objectId, subjectId, permission ).get( 0 );
    }

    List<Boolean> check( String objectId, String subjectId, List<String> permissions );

    List<String> checkAll( String objectId, String subjectId );

    boolean add( String objectId, String subjectId, String roleId, boolean inherit );

    boolean remove( String objectId, String subjectId, String roleId );

    List<AclRole> list( String objectId, String subjectId );

    List<String> getChildren( String parentId, String type, boolean recursive );

    List<String> findChildren( String parentId, String subjectId, String type, String permission );

    <T extends AclObject> Optional<T> registerObject( String parentId, T obj );

    default <T extends AclObject> void refresh( T newObject, T oldObject ) {
        newObject.id = oldObject.id;
        newObject.owner = oldObject.owner;
        newObject.type = oldObject.type;

        newObject.parents.clear();
        newObject.parents.addAll( oldObject.parents );

        newObject.acl.ancestors.clear();
        newObject.acl.ancestors.addAll( oldObject.acl.ancestors );

        newObject.acl.acls.clear();
        newObject.acl.acls.addAll( oldObject.acl.acls );
        newObject.permissions = null;
    }

    void unregisterObject( String objectId );

    default AclObject removeAcl( AclObject value ) {
        final AclObject clone = value.clone();
        clone.acl = null;
        return clone;
    }

    @ToString
    @EqualsAndHashCode
    class ObjectRole implements Serializable {
        private static final long serialVersionUID = 5300251139316547048L;

        public final String objectId;
        public final List<AclRole> roles;

        @JsonCreator
        public ObjectRole( String objectId, List<AclRole> roles ) {
            this.objectId = objectId;
            this.roles = roles;
        }
    }

    @ToString
    @EqualsAndHashCode
    class SubjectRole implements Serializable {
        private static final long serialVersionUID = -3440629660794359704L;

        public final String subjectId;
        public final List<AclRole> roles;

        @JsonCreator
        public SubjectRole( String subjectId, List<AclRole> roles ) {
            this.subjectId = subjectId;
            this.roles = roles;
        }
    }
}
