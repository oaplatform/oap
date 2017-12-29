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
import oap.storage.Storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Created by igor.petrenko on 29.12.2017.
 */
@Slf4j
public class DefaultAclSchema implements AclSchema {
    private final Storage<AclObject> objectStorage;
    private final AclType schema;

    @JsonCreator
    public DefaultAclSchema( Storage<AclObject> objectStorage, AclType schema ) {
        this.objectStorage = objectStorage;
        this.schema = schema;

        log.info( "acl schema = {}", schema );
    }

    @Override
    public void validateNewObject( AclObject parent, String newObjectType ) throws AclSecurityException {
        log.trace( "validateNewObject parent = {}, newObjectType = {}", parent, newObjectType );

        val parentSchema = getSchemas( parent );
        if( parentSchema.stream().noneMatch( schema -> schema.containsKey( newObjectType ) ) ) {
            throw new AclSecurityException( newObjectType + " is not allowed here." );
        }
    }

    private List<AclType> getSchemas( AclObject parent ) {
        if( parent == null ) return singletonList( schema );
        if( parent.parents.isEmpty() ) return Optional.ofNullable( schema.get( parent.type ) ).orElse( emptyList() );

        return parent.parents
            .stream()
            .flatMap( id ->
                getSchemas( objectStorage.get( id ).get() )
                    .stream()
                    .flatMap( aclType ->
                        Optional.ofNullable( aclType.get( parent.type ) )
                            .map( Collection::stream )
                            .orElse( Stream.<AclType>empty() ) )
            )
            .collect( Collectors.toList() );
    }

}
