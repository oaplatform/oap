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
import lombok.val;
import oap.json.schema.DefaultSchemaAST;
import oap.json.schema.SchemaAST;
import oap.json.schema._array.ArraySchemaAST;
import oap.json.schema._dictionary.DictionarySchemaAST;
import oap.json.schema._number.NumberSchemaAST;
import oap.json.schema._object.Dynamic;
import oap.json.schema._object.ObjectSchemaAST;
import oap.json.schema._string.StringSchemaAST;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.IOException;

/**
 * Created by igor.petrenko on 17.10.2017.
 */
public class ElasticSearchSchema {
    @SneakyThrows
    public static String convert( SchemaAST<?> schemaAST ) {
        val jfactory = new JsonFactory();
        val stringBuilder = new StringBuilder();
        try( val stringBuilderWriter = new StringBuilderWriter( stringBuilder );
             val jsonGenerator = jfactory.createGenerator( stringBuilderWriter ) ) {
            jsonGenerator.writeStartObject();
            convert( schemaAST, jsonGenerator, true );
            jsonGenerator.writeEndObject();
        }

        return stringBuilder.toString();
    }

    private static void convert( SchemaAST<?> schemaAST, JsonGenerator jsonGenerator, boolean top ) throws IOException {
        if( schemaAST instanceof ObjectSchemaAST ) {
            val objectSchemaAST = ( ObjectSchemaAST ) schemaAST;

            if( !objectSchemaAST.common.index.orElse( true ) )
                jsonGenerator.writeBooleanField( "enabled", false );

            val nested = objectSchemaAST.nested.orElse( false );
            if( !top || nested )
                jsonGenerator.writeStringField( "type", !nested ? "object" : "nested" );

            if( objectSchemaAST.dynamic.isPresent() ) {
                switch( objectSchemaAST.dynamic.orElse( Dynamic.TRUE ) ) {
                    case TRUE:
                        jsonGenerator.writeStringField( "dynamic", "true" );
                        break;
                    case FALSE:
                        jsonGenerator.writeStringField( "dynamic", "false" );
                        break;
                    case STRICT:
                        jsonGenerator.writeStringField( "dynamic", "strict" );
                        break;
                }
            }

            jsonGenerator.writeObjectFieldStart( "properties" );
            for( val entry : objectSchemaAST.properties.entrySet() ) {
                jsonGenerator.writeObjectFieldStart( entry.getKey() );

                convert( entry.getValue(), jsonGenerator, false );

                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndObject();
        } else if( schemaAST instanceof ArraySchemaAST ) {
            convert( ( ( ArraySchemaAST ) schemaAST ).items, jsonGenerator, false );
        } else {
            val schemaType = schemaAST.common.schemaType;
            if( schemaAST instanceof DefaultSchemaAST ) {
                switch( schemaType ) {
                    case "boolean":
                        jsonGenerator.writeStringField( "type", "boolean" );
                        break;
                    case "date":
                        jsonGenerator.writeStringField( "type", "date" );
                        break;
                    default:
                        throw new IllegalArgumentException( "Unknown schema type " + schemaType );
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
                    case "integer":
                    case "long":
                        jsonGenerator.writeStringField( "type", "long" );
                        break;
                    case "double":
                        jsonGenerator.writeStringField( "type", "double" );
                        break;
                    default:
                        throw new IllegalArgumentException( "Unknown schema type " + schemaType );
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

    private static void convertCommon( SchemaAST<?> schemaAST, JsonGenerator jsonGenerator ) throws IOException {
        if( !schemaAST.common.include_in_all.orElse( true ) )
            jsonGenerator.writeBooleanField( "include_in_all", false );
        if( !schemaAST.common.index.orElse( true ) )
            jsonGenerator.writeBooleanField( "index", false );
    }
}
