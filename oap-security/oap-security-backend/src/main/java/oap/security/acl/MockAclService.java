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
import oap.util.IdBean;
import oap.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static oap.util.Pair.__;

@Slf4j
public class MockAclService implements AclService {
    private final HashMap<Pair<String, String>, List<String>> checks = new HashMap<>();
    private final HashMap<String, List<String>> children = new HashMap<>();

    public void addCheck( String objectId, String subjectId, List<String> permissions ) {
        checks.put( __( objectId, subjectId ), permissions );
    }

    public void addChildren( String parentId, String type, boolean recursive, String subjectId, String permission, List<String> children ) {
        this.children.put( parentId + "_" + type + "_" + recursive + "_" + subjectId + "_" + permission, children );
    }

    @Override
    public List<SubjectRole> getSubjectRoles( String objectId, boolean inherited ) {
        return emptyList();
    }

    @Override
    public List<ObjectRole> getRoles( String userId, boolean inherited ) {
        return emptyList();
    }

    @Override
    public void validate( String objectId, String subjectId, String... permissions ) throws AclSecurityException {
        log.debug( "validate objectId={}, subjectId={}, permissions={}", objectId, subjectId, asList( permissions ) );
        var p = checkAll( objectId, subjectId );

        if( p.isEmpty() || !p.containsAll( asList( permissions ) ) ) {
            throw new AclSecurityException();
        }
    }

    @Override
    public List<Boolean> check( String objectId, String subjectId, List<String> permissions ) {
        log.debug( "check objectId={}, subjectId={}, permissions={}", objectId, subjectId, permissions );
        var p = checkAll( objectId, subjectId );

        return permissions.stream().map( p::contains ).collect( toList() );
    }

    @Override
    public List<String> checkAll( String objectId, String subjectId ) {
        return checks.getOrDefault( __( objectId, subjectId ), emptyList() );
    }

    @Override
    public boolean add( String objectId, String subjectId, String roleId, boolean inherit ) {
        return false;
    }

    @Override
    public boolean remove( String objectId, String subjectId, Optional<String> roleId ) {
        return false;
    }

    @Override
    public List<AclRole> list( String objectId, String subjectId ) {
        return emptyList();
    }

    @Override
    public List<String> getChildren( String parentId, String type, boolean recursive ) {
        return children.entrySet()
            .stream()
            .filter( e -> e.getKey().startsWith( parentId + "_" + type + "_" + recursive ) )
            .flatMap( e -> e.getValue().stream() )
            .collect( java.util.stream.Collectors.toList() );
    }

    @Override
    public List<String> getChildren( String parentId, String type, boolean recursive, String subjectId, String permission ) {
        var ret = children.get( parentId + "_" + type + "_" + recursive + "_" + subjectId + "_" + permission );
        return ret != null ? ret : emptyList();
    }

    @Override
    public Predicate<SecurityContainer<? extends IdBean>> getAclFilter( String parentId, String subjectId, String permission ) {
        return ( p ) -> false;
    }

    @Override
    public <T extends IdBean> Optional<SecurityContainer<T>> addChild( String parentId, T object, String type, String owner ) {
        return Optional.empty();
    }

    @Override
    public Optional<AclObject> addChild( String parentId, String id ) {
        return Optional.empty();
    }

    @Override
    public void unregisterObject( String objectId ) {

    }
}
