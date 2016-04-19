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

import oap.json.schema._array.ArraySchemaAST;
import oap.json.schema._object.ObjectSchemaAST;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Created by Igor Petrenko on 19.04.2016.
 */
public class SchemaPath {
    private final String[] paths;

    public SchemaPath( String path ) {
        paths = StringUtils.split( path, '.' );
    }

    private static Optional<SchemaAST> traverse( SchemaAST schema, String[] paths, int index ) {
        if( index >= paths.length ) return Optional.of( schema );

        if( schema instanceof ObjectSchemaAST ) {
            final String property = paths[index];
            final SchemaAST s = ( ( ObjectSchemaAST ) schema ).properties.get( property );
            if( s == null ) return Optional.empty();
            return traverse( s, paths, index + 1 );
        } else if( schema instanceof ArraySchemaAST ) {
            return traverse( ( ( ArraySchemaAST ) schema ).items, paths, index );
        } else if( index == paths.length ) {
            return Optional.of( schema );
        } else return Optional.empty();
    }

    public final Optional<SchemaAST> traverse( SchemaAST schema ) {
        return traverse( schema, paths, 0 );
    }
}
