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

import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.json.JsonException;
import oap.json.schema.JsonSchema;
import oap.reflect.Reflection;
import oap.util.Strings;
import oap.ws.WsClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class JsonValidatorPeer implements ValidatorPeer {
    private final ConcurrentHashMap<String, JsonSchema> cache = new ConcurrentHashMap<>();
    private final WsValidateJson validate;
    private final String schemaRef;
    private final boolean dynamic;

    public JsonValidatorPeer( WsValidateJson validate, Reflection.Method targetMethod, Object instance, Type type ) {
        this.schemaRef = validate.schema();
        this.dynamic = this.schemaRef.contains( "${" );
        this.validate = validate;
    }

    @Override
    public ValidationErrors validate( Object value, Map<Reflection.Parameter, Object> originalValues ) {
        try {
            var mapValue = Binder.json.unmarshal( Map.class, ( String ) value );
            var factory = getJsonSchema( originalValues );
            return ValidationErrors.errors( factory.validate( mapValue, validate.ignoreRequired() ) );
        } catch( JsonException e ) {
            throw new WsClientException( e.getMessage(), e );
        }
    }

    private JsonSchema getJsonSchema( Map<Reflection.Parameter, Object> originalValues ) {
        if( !dynamic ) return cache.computeIfAbsent( Strings.UNDEFINED, s -> JsonSchema.schema( schemaRef ) );

        log.trace( "dynamic schema ref {}", schemaRef );

        StringBuilder id = new StringBuilder();

        var ref = Strings.substitute( schemaRef, key -> originalValues
            .entrySet()
            .stream()
            .filter( e -> key.equals( e.getKey().name() ) )
            .map( e -> {
                var value = e.getValue().toString();
                log.trace( "key={}, value={}", e.getKey(), e.getValue() );
                id.append( value );
                return value;
            } )
            .findAny()
            .orElse( Strings.UNKNOWN ) );

        return cache.computeIfAbsent( id.toString(), i -> JsonSchema.schema( ref ) );
    }
}
