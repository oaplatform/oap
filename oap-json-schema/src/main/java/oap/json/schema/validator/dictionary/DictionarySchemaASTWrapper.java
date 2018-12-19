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

package oap.json.schema.validator.dictionary;

import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.SchemaASTWrapper;
import oap.json.schema.SchemaId;
import oap.json.schema.SchemaWrapperPath;
import oap.json.schema.ValidationSyntaxException;

import java.util.Optional;

/**
 * Created by Igor Petrenko on 12.04.2016.
 */
public class DictionarySchemaASTWrapper extends SchemaASTWrapper<DictionarySchemaAST> {
    Optional<String> name;
    Optional<String> parent;

    public DictionarySchemaASTWrapper( SchemaId id ) {
        super( id );
    }

    @Override
    public DictionarySchemaAST unwrap( JsonSchemaParserContext context ) {
        return new DictionarySchemaAST(
            common, getName( context ), parent.map( p -> {
            final DictionarySchemaASTWrapper parent = getParent( context, p );
            return context.computeIfAbsent( parent.id, () -> parent.unwrap( context ) );
        } ), id.toString()
        );
    }

    private String getName( JsonSchemaParserContext context ) {
        final String name = parent
            .map( p -> getParent( context, p )
                .getName( context )
            )
            .orElseGet( () -> this.name.get() );

        return name;
    }

    private DictionarySchemaASTWrapper getParent( JsonSchemaParserContext context, String parent ) {
        return ( DictionarySchemaASTWrapper ) new SchemaWrapperPath( parent )
            .traverse( context.getRoot() )
            .orElseThrow( () -> new ValidationSyntaxException( "[" + id + "] json-path '" + this.parent.get() + "' not found" ) );
    }
}
