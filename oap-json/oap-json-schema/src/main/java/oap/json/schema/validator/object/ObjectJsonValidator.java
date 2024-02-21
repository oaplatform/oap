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
                var np = properties
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
                var validatorProperties = properties
                        .withPath( k )
                        .withAdditionalProperties( schema.additionalProperties );
                errors.addAll( properties.validator.apply( validatorProperties, ast, v ) );
            }
        } );

        List<String> additionalProperties = Stream.of( mapValue.keySet() )
            .filter( v -> !objectProperties.containsKey( v ) )
            .toList();

        if( !schema.additionalProperties.orElse( properties.additionalProperties.orElse( true ) )
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

        return wrapper;
    }

}
