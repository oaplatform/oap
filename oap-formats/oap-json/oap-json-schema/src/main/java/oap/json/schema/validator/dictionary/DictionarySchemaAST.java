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

import oap.json.schema.AbstractSchemaAST;

import java.util.Optional;

public class DictionarySchemaAST extends AbstractSchemaAST<DictionarySchemaAST> {
    public final String name;
    public final Optional<DictionarySchemaAST> parent;

    public DictionarySchemaAST( CommonSchemaAST common, String name, Optional<DictionarySchemaAST> parent, String path ) {
        super( common, path );
        this.name = name;
        this.parent = parent;
    }

    @Override
    public DictionarySchemaAST merge( DictionarySchemaAST cs ) {
        return new DictionarySchemaAST( common.merge( cs.common ), name, parent, path );
    }
}
