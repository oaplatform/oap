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
import lombok.ToString;
import oap.util.Mergeable;
import oap.util.Stream;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public interface AclSchema {
    void validateNewObject( AclObject parent, String newObjectType ) throws AclSecurityException;

    Optional<AclObject> getObject( String id );

    Stream<AclObject> selectObjects();

    List<AclObject> listObjects();

    Stream<AclObject> selectLocalObjects();

    Optional<AclObject> updateLocalObject( String id, Consumer<AclObject> cons );

    Iterable<AclObject> objects();

    Iterable<AclObject> localObjects();

    void deleteObject( String id );

    List<String> getPermissions( String objectId );

    AclSchemaBean addSchema( String owner, AclSchemaBean clientSchema );

    @ToString
    class AclSchemaBean implements Mergeable<AclSchemaBean>, Serializable {
        private static final long serialVersionUID = 6385590066545729318L;
        public final Set<String> permissions;
        public final Map<String, AclSchemaBean> children;
        @JsonInclude( JsonInclude.Include.NON_NULL )
        public String parentPath;

        @JsonCreator
        public AclSchemaBean( Set<String> permissions, Map<String, AclSchemaBean> children ) {
            this.permissions = permissions != null ? permissions : new HashSet<>();
            this.children = children != null ? children : new HashMap<>();
        }

        public Optional<AclSchemaBean> getChild( String type ) {
            return Optional.ofNullable( children.get( type ) );
        }

        public boolean containsChild( String objectType ) {
            return children.containsKey( objectType );
        }

        @Override
        public AclSchemaBean merge( AclSchemaBean bean ) {
            this.permissions.addAll( bean.permissions );
            this.children.putAll( bean.children );

            return this;
        }

        public AclSchemaBean findByPath( String path ) {
            var nodes = StringUtils.split( path, '.' );

            return findByPath( this, nodes, 0 );
        }

        private AclSchemaBean findByPath( AclSchemaBean schema, String[] nodes, int index ) {
            if( index >= nodes.length ) return schema;

            var node = nodes[index];
            var nodeBean = schema.children.get( node );
            if( nodeBean == null ) throw new IllegalArgumentException( "Unknown node " + node );
            return findByPath( nodeBean, nodes, index + 1 );
        }
    }
}
