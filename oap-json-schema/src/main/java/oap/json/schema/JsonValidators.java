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
package oap.json.schema;

import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonValidators {
    private static Map<String, JsonValidatorFactory> schemas = new ConcurrentHashMap<>();

    private final Map<String, JsonSchemaValidator<?>> validators = new HashMap<>();

    public JsonValidators( VL validators ) {
        for( val validator : validators ) {
            this.validators.put( validator.type, validator );
        }
    }

    public JsonValidatorFactory schema( String url, SchemaStorage storage ) {
        return schemas.computeIfAbsent( url, u -> schemaFromString( storage.get( url ), storage ) );
    }

    public JsonValidatorFactory schemaFromString( String schema, SchemaStorage storage ) {
        return new JsonValidatorFactory( schema, storage, validators );
    }

    public List<JsonSchemaValidator> validators() {
        return new ArrayList<>( validators.values() );
    }

    public static class VL extends ArrayList<JsonSchemaValidator<?>> {}

}
