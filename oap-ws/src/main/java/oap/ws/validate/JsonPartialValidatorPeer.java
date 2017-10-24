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

package oap.ws.validate;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.json.JsonException;
import oap.json.schema.JsonValidatorFactory;
import oap.json.schema.JsonValidators;
import oap.json.schema.ResourceSchemaStorage;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.ws.WsClientException;
import oap.ws.WsException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class JsonPartialValidatorPeer implements ValidatorPeer {
    private static final ResourceSchemaStorage storage = new ResourceSchemaStorage();
    private final JsonValidatorFactory factory;
    private final WsPartialValidateJson validate;
    private final Reflection.Method method;
    private final Object instance;

    public JsonPartialValidatorPeer( WsPartialValidateJson validate,
                                     Reflection.Method targetMethod, Object instance, Type type, JsonValidators jsonValidators ) {
        factory = jsonValidators.schema( validate.schema(), storage );
        this.validate = validate;
        this.instance = instance;
        this.method = Reflect.reflect( instance.getClass() )
            .method( validate.methodName() )
            .orElseThrow( () -> new WsException( "No such method " + validate.methodName() ) );
    }

    private static Object getValue( final LinkedHashMap<Reflection.Parameter, Object> originalValues,
                                    final String idName ) {
        return originalValues.entrySet().stream()
            .filter( p -> p.getKey().name().equals( idName ) )
            .findFirst()
            .get()
            .getValue();
    }

    @Override
    @SneakyThrows
    public ValidationErrors validate( Object value, LinkedHashMap<Reflection.Parameter, Object> originalValues ) {
        try {
            final Object objectId = getValue( originalValues, validate.idParameterName() );

            final String id = objectId instanceof Optional ?
                ( ( Optional ) objectId ).get().toString() : objectId.toString();

            final Object root = method.invoke( instance, id );

            if( root == null ) {
                return ValidationErrors.empty();
            }

            final String fetchedRoot = Binder.json.marshal( Binder.json.clone( root ) );

            log.trace( "Retrieved object [{}] with id [{}]", fetchedRoot, id );

            final Map rootMap = Binder.json.unmarshal( Map.class, fetchedRoot );
            final Map partialValue = Binder.json.unmarshal( Map.class, ( String ) value );

            Map child = rootMap;
            for( String pathElement : validate.path().split( "(?<=})\\." ) ) {

                if( pathElement.contains( "$" ) ) {
                    final String[] split = pathElement.split( "\\." );

                    final Object next = child.get( split[0] );

                    Preconditions.checkState( next != null, "schema has no elements for value " + split[0] );
                    Preconditions.checkState( next instanceof List, split[0] + " should be of type list" );

                    final Object idValue = ( ( Optional ) getValue( originalValues,
                        split[1].replaceAll( "\\$|\\{|\\}", "" ) ) ).get();

                    final Optional matchedChild = ( ( List ) next ).stream()
                        .filter( o -> idValue.equals( ( ( Map ) o ).get( "id" ).toString() ) )
                        .findFirst();

                    child = ( Map ) matchedChild.get();

                    continue;
                }

                final Object next = child.get( pathElement );
                if( next == null ) {
                    child.put( pathElement, Collections.singletonList( partialValue ) );

                    break;
                } else if( next instanceof List ) {
                    final List childElements = ( List ) next;

                    childElements.removeIf( elements -> {
                        final Object partialValueId = partialValue.get( "id" );

                        return partialValueId != null && partialValueId.toString().equals( ( ( Map ) elements ).get( "id" ).toString() );
                    } );

                    childElements.add( partialValue );

                    child.put( pathElement, childElements );

                    break;
                } else {
                    child = ( Map ) next;
                }
            }

            return ValidationErrors.errors( factory.validate( rootMap, validate.ignoreRequired() ) );
        } catch( JsonException e ) {
            throw new WsClientException( e.getMessage(), e );
        }
    }

}
