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
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Strings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Created by igor.petrenko on 20.12.2017.
 */
@ToString
@EqualsAndHashCode
public abstract class AclObject implements Serializable {
    public final String type;
    public final AclPrivate acl;
    public LinkedHashSet<String> parents;
    public String id;
    public String owner;
    @JsonInclude( JsonInclude.Include.NON_DEFAULT )
    public List<String> permissions;

    public AclObject( String id,
                      String type,
                      List<String> parents,
                      List<String> ancestors,
                      List<Acl> acls,
                      String owner ) {
        this.id = id;
        this.type = type;
        this.parents = new LinkedHashSet<>( parents != null ? parents : emptyList() );
        acl = new AclPrivate(
            new LinkedHashSet<>( ancestors != null ? ancestors : emptyList() ),
            new LinkedHashSet<>( acls != null ? acls : emptyList() )
        );
        this.owner = owner;
    }


    public AclObject( String type ) {
        this( null, type, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Strings.UNKNOWN );
    }

    public AclObject( String id, String type ) {
        this( id, type, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Strings.UNKNOWN );
    }

    @ToString
    public static class AclPrivate {
        public final LinkedHashSet<String> ancestors;
        public final LinkedHashSet<Acl> acls;

        @JsonCreator
        public AclPrivate( LinkedHashSet<String> ancestors, LinkedHashSet<Acl> acls ) {
            this.ancestors = ancestors != null ? ancestors : new LinkedHashSet<>();
            this.acls = acls != null ? acls : new LinkedHashSet<>();
        }
    }

    @JsonInclude( JsonInclude.Include.NON_DEFAULT )
    @ToString
    @EqualsAndHashCode
    public static class Acl {
        public final AclRole role;
        public final String subjectId;
        public final String parent;
        public final boolean inheritance;

        public Acl( AclRole role, String subjectId, String parent, boolean inheritance ) {
            this.role = role;
            this.subjectId = subjectId;
            this.parent = parent;
            this.inheritance = inheritance;
        }

        public Acl cloneWithParent( String parent ) {
            return new Acl( role, subjectId, parent, inheritance );
        }
    }
}
