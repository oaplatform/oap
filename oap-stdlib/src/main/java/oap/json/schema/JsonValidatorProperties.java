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

import lombok.extern.slf4j.Slf4j;
import oap.util.function.TriFunction;

import java.util.List;
import java.util.Optional;

@Slf4j
public class JsonValidatorProperties {
    public final Optional<Boolean> additionalProperties;
    public final boolean ignoreRequiredDefault;
    public final Object rootJson;
    public final AbstractSchemaAST rootSchema;
    public final TriFunction<JsonValidatorProperties, AbstractSchemaAST, Object, List<String>>
        validator;
    public final Optional<String> path;
    public final Optional<String> prefixPath;

    public JsonValidatorProperties(
        AbstractSchemaAST rootSchema,
        Object rootJson,
        Optional<String> prefixPath,
        Optional<String> path,
        Optional<Boolean> additionalProperties,
        boolean ignoreRequiredDefault,
        TriFunction<JsonValidatorProperties, AbstractSchemaAST, Object, List<String>> validator ) {
        this.rootSchema = rootSchema;
        this.rootJson = rootJson;
        this.prefixPath = prefixPath;
        this.path = path;
        this.additionalProperties = additionalProperties;
        this.ignoreRequiredDefault = ignoreRequiredDefault;
        this.validator = validator;
    }

    public JsonValidatorProperties withPath( String path ) {
        Optional<String> jsonPathModified = this.path.map( p -> p + "/" + path );
//        jsonPathModified.ifPresent( x -> log.trace( "JSON path: {}", x ) );
        Optional<String> jsonPath = jsonPathModified.or( () -> Optional.of( path ) );
        return new JsonValidatorProperties( rootSchema, rootJson, prefixPath, jsonPath, additionalProperties, ignoreRequiredDefault, validator );
    }

    public JsonValidatorProperties withAdditionalProperties( Optional<Boolean> additionalProperties ) {
        return additionalProperties.map( ap -> new JsonValidatorProperties(
            rootSchema, rootJson, prefixPath, path, additionalProperties,
            ignoreRequiredDefault, validator ) )
            .orElse( this );
    }


    public String error( String message ) {
        return error( path, message );
    }

    public String error( Optional<String> path, String message ) {
        return path.map( p -> "/" + p + ": " ).orElse( "" ) + message;
    }
}
