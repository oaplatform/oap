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
import java.util.stream.Stream;

public class ConditionalAST {
    public static final ConditionalAST EMPTY = new ConditionalAST(
        Optional.empty(), Optional.empty(), Optional.empty(),
        List.of(), List.of(), List.of(), Optional.empty() );

    public final Optional<AbstractSchemaAST> ifSchema;
    public final Optional<AbstractSchemaAST> thenSchema;
    public final Optional<AbstractSchemaAST> elseSchema;
    public final List<AbstractSchemaAST> allOf;
    public final List<AbstractSchemaAST> anyOf;
    public final List<AbstractSchemaAST> oneOf;
    public final Optional<AbstractSchemaAST> notSchema;

    public ConditionalAST( Optional<AbstractSchemaAST> ifSchema,
                           Optional<AbstractSchemaAST> thenSchema,
                           Optional<AbstractSchemaAST> elseSchema,
                           List<AbstractSchemaAST> allOf,
                           List<AbstractSchemaAST> anyOf,
                           List<AbstractSchemaAST> oneOf,
                           Optional<AbstractSchemaAST> notSchema ) {
        this.ifSchema = ifSchema;
        this.thenSchema = thenSchema;
        this.elseSchema = elseSchema;
        this.allOf = allOf;
        this.anyOf = anyOf;
        this.oneOf = oneOf;
        this.notSchema = notSchema;
    }

    public boolean isEmpty() {
        return ifSchema.isEmpty() && allOf.isEmpty() && anyOf.isEmpty() && oneOf.isEmpty() && notSchema.isEmpty();
    }

    public ConditionalAST merge( ConditionalAST cs ) {
        return new ConditionalAST(
            ifSchema.isPresent() ? ifSchema : cs.ifSchema,
            thenSchema.isPresent() ? thenSchema : cs.thenSchema,
            elseSchema.isPresent() ? elseSchema : cs.elseSchema,
            Stream.concat( allOf.stream(), cs.allOf.stream() ).distinct().toList(),
            Stream.concat( anyOf.stream(), cs.anyOf.stream() ).distinct().toList(),
            Stream.concat( oneOf.stream(), cs.oneOf.stream() ).distinct().toList(),
            notSchema.isPresent() ? notSchema : cs.notSchema
        );
    }
}
