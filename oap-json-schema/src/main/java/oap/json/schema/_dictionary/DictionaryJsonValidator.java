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

package oap.json.schema._dictionary;

import lombok.extern.slf4j.Slf4j;
import oap.dictionary.Dictionaries;
import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryNotFoundError;
import oap.json.schema.BooleanReference;
import oap.json.schema.JsonPath;
import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.JsonSchemaValidator;
import oap.json.schema.JsonValidatorProperties;
import oap.json.schema.SchemaPath;
import oap.util.Lists;
import oap.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static oap.json.schema.SchemaPath.rightTrimItems;

/**
 * Created by Igor Petrenko on 12.04.2016.
 */
@Slf4j
public class DictionaryJsonValidator extends JsonSchemaValidator<DictionarySchemaAST> {

    public DictionaryJsonValidator() {
        super( "dictionary" );
    }

    private static String printIds( final List<Dictionary> dictionaries ) {
        return dictionaries
            .stream()
            .flatMap( dictionary -> dictionary.ids().stream() )
            .distinct()
            .collect( toList() )
            .toString();
    }

    private Result<List<Dictionary>, List<String>> validate( JsonValidatorProperties properties, Optional<DictionarySchemaAST> schemaOpt, List<Dictionary> dictionaries ) {
        if( !schemaOpt.isPresent() ) return Result.success( dictionaries );

        final DictionarySchemaAST schema = schemaOpt.get();

        final Result<List<Dictionary>, List<String>> cd = validate( properties, schema.parent, dictionaries );

        if( !cd.isSuccess() ) return cd;

        final JsonPath jsonPath = new JsonPath( rightTrimItems( schema.path ), properties.path );
        List<Object> parentValues = jsonPath.traverse( properties.rootJson );

        if( parentValues.isEmpty() ) {
            final String fixedPath = jsonPath.getFixedPath();
            if( !schema.common.required.orElse( BooleanReference.FALSE )
                .apply( properties.rootJson, properties.rootJson, Optional.of( fixedPath ), properties.prefixPath ) )
//            return Result.failure( Lists.of( properties.error( fixedPath, "required property is missing" ) ) );
//         else
                parentValues = cd.successValue
                    .stream()
                    .flatMap( d -> d.getValues().stream().map( Dictionary::getId ) )
                    .collect( toList() );
        }

        final ArrayList<Dictionary> cDict = new ArrayList<>();

        for( Object parentValue : parentValues ) {
            List<Dictionary> children = cd.successValue
                .stream()
                .map( d -> d.getValueOpt( parentValue.toString() ) )
                .filter( Optional::isPresent )
                .map( Optional::get ).collect( toList() );
            if( children.isEmpty() )
                return Result.failure( Lists.of(
                    properties.error( "instance does not match any member resolve the enumeration " + printIds( cd.successValue ) )
                ) );

            cDict.addAll( children );
        }

        return Result.success( cDict );
    }

    @Override
    public List<String> validate( JsonValidatorProperties properties, DictionarySchemaAST schema, Object value ) {
        final List<Dictionary> dictionaries;
        try {
            dictionaries = Lists.of( Dictionaries.getCachedDictionary( schema.name ) );
        } catch( final DictionaryNotFoundError e ) {
            return Lists.of( properties.error( "dictionary " + schema.name + " not found" ) );
        }

        final List<String> errors = new ArrayList<>();

        validate( properties, schema.parent, dictionaries )
            .ifFailure( errors::addAll )
            .ifSuccess( successes -> {
                if( !successes.isEmpty() &&
                    successes.stream().noneMatch( d -> d.containsValueWithId( String.valueOf( value ) ) ) ) {

                    errors.addAll( Lists.of( properties.error( "instance does not match any member resolve " +
                        "the enumeration " + printIds( successes ) ) ) );
                }
            } );

        return errors;
    }

    @Override
    public DictionarySchemaASTWrapper parse( JsonSchemaParserContext context ) {
        DictionarySchemaASTWrapper wrapper = context.createWrapper( DictionarySchemaASTWrapper::new );

        wrapper.common = node( context ).asCommon();
        wrapper.name = node( context ).asString( "name" ).optional();
        wrapper.parent = node( context ).asMap( "parent" ).optional()
            .flatMap( m -> Optional.ofNullable( ( String ) m.get( "json-path" ) ).map( jp -> SchemaPath.resolve( context.rootPath, jp ) ) );

        return wrapper;
    }
}
