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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SchemaAST {
    public final CommonSchemaAST common;

    public SchemaAST( CommonSchemaAST common ) {
        this.common = common;
    }

    public static class CommonSchemaAST {
        public final String schemaType;
        public final Optional<Boolean> required;
        public final Optional<Object> defaultValue;
        public final Optional<Function<Object, List<Object>>> enumValue;

        public CommonSchemaAST( String schemaType, Optional<Boolean> required, Optional<Object> defaultValue,
            Optional<Function<Object, List<Object>>> enumValue ) {
            this.schemaType = schemaType;
            this.required = required;
            this.defaultValue = defaultValue;
            this.enumValue = enumValue;
        }
    }
}
