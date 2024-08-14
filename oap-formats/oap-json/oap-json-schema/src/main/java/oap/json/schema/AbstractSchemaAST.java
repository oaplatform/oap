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

import lombok.ToString;
import oap.util.Lists;
import oap.util.Mergeable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractSchemaAST<T extends AbstractSchemaAST<T>> implements Mergeable<T> {
    public final String path;
    public final CommonSchemaAST common;

    public AbstractSchemaAST( CommonSchemaAST common, String path ) {
        this.common = common;
        this.path = path;
    }

    @ToString
    public static class CommonSchemaAST implements Mergeable<CommonSchemaAST> {
        public final String schemaType;
        public final Optional<BooleanReference> required;
        public final Optional<BooleanReference> enabled;
        public final Optional<Object> defaultValue;
        public final Optional<EnumFunction> enumValue;
        public final Optional<String> title;
        public final Optional<String> description;
        public final ArrayList<Object> examples = new ArrayList<>();

        public CommonSchemaAST( String schemaType,
                                Optional<BooleanReference> required,
                                Optional<BooleanReference> enabled,
                                Optional<Object> defaultValue,
                                Optional<EnumFunction> enumValue,
                                Optional<String> title,
                                Optional<String> description,
                                List<Object> examples
        ) {
            this.schemaType = schemaType;
            this.required = required;
            this.enabled = enabled;
            this.defaultValue = defaultValue;
            this.enumValue = enumValue;
            this.title = title;
            this.description = description;
            this.examples.addAll( examples );
        }

        @Override
        public CommonSchemaAST merge( CommonSchemaAST common ) {
            return new CommonSchemaAST(
                schemaType,
                required.isPresent() ? required : common.required,
                enabled.isPresent() ? enabled : common.enabled,
                defaultValue.isPresent() ? defaultValue : common.defaultValue,
                enumValue.isPresent() ? enumValue : common.enumValue,
                title.isPresent() ? title : common.title,
                description.isPresent() ? description : common.description,
                Lists.concat( examples, examples )
            );
        }
    }
}
