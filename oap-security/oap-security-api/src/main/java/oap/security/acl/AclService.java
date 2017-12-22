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

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * Created by igor.petrenko on 20.12.2017.
 */
public interface AclService {
    void validate( String objectId, String subjectId, String... permissions ) throws AclSecurityException;

    default List<Boolean> check( String objectId, String subjectId, String... permissions ) {
        return check( objectId, subjectId, asList( permissions ) );
    }

    default boolean checkOne( String objectId, String subjectId, String permission ) {
        return check( objectId, subjectId, permission ).get( 0 );
    }

    List<Boolean> check( String objectId, String subjectId, List<String> permissions );

    boolean add( String objectId, String subjectId, String roleId, boolean inherit );

    boolean remove( String objectId, String subjectId, String roleId );

    List<AclRole> list( String objectId, String subjectId );

    Optional<String> registerObject( String parentId, String type );
}
