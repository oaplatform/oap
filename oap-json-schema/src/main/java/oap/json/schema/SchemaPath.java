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

import lombok.AllArgsConstructor;
import lombok.val;
import oap.json.schema._array.ArraySchemaAST;
import oap.json.schema._object.ObjectSchemaAST;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Optional;

/**
 * Created by Admin on 24.05.2016.
 */
public final class SchemaPath {
    public static String resolve( String path1, String... paths ) {
        if( paths.length == 0 ) return path1;

        String res = path1;

        for( String p : paths ) {
            if( res.isEmpty() && p.isEmpty() ) continue;
            if( p.isEmpty() ) continue;

            if( res.isEmpty() ) res = p;
            else res += "." + p;
        }

        return res;
    }

    public static String rightTrimItems( String path ) {
        return path.endsWith( ".items" ) ? path.substring( 0, path.length() - ".items".length() ) : path;
    }

    public static Result traverse( SchemaAST root, String path ) {
        SchemaAST schemaAST = root;
        val additionalProperties = new MutableObject<Boolean>( null );

        for( val item : StringUtils.split( path, '.' ) ) {
            schemaAST = skipArray( schemaAST, additionalProperties );

            if( schemaAST instanceof ObjectSchemaAST ) {
                val objectSchemaAST = ( ObjectSchemaAST ) schemaAST;
                schemaAST = objectSchemaAST.properties.get( item );
                objectSchemaAST.additionalProperties.ifPresent( additionalProperties::setValue );
                if( schemaAST == null )
                    return new Result( Optional.empty(), Optional.ofNullable( additionalProperties.getValue() ) );
            } else {
                return new Result( Optional.empty(), Optional.ofNullable( additionalProperties.getValue() ) );
            }
        }

        schemaAST = skipArray( schemaAST, additionalProperties );

        return new Result( Optional.of( schemaAST ), Optional.ofNullable( additionalProperties.getValue() ) );
    }

    private static SchemaAST skipArray( SchemaAST schemaAST, MutableObject<Boolean> additionalProperties ) {
        while( schemaAST instanceof ArraySchemaAST ) {
            val arraySchemaAST = ( ArraySchemaAST ) schemaAST;
            schemaAST = arraySchemaAST.items;
            arraySchemaAST.additionalProperties.ifPresent( additionalProperties::setValue );
        }
        return schemaAST;
    }

    @AllArgsConstructor
    public static class Result {
        public final Optional<SchemaAST> schema;
        public final Optional<Boolean> additionalProperties;
    }
}
