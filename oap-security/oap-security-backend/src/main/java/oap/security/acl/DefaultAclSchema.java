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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryParser;
import oap.storage.Storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static oap.dictionary.DictionaryParser.INCREMENTAL_ID_STRATEGY;

/**
 * Created by igor.petrenko on 29.12.2017.
 */
@Slf4j
public class DefaultAclSchema implements AclSchema {
    private final Map<String, Storage<? extends AclObject>> objectStorage;
    private final Dictionary schema;

    @JsonCreator
    public DefaultAclSchema( Map<String, Storage<? extends AclObject>> objectStorage, String schema ) {
        this.objectStorage = new HashMap<>( objectStorage );
        this.objectStorage.put( "root", new RootStorage() );

        log.info( "acl schema path = {}", schema );

        this.schema = DictionaryParser.parse( schema, INCREMENTAL_ID_STRATEGY );

        log.info( "acl schema = {}", this.schema );
    }

    @Override
    public void validateNewObject( AclObject parent, String newObjectType ) throws AclSecurityException {
        log.trace( "validateNewObject parent = {}, newObjectType = {}", parent, newObjectType );

        val parentSchema = getSchemas( parent );
        if( parentSchema.stream().noneMatch( schema -> schema.containsValueWithId( newObjectType ) ) ) {
            throw new AclSecurityException( newObjectType + " is not allowed here." );
        }
    }

    @Override
    public Optional<? extends AclObject> getObject( String id ) {
        for( val storage : objectStorage.values() ) {
            AclObject obj;
            if( ( obj = storage.get( id ).orElse( null ) ) != null ) return Optional.of( obj );
        }

        return Optional.empty();
    }

    @Override
    public Stream<AclObject> selectObjects() {
        return objectStorage.values()
            .stream()
            .flatMap( Storage::select );
    }

    @Override
    public Optional<? extends AclObject> updateObject( String id, Consumer<AclObject> cons ) {
        for( val os : objectStorage.values() ) {
            val res = os.update( id, ( o ) -> {
                cons.accept( o );
                return o;
            } );

            if( res.isPresent() ) return res;
        }

        return Optional.empty();
    }

    @Override
    public Iterable<AclObject> objects() {
        return () -> selectObjects().iterator();
    }

    @Override
    public void deleteObject( String id ) {
        for( val os : objectStorage.values() ) {
            if( os.delete( id ).isPresent() ) return;
        }
    }

    @Override
    public List<String> getPermissions( String objectId ) {
        val object = getObject( objectId ).orElse( null );
        if( object == null ) return emptyList();

        val objectSchema = getSchemas( object );

        return objectSchema
            .stream()
            .flatMap( os -> ( ( List<String> ) os.getProperty( "permissions" ).orElse( emptyList() ) ).stream() )
            .distinct()
            .collect( toList() );
    }

    @SuppressWarnings( "unchecked" )
    private List<Dictionary> getSchemas( AclObject parent ) {
        if( parent == null ) return singletonList( schema );
        if( parent.parents.isEmpty() )
            return Optional.ofNullable( schema.getValue( parent.type ) )
                .map( Collections::singletonList ).orElse( emptyList() );

        return parent.parents
            .stream()
            .flatMap( id ->
                getSchemas( getObject( id ).get() )
                    .stream()
                    .flatMap( aclType ->
                        Optional.ofNullable( aclType.getValue( parent.type ) )
                            .map( Stream::of )
                            .orElse( Stream.empty() ) )
            )
            .collect( toList() );
    }

}
