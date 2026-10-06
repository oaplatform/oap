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

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class JsonValidatorProperties {
    public final Optional<Boolean> additionalProperties;
    public final boolean ignoreRequiredDefault;
    public final boolean forceIgnoreAdditionalProperties;
    public final Object rootJson;
    public final AbstractSchemaAST rootSchema;
    public final TriFunction<JsonValidatorProperties, AbstractSchemaAST, Object, List<JsonSchemaError>>
        validator;
    public final Optional<String> path;
    public final Optional<String> prefixPath;

    public JsonValidatorProperties(
        AbstractSchemaAST rootSchema,
        Object rootJson,
        Optional<String> prefixPath,
        Optional<String> path,
        boolean forceIgnoreAdditionalProperties,
        Optional<Boolean> additionalProperties,
        boolean ignoreRequiredDefault,
        TriFunction<JsonValidatorProperties, AbstractSchemaAST, Object, List<JsonSchemaError>> validator ) {
        this.rootSchema = rootSchema;
        this.rootJson = rootJson;
        this.prefixPath = prefixPath;
        this.path = path;
        this.forceIgnoreAdditionalProperties = forceIgnoreAdditionalProperties;
        this.additionalProperties = additionalProperties;
        this.ignoreRequiredDefault = ignoreRequiredDefault;
        this.validator = validator;
    }

    public JsonValidatorProperties withPath( String path ) {
        Optional<String> jsonPathModified = this.path.map( p -> p + "/" + path );
//        jsonPathModified.ifPresent( x -> log.trace( "JSON path: {}", x ) );
        Optional<String> jsonPath = jsonPathModified.or( () -> Optional.of( path ) );
        return new JsonValidatorProperties( rootSchema, rootJson, prefixPath, jsonPath, forceIgnoreAdditionalProperties, additionalProperties, ignoreRequiredDefault, validator );
    }

    public JsonValidatorProperties withAdditionalProperties( Optional<Boolean> additionalProperties ) {
        return additionalProperties.map( ap -> new JsonValidatorProperties(
            rootSchema, rootJson, prefixPath, path, forceIgnoreAdditionalProperties, additionalProperties,
            ignoreRequiredDefault, validator ) )
            .orElse( this );
    }

    public JsonValidatorProperties withoutAdditionalProperties() {
        return new JsonValidatorProperties( rootSchema, rootJson, prefixPath, path, forceIgnoreAdditionalProperties,
            Optional.empty(), ignoreRequiredDefault, validator );
    }


    /** The error of {@code keyword} with its message template; the path (if any) is prefixed to the template. */
    public JsonSchemaError error( JsonSchemaValidatorErrors keyword, Map<String, Object> args ) {
        Map<String, Object> values = new LinkedHashMap<>( args );
        String template = keyword.template;
        if( path.isPresent() ) {
            values.put( "path", path.get() );
            template = "/${path}: " + template;
        }
        return new JsonSchemaError( keyword.code, template, values );
    }

    /** As {@link #error(JsonSchemaValidatorErrors, Map)}, but a custom {@code errorMessage} of the schema replaces the template. */
    public JsonSchemaError error( AbstractSchemaAST schema, JsonSchemaValidatorErrors keyword, Map<String, Object> args ) {
        Optional<String> custom = keyword.keyword == null ? Optional.empty() : schema.common.errorMessage( keyword.keyword );
        if( custom.isEmpty() ) return error( keyword, args );

        Object[] fmtArgs = new Object[ keyword.placeholders().size() + 1 ];
        fmtArgs[0] = path.orElse( "" );
        for( int i = 0; i < keyword.placeholders().size(); i++ )
            fmtArgs[i + 1] = args.get( keyword.placeholders().get( i ) );
        return new JsonSchemaError( keyword.code, MessageFormat.format( custom.get(), fmtArgs ), Map.of() );
    }

    public JsonSchemaError requiredError( AbstractSchemaAST schema, String propertyName ) {
        Optional<String> custom = schema.common.errorMessage( "required", propertyName );
        if( custom.isEmpty() ) return error( JsonSchemaValidatorErrors.REQUIRED, Map.of() );
        return new JsonSchemaError( JsonSchemaValidatorErrors.REQUIRED.code, MessageFormat.format( custom.get(), path.orElse( "" ) ), Map.of() );
    }
}
