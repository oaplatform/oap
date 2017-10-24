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
import oap.json.Binder;
import oap.util.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class JsonValidatorFactory {
    public final SchemaAST schema;
    private final Map<String, JsonSchemaValidator<?>> validators;

    JsonValidatorFactory( String schema, SchemaStorage storage, Map<String, JsonSchemaValidator<?>> validators ) {
        this.validators = validators;
        final JsonSchemaParserContext context = new JsonSchemaParserContext(
            "", null, "",
            this::parse,
            ( rp, url ) -> parse( url, storage.get( url ), storage, rp ),
            "", "", new HashMap<>(), new HashMap<>() );

        this.schema = parse( schema, context ).unwrap( context );
    }

    private SchemaASTWrapper parse( String schema, JsonSchemaParserContext context ) {
        return parse( context.withNode( "", Binder.hocon.unmarshal( Object.class, schema ) ) );
    }

    @SuppressWarnings( "unchecked" )
    private List<String> validate( JsonValidatorProperties properties, SchemaAST schema, Object value ) {
        JsonSchemaValidator jsonSchemaValidator = validators.get( schema.common.schemaType );
        if( jsonSchemaValidator == null ) {
            log.trace( "registered validators: " + validators.keySet() );
            throw new ValidationSyntaxException( "[schema:type]: unknown simple type [" + schema.common.schemaType + "]" );
        }

        if( value == null && !properties.ignoreRequiredDefault
            && schema.common.required.orElse( BooleanReference.FALSE )
            .apply( properties.rootJson, value, properties.path, properties.prefixPath ) )
            return Lists.of( properties.error( "required property is missing" ) );
        else if( value == null ) return Lists.empty();
        else {
            List<String> errors = jsonSchemaValidator.validate( properties, schema, value );
            schema.common.enumValue
                .filter( e -> !e.apply( properties.rootJson, properties.path ).contains( value ) )
                .ifPresent( e -> errors.add( properties.error( "instance does not match any member resolve the enumeration "
                    + e.apply( properties.rootJson, properties.path ) ) ) );
            return errors;
        }
    }

    SchemaASTWrapper parse( String schema, SchemaStorage storage ) {
        return parse( "", schema, storage, "" );
    }

    SchemaASTWrapper parse( String schemaName, String schema, SchemaStorage storage, String rootPath ) {
        final JsonSchemaParserContext context = new JsonSchemaParserContext(
            schemaName,
            null, "",
            this::parse,
            ( rp, url ) -> parse( url, storage.get( url ), storage, rp ),
            rootPath, "", new HashMap<>(), new HashMap<>() );
        return parse( schema, context );
    }

    private SchemaASTWrapper parse( JsonSchemaParserContext context ) {
        JsonSchemaValidator<?> schemaParser = validators.get( context.schemaType );
        if( schemaParser != null ) {
            return schemaParser.parse( context );
        } else {
            log.trace( "registered parsers: {}", validators.keySet() );
            throw new ValidationSyntaxException(
                "[schema:type]: unknown simple type [" + context.schemaType + "]" );
        }
    }

    public List<String> partialValidate( Object root, Object json, String path, boolean ignoreRequiredDefault ) {
        final SchemaPath.Result traverseResult = SchemaPath.traverse( this.schema, path );
        final SchemaAST partialSchema = traverseResult.schema
            .orElseThrow( () -> new ValidationSyntaxException( "path " + path + " not found" ) );

        JsonValidatorProperties properties = new JsonValidatorProperties(
            schema,
            root,
            Optional.of( path ),
            Optional.empty(),
            traverseResult.additionalProperties,
            ignoreRequiredDefault,
            this::validate
        );

        return validate( properties, partialSchema, json );
    }

    @SuppressWarnings( "unchecked" )
    public List<String> validate( Object json, boolean ignoreRequiredDefault ) {
        JsonValidatorProperties properties = new JsonValidatorProperties(
            schema,
            json,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            ignoreRequiredDefault,
            this::validate
        );
        return validate( properties, schema, json );
    }
}
