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
import oap.json.Binder;
import oap.json.JsonException;
import oap.json.schema.JsonValidatorFactory;
import oap.json.schema.ResourceSchemaStorage;
import oap.reflect.Reflection;
import oap.ws.WsClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonPartialValidatorPeer implements ValidatorPeer {
    private static final ResourceSchemaStorage storage = new ResourceSchemaStorage();
    private final JsonValidatorFactory factory;
    private final WsPartialValidateJson validate;
    private final Map<Reflection.Parameter, Object> values;
    private final Reflection.Method targetMethod;
    private final Object instance;

    public JsonPartialValidatorPeer( WsPartialValidateJson validate,
                                     Map<Reflection.Parameter, Object> values,
                                     Reflection.Method targetMethod, Object instance, Type type ) {
        factory = JsonValidatorFactory.schema( validate.schema(), storage );
        this.validate = validate;
        this.values = values;
        this.targetMethod = targetMethod;
        this.instance = instance;
    }

    @Override
    @SneakyThrows
    public ValidationErrors validate( Object value ) {
        try {
            final Object objectId = getValue( validate.idParameterName() );

            final String id = objectId instanceof Optional ?
                ( ( Optional ) objectId ).get().toString() : objectId.toString();

            final Object root = ( ( WsPartialValidateJson.PartialValidateJsonRootLoader<?> ) (
                validate.root().isInstance( instance )
                    ? instance : validate.root().newInstance() ) ).get( id ).orElse( null );

            if( root == null ) {
                return ValidationErrors.empty();
            }

            final Map rootMap = Binder.json.unmarshal( Map.class, Binder.json.marshal( Binder.json.clone( root ) ) );
            final Map partialValue = Binder.json.unmarshal( Map.class, ( String ) value );

            Map child = rootMap;
            for( String pathElement : validate.path().split( "(?<=})\\." ) ) {

                if( pathElement.contains( "$" ) ) {
                    final String[] split = pathElement.split( "\\." );

                    final Object next = child.get( split[0] );

                    Preconditions.checkState( next != null, "schema has no elements for value " + split[0] );
                    Preconditions.checkState( next instanceof List, split[0] + " should be of type list" );

                    final Object idValue = ( ( Optional ) getValue( split[1].replaceAll( "\\$|\\{|\\}", "" ) ) ).get();

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

    private Object getValue( final String idName ) {
        return values.entrySet().stream()
            .filter( p -> p.getKey().name().equals( idName ) )
            .findFirst()
            .get()
            .getValue();
    }

}
