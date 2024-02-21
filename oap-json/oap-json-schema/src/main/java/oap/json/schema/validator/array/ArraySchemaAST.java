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
package oap.json.schema.validator.array;

import oap.json.schema.AbstractSchemaAST;

import java.util.Optional;

public class ArraySchemaAST extends AbstractSchemaAST<ArraySchemaAST> {
    public final Optional<Boolean> additionalProperties;
    public final Optional<Integer> minItems;
    public final Optional<Integer> maxItems;
    public final AbstractSchemaAST items;
    public final Optional<String> idField;

    public ArraySchemaAST( CommonSchemaAST common, Optional<Boolean> additionalProperties,
                           Optional<Integer> minItems, Optional<Integer> maxItems,
                           Optional<String> idField,
                           AbstractSchemaAST items, String path ) {
        super( common, path );
        this.additionalProperties = additionalProperties;
        this.minItems = minItems;
        this.maxItems = maxItems;
        this.idField = idField;
        this.items = items;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public ArraySchemaAST merge( ArraySchemaAST cs ) {
        return new ArraySchemaAST(
            common.merge( cs.common ),
            additionalProperties.isPresent() ? additionalProperties : cs.additionalProperties,
            minItems.isPresent() ? minItems : cs.minItems,
            maxItems.isPresent() ? maxItems : cs.maxItems,
            idField.isPresent() ? idField : cs.idField,
            items.common.schemaType.equals( cs.items.common.schemaType ) ? ( AbstractSchemaAST ) items.merge( cs.items )
                : items,
            path
        );
    }
}
