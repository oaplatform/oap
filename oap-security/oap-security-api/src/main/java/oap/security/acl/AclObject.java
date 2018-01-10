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
import oap.json.Binder;
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
@JsonInclude( JsonInclude.Include.NON_DEFAULT )
public class AclObject implements Serializable, Cloneable {
    public final LinkedHashSet<String> ancestors;
    public final LinkedHashSet<Acl> acls;
    public String type;
    public LinkedHashSet<String> parents;
    public String id;
    public String owner;

    @JsonCreator
    public AclObject( String id,
                      String type,
                      List<String> parents,
                      List<String> ancestors,
                      List<Acl> acls,
                      String owner ) {
        this.id = id;
        this.type = type;
        this.parents = new LinkedHashSet<>( parents != null ? parents : emptyList() );
        this.ancestors = new LinkedHashSet<>( ancestors != null ? ancestors : emptyList() );
        this.acls = new LinkedHashSet<>( acls != null ? acls : emptyList() );
        this.owner = owner;
    }

    public AclObject( String type,
                      List<String> parents,
                      List<String> ancestors,
                      List<Acl> acls,
                      String owner ) {
        this( null, type, parents, ancestors, acls, owner );
    }


    public AclObject( String type ) {
        this( null, type, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Strings.UNKNOWN );
    }

    public AclObject( String id, String type ) {
        this( id, type, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Strings.UNKNOWN );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    protected AclObject clone() {
        return Binder.json.clone( this );
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
