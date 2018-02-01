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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.Resources;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.storage.Storage;
import oap.util.Lists;
import oap.util.Strings;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * Created by igor.petrenko on 29.12.2017.
 */
@Slf4j
public class DefaultAclSchema implements AclSchema {
    private final Map<String, Storage<? extends SecurityContainer<?>>> objectStorage;
    private final Optional<AclSchema> remoteSchema;
    private final AclSchemaBean schema;

    @JsonCreator
    public DefaultAclSchema( Map<String, Storage<? extends SecurityContainer<?>>> objectStorage, String schema, AclSchema remoteSchema ) {
        this.objectStorage = objectStorage;
        this.remoteSchema = Optional.ofNullable( remoteSchema );

        log.info( "acl schema path = {}", schema );

        final List<URL> urls = Resources.urls( schema );
        log.debug( "found {}", urls );

        final Optional<URL> url = Resources.url( getClass(), schema );
        log.debug( "found2 {}", url );

        val configs = Lists.tail( urls ).stream().map( Strings::readString ).toArray( String[]::new );

        this.schema = Binder.hoconWithConfig( configs ).unmarshal( new TypeRef<AclSchemaBean>() {}, Lists.head( urls ) );

        log.info( "acl schema = {}", this.schema );
    }

    @Override
    public void validateNewObject( AclObject parent, String newObjectType ) throws AclSecurityException {
        log.trace( "validateNewObject parent = {}, newObjectType = {}", parent, newObjectType );

        val parentSchema = getSchemas( parent );
        if( parentSchema.stream().noneMatch( schema -> schema.containsChild( newObjectType ) ) ) {
            throw new AclSecurityException( newObjectType + " is not allowed here." );
        }
    }

    @Override
    public Optional<AclObject> getObject( String id ) {
        for( val storage : objectStorage.values() ) {
            Optional<? extends SecurityContainer<?>> con;
            if( ( con = storage.get( id ) ).isPresent() ) return con.map( c -> c.acl );
        }

        return remoteSchema.flatMap( rs -> rs.getObject( id ) );
    }

    @Override
    public Stream<AclObject> selectObjects() {
        return remoteSchema
            .map( rs -> Stream.concat( selectLocalObjects(), rs.selectObjects() ) )
            .orElse( selectLocalObjects() );
    }

    @Override
    public Stream<AclObject> selectLocalObjects() {
        return objectStorage.values()
            .stream()
            .flatMap( Storage::select )
            .map( con -> con.acl );
    }

    @Override
    public Optional<AclObject> updateLocalObject( String id, Consumer<AclObject> cons ) {
        for( val os : objectStorage.values() ) {
            val res = os.update( id, con -> {
                cons.accept( con.acl );
                return con;
            } );

            if( res.isPresent() ) return res.map( r -> r.acl );
        }

        return Optional.empty();
    }

    @Override
    public Iterable<AclObject> objects() {
        return () -> selectObjects().iterator();
    }

    @Override
    public Iterable<AclObject> localObjects() {
        return () -> selectLocalObjects().iterator();
    }

    @Override
    public void deleteObject( String id ) {
        for( val os : objectStorage.values() ) {
            if( os.delete( id ).isPresent() ) return;
        }

        remoteSchema.ifPresent( rs -> rs.deleteObject( id ) );
    }

    @Override
    public List<String> getPermissions( String objectId ) {
        val object = getObject( objectId ).orElse( null );
        if( object == null ) return emptyList();

        val objectSchema = getSchemas( object );

        return objectSchema
            .stream()
            .flatMap( os -> os.permissions.stream() )
            .distinct()
            .collect( toList() );
    }

    @SuppressWarnings( "unchecked" )
    private List<AclSchemaBean> getSchemas( AclObject parent ) {
        if( parent == null ) return singletonList( schema );
        if( parent.parents.isEmpty() )
            return schema.getChild( parent.type )
                .map( Collections::singletonList )
                .orElse( Collections.emptyList() );

        return parent.parents
            .stream()
            .flatMap( id ->
                getSchemas( getObject( id ).orElseThrow( () -> new IllegalStateException( "Unknown object " + id ) ) )
                    .stream()
                    .flatMap( aclType ->
                        aclType.getChild( parent.type )
                            .map( Stream::of )
                            .orElse( Stream.empty() ) )
            )
            .collect( toList() );
    }

    @ToString
    private static class AclSchemaBean {
        public final List<String> permissions;
        public final Map<String, AclSchemaBean> children;

        @JsonCreator
        public AclSchemaBean( List<String> permissions, Map<String, AclSchemaBean> children ) {
            this.permissions = permissions;
            this.children = children;
        }

        public Optional<AclSchemaBean> getChild( String type ) {
            return Optional.ofNullable( children.get( type ) );
        }

        public boolean containsChild( String objectType ) {
            return children.containsKey( objectType );
        }
    }
}
