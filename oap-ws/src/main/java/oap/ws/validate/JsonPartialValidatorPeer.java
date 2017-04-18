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

import lombok.SneakyThrows;
import lombok.val;
import oap.json.Binder;
import oap.json.JsonException;
import oap.json.schema.JsonValidatorFactory;
import oap.json.schema.ResourceSchemaStorage;
import oap.reflect.Reflection;
import oap.ws.WsClientException;

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
            final String idName = validate.idParameterName();
            final Object objectId = values.entrySet()
                .stream()
                .filter( p -> p.getKey().name().equals( idName ) )
                .findFirst()
                .get()
                .getValue();

            final String id = objectId instanceof Optional ?
                ( ( Optional ) objectId ).get().toString() : objectId.toString();

            final Object root = ( ( WsPartialValidateJson.PartialValidateJsonRootLoader<?> ) (
                validate.root().isInstance( instance )
                    ? instance : validate.root().newInstance() ) ).get( id ).orElse( null );

            if( root == null ) return ValidationErrors.empty();

            final Object rootMap = Binder.json.unmarshal( Map.class, Binder.json.marshal( root ) );

            val partialValue = Binder.json.unmarshal( Map.class, ( String ) value );
            return ValidationErrors.errors( factory.partialValidate( rootMap, partialValue, validate.path(), validate.ignoreRequired() ) );
        } catch( JsonException e ) {
            throw new WsClientException( e.getMessage(), e );
        }
    }
}
