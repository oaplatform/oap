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
package oap.json.schema.validator.object;

import lombok.extern.slf4j.Slf4j;
import oap.json.schema.AbstractJsonSchemaValidator;
import oap.json.schema.AbstractSchemaAST;
import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonValidatorProperties;
import oap.json.schema.SchemaPath;
import oap.util.Stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class ObjectJsonValidator extends AbstractJsonSchemaValidator<ObjectSchemaAST> {
    public ObjectJsonValidator() {
        super( "object" );
    }

    @Override
    public List<String> validate( JsonValidatorProperties properties, ObjectSchemaAST schema, Object value ) {
        if( !( value instanceof Map<?, ?> ) ) return typeFailed( properties, schema, value );

        @SuppressWarnings( "unchecked" ) final Map<String, Object> mapValue = ( Map<String, Object> ) value;

        final List<String> errors = new ArrayList<>();

        final Map<String, AbstractSchemaAST> objectProperties = new HashMap<>();

        schema.properties.forEach( ( k, ast ) -> {
            if( ast.common.enabled.map( e -> {
                JsonValidatorProperties np = properties
                    .withPath( k );
                boolean evaluated = e.apply( properties.rootJson, value, np.path, np.prefixPath );
                log.trace( "evaluated '{}' with value '{}'", np.path, value );
                return evaluated;
            } ).orElse( true ) )
                objectProperties.put( k, ast );
        } );

        objectProperties.forEach( ( k, ast ) -> {
            Object v = mapValue.get( k );
            if( v == null && ast.common.defaultValue.isPresent() )
                mapValue.put( k, ast.common.defaultValue.get() );
            else {
                JsonValidatorProperties validatorProperties = properties
                    .withPath( k )
                    .withAdditionalProperties( schema.additionalProperties );
                errors.addAll( properties.validator.apply( validatorProperties, ast, v ) );
            }
        } );

        List<String> additionalProperties = Stream.of( mapValue.keySet() )
            .filter( v -> !objectProperties.containsKey( v ) )
            .toList();

        schema.ifSchema.ifPresent( ifAst -> {
            boolean matches = properties.validator.apply( properties, ifAst, value ).isEmpty();
            if( matches ) {
                schema.thenSchema.ifPresent( thenAst -> errors.addAll( properties.validator.apply( properties, thenAst, value ) ) );
            } else {
                schema.elseSchema.ifPresent( elseAst -> errors.addAll( properties.validator.apply( properties, elseAst, value ) ) );
            }
        } );

        schema.allOf.forEach( ast -> errors.addAll( properties.validator.apply( properties, ast, value ) ) );

        if( !schema.anyOf.isEmpty()
            && schema.anyOf.stream().noneMatch( ast -> properties.validator.apply( properties, ast, value ).isEmpty() ) ) {
            errors.add( properties.error( "instance does not match any schema in anyOf" ) );
        }

        if( !schema.oneOf.isEmpty() ) {
            long matched = schema.oneOf.stream().filter( ast -> properties.validator.apply( properties, ast, value ).isEmpty() ).count();
            if( matched != 1 ) {
                errors.add( properties.error( "instance must match exactly one schema in oneOf, matched " + matched ) );
            }
        }

        schema.notSchema.ifPresent( notAst -> {
            if( properties.validator.apply( properties, notAst, value ).isEmpty() ) {
                errors.add( properties.error( "instance must not be valid against the schema in not" ) );
            }
        } );

        if( !properties.ignoreRequiredDefault ) {
            for( String name : schema.required ) {
                if( objectProperties.containsKey( name ) && mapValue.get( name ) == null ) {
                    errors.add( properties.withPath( name ).error( "required property is missing" ) );
                }
            }
        }

        if( !properties.forceIgnoreAdditionalProperties
            && !schema.additionalProperties.orElse( properties.additionalProperties.orElse( true ) )
            && !additionalProperties.isEmpty() ) {

            errors.add( properties.error( "additional properties are not permitted " + additionalProperties ) );
        }

        return errors;
    }

    @Override
    public ObjectSchemaASTWrapper parse( JsonSchemaParserContext context ) {
        var wrapper = context.createWrapper( ObjectSchemaASTWrapper::new );

        wrapper.common = node( context ).asCommon();
        wrapper.additionalProperties = node( context ).asBoolean( ADDITIONAL_PROPERTIES ).optional();
        wrapper.extendsValue = node( context ).asString( "extends" ).optional();
        wrapper.nested = node( context ).asBoolean( "nested" ).optional();
        wrapper.dynamic = Optional.ofNullable( context.node.get( "dynamic" ) ).map( v -> Dynamic.valueOf( v.toString().toUpperCase() ) );

        wrapper.extendsSchema = wrapper.extendsValue
            .map( url -> ( ObjectSchemaASTWrapper ) context.urlParser.apply( SchemaPath.resolve( context.rootPath, context.path ), url ) );

        wrapper.declaredProperties = node( context ).asMapAST( "properties", context ).required();

        wrapper.required = node( context ).asList( "required" ).optional()
            .map( list -> list.stream().map( String.class::cast ).toList() )
            .orElse( List.of() );

        wrapper.ifSchema = node( context ).asAST( "if", context ).optional();
        wrapper.thenSchema = node( context ).asAST( "then", context ).optional();
        wrapper.elseSchema = node( context ).asAST( "else", context ).optional();

        wrapper.allOf = node( context ).asListAST( "allOf", context ).optional().orElse( List.of() );
        wrapper.anyOf = node( context ).asListAST( "anyOf", context ).optional().orElse( List.of() );
        wrapper.oneOf = node( context ).asListAST( "oneOf", context ).optional().orElse( List.of() );
        wrapper.notSchema = node( context ).asAST( "not", context ).optional();

        return wrapper;
    }

}
