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

package oap.json.schema.elasticsearch;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import lombok.SneakyThrows;
import oap.json.schema.AbstractSchemaAST;
import oap.json.schema.DefaultSchemaAST;
import oap.json.schema.validator.array.ArraySchemaAST;
import oap.json.schema.validator.dictionary.DictionarySchemaAST;
import oap.json.schema.validator.number.NumberSchemaAST;
import oap.json.schema.validator.object.Dynamic;
import oap.json.schema.validator.object.ObjectSchemaAST;
import oap.json.schema.validator.string.StringSchemaAST;
import oap.util.Try;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.IOException;

public class ElasticSearchSchema {
    @SneakyThrows
    public static String convert( AbstractSchemaAST<?> schemaAST ) {
        var jfactory = new JsonFactory();
        var stringBuilder = new StringBuilder();
        try( var stringBuilderWriter = new StringBuilderWriter( stringBuilder );
             var jsonGenerator = jfactory.createGenerator( stringBuilderWriter ) ) {
            jsonGenerator.writeStartObject();
            convert( schemaAST, jsonGenerator, true );
            jsonGenerator.writeEndObject();
        }

        return stringBuilder.toString();
    }

    private static void convert( AbstractSchemaAST<?> schemaAST, JsonGenerator jsonGenerator, boolean top ) throws IOException {
        if( schemaAST instanceof ObjectSchemaAST ) {
            var objectSchemaAST = ( ObjectSchemaAST ) schemaAST;

            if( !objectSchemaAST.common.index.orElse( true ) )
                jsonGenerator.writeBooleanField( "enabled", false );

            var nested = objectSchemaAST.nested.orElse( false );
            if( !top || nested )
                jsonGenerator.writeStringField( "type", !nested ? "object" : "nested" );

            if( objectSchemaAST.dynamic.isPresent() ) {
                switch( objectSchemaAST.dynamic.orElse( Dynamic.TRUE ) ) {
                    case TRUE -> jsonGenerator.writeStringField( "dynamic", "true" );
                    case FALSE -> jsonGenerator.writeStringField( "dynamic", "false" );
                    default -> jsonGenerator.writeStringField( "dynamic", "strict" );
                }
            }

            jsonGenerator.writeObjectFieldStart( "properties" );
            for( var entry : objectSchemaAST.properties.entrySet() ) {
                if( entry.getKey().equals( "_id" ) ) continue;
                jsonGenerator.writeObjectFieldStart( entry.getKey() );

                convert( entry.getValue(), jsonGenerator, false );

                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndObject();
        } else if( schemaAST instanceof ArraySchemaAST ) {
            convert( ( ( ArraySchemaAST ) schemaAST ).items, jsonGenerator, false );
        } else {
            var schemaType = schemaAST.common.schemaType;
            if( schemaAST instanceof DefaultSchemaAST ) {
                switch( schemaType ) {
                    case "boolean" -> jsonGenerator.writeStringField( "type", "boolean" );
                    case "date" -> jsonGenerator.writeStringField( "type", "date" );
                    default -> throw new IllegalArgumentException( "Unknown schema type " + schemaType );
                }

                convertCommon( schemaAST, jsonGenerator );
            } else if( schemaAST instanceof StringSchemaAST ) {
                if( schemaType.equals( "string" ) ) {
                    jsonGenerator.writeStringField( "type", "keyword" );
                } else {
                    jsonGenerator.writeStringField( "type", "text" );
                }

                convertCommon( schemaAST, jsonGenerator );
            } else if( schemaAST instanceof NumberSchemaAST ) {
                switch( schemaType ) {
                    case "integer", "long" -> jsonGenerator.writeStringField( "type", "long" );
                    case "double" -> jsonGenerator.writeStringField( "type", "double" );
                    default -> throw new IllegalArgumentException( "Unknown schema type " + schemaType );
                }

                convertCommon( schemaAST, jsonGenerator );
            } else if( schemaAST instanceof DictionarySchemaAST ) {
                jsonGenerator.writeStringField( "type", "keyword" );

                convertCommon( schemaAST, jsonGenerator );
            } else {
                throw new IllegalArgumentException( "Unknown schema type " + schemaType );
            }
        }
    }

    private static void convertCommon( AbstractSchemaAST<?> schemaAST, JsonGenerator jsonGenerator ) throws IOException {
        if( !schemaAST.common.includeInAll.orElse( true ) )
            jsonGenerator.writeBooleanField( "include_in_all", false );

        if( !schemaAST.common.index.orElse( true ) )
            jsonGenerator.writeBooleanField( "index", false );

        if( schemaAST.common.norms.orElse( false ) )
            jsonGenerator.writeBooleanField( "norms", true );

        schemaAST.common.analyzer.ifPresent( Try.consume( a -> jsonGenerator.writeStringField( "analyzer", a ) ) );
    }
}
