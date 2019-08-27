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

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import oap.json.schema.validator.AnyJsonValidator;
import oap.json.schema.validator.BooleanJsonValidator;
import oap.json.schema.validator.DateJsonValidator;
import oap.json.schema.validator.array.ArrayJsonValidator;
import oap.json.schema.validator.dictionary.DictionaryJsonValidator;
import oap.json.schema.validator.number.DoubleJsonValidator;
import oap.json.schema.validator.number.IntegerJsonValidator;
import oap.json.schema.validator.number.LongJsonValidator;
import oap.json.schema.validator.object.ObjectJsonValidator;
import oap.json.schema.validator.string.StringJsonValidator;
import oap.util.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class JsonSchema {
    private static final Map<String, JsonSchemaValidator<?>> validators = new HashMap<>();
    private static Map<String, JsonSchema> schemas = new ConcurrentHashMap<>();

    static {
        JsonSchema.add( new BooleanJsonValidator() );
        JsonSchema.add( new ArrayJsonValidator() );
        JsonSchema.add( new DateJsonValidator() );
        JsonSchema.add( new DoubleJsonValidator() );
        JsonSchema.add( new IntegerJsonValidator() );
        JsonSchema.add( new LongJsonValidator() );
        JsonSchema.add( new DoubleJsonValidator() );
        JsonSchema.add( new StringJsonValidator( "text" ) );
        JsonSchema.add( new StringJsonValidator( "string" ) );
        JsonSchema.add( new ObjectJsonValidator() );
        JsonSchema.add( new DictionaryJsonValidator() );
        JsonSchema.add( new AnyJsonValidator() );
    }

    public final SchemaAST schema;


    JsonSchema( String schemaJson ) {
        this( schemaJson, ResourceSchemaStorage.INSTANCE );
    }

    JsonSchema( String schemaJson, SchemaStorage storage ) {
        final JsonSchemaParserContext context = new JsonSchemaParserContext(
            "", null, "",
            this::parse,
            ( rp, schemaPath ) -> parse( schemaPath, storage.get( schemaPath ), rp, storage ),
            "", "", new HashMap<>(), new HashMap<>(), storage );

        this.schema = parse( schemaJson, context ).unwrap( context );
    }

    public static JsonSchema schema( String schemaPath ) {
        return schemas.computeIfAbsent( schemaPath, u -> schemaFromString( ResourceSchemaStorage.INSTANCE.get( u ) ) );
    }

    public static JsonSchema schemaFromString( String schemaJson, SchemaStorage storage ) {
        return new JsonSchema( schemaJson, storage );
    }

    public static JsonSchema schemaFromString( String schemaJson ) {
        return new JsonSchema( schemaJson );
    }

    public static void add( JsonSchemaValidator<?> validator ) {
        validators.put( validator.type, validator );
    }

    private SchemaASTWrapper parse( String schema, JsonSchemaParserContext context ) {
        return parse( context.withNode( "", parseWithTemplate( schema, context.storage ) ) );
    }

    private Object parseWithTemplate( String schema, SchemaStorage storage ) {
        var obj = Binder.hoconWithoutSystemProperties.unmarshal( Object.class, schema );
        resolveTemplates( obj, storage );
        log.trace( "schema = {}", Binder.json.marshal( obj ) );
        return obj;
    }

    @SuppressWarnings( "unchecked" )
    private void resolveTemplates( Object obj, SchemaStorage storage ) {
        if( obj instanceof Map<?, ?> ) {
            var map = ( Map<Object, Object> ) obj;
            var templatePath = map.get( "template" );
            if( templatePath != null ) {
                Preconditions.checkArgument( templatePath instanceof String );

                var templateStr = storage.get( ( String ) templatePath );
                var templateMap = Binder.hoconWithoutSystemProperties.unmarshal( Object.class, templateStr );
                log.trace( "template path = {}, template = {}", templatePath, templateStr );

                map.remove( "template" );
                addTemplate( map, templateMap );
            }

            map.values().forEach( obj1 -> resolveTemplates( obj1, storage ) );
        } else if( obj instanceof List<?> ) {
            var list = ( List<?> ) obj;
            list.forEach( obj1 -> resolveTemplates( obj1, storage ) );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void addTemplate( Object sl, Object sr ) {
        if( sl instanceof Map<?, ?> ) {
            Preconditions.checkArgument( sr instanceof Map<?, ?> );

            var mapl = ( Map<Object, Object> ) sl;
            var mapr = ( Map<Object, Object> ) sr;
            mapr.forEach( ( key, vr ) -> {
                var vl = mapl.get( key );
                if( vl != null ) {
                    addTemplate( vl, vr );
                } else
                    mapl.put( key, vr );
            } );
        }
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
        return parse( "", schema, "", storage );
    }

    SchemaASTWrapper parse( String schemaName, String schema, String rootPath, SchemaStorage storage ) {
        var context = new JsonSchemaParserContext(
            schemaName,
            null, "",
            this::parse,
            ( rp, schemaPath ) -> parse( schemaPath, storage.get( schemaPath ), rp, storage ),
            rootPath, "", new HashMap<>(), new HashMap<>(), storage );
        return parse( schema, context );
    }

    private SchemaASTWrapper parse( JsonSchemaParserContext context ) {
        var schemaParser = validators.get( context.schemaType );
        if( schemaParser != null ) {
            return schemaParser.parse( context );
        } else {
            log.trace( "registered parsers: {}", validators.keySet() );
            throw new ValidationSyntaxException(
                "[schema:type]: unknown simple type [" + context.schemaType + "]" );
        }
    }

    public List<String> partialValidate( Object root, Object json, String path, boolean ignoreRequiredDefault ) {
        var traverseResult = SchemaPath.traverse( this.schema, path );
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
