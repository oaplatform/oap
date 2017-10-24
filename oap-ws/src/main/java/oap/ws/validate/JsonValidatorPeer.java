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

import oap.json.Binder;
import oap.json.JsonException;
import oap.json.schema.JsonValidatorFactory;
import oap.json.schema.JsonValidators;
import oap.json.schema.ResourceSchemaStorage;
import oap.reflect.Reflection;
import oap.ws.WsClientException;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonValidatorPeer implements ValidatorPeer {
    private static final ResourceSchemaStorage storage = new ResourceSchemaStorage();
    private final JsonValidatorFactory factory;
    private final WsValidateJson validate;

    public JsonValidatorPeer( WsValidateJson validate,
                              Reflection.Method targetMethod, Object instance, Type type, JsonValidators jsonValidators ) {
        this.factory = jsonValidators.schema( validate.schema(), storage );
        this.validate = validate;
    }

    @Override
    public ValidationErrors validate( Object value, LinkedHashMap<Reflection.Parameter, Object> originalValues ) {
        try {
            final Map mapValue = Binder.json.unmarshal( Map.class, ( String ) value );

            return ValidationErrors.errors( factory.validate( mapValue, validate.ignoreRequired() ) );
        } catch( JsonException e ) {
            throw new WsClientException( e.getMessage(), e );
        }
    }
}
