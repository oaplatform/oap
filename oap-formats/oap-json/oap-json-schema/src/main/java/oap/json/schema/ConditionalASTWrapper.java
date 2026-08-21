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

public class ConditionalASTWrapper {
    public static final ConditionalASTWrapper EMPTY = new ConditionalASTWrapper(
        Optional.empty(), Optional.empty(), Optional.empty(),
        List.of(), List.of(), List.of(), Optional.empty() );

    public final Optional<AbstractSchemaASTWrapper> ifSchema;
    public final Optional<AbstractSchemaASTWrapper> thenSchema;
    public final Optional<AbstractSchemaASTWrapper> elseSchema;
    public final List<AbstractSchemaASTWrapper> allOf;
    public final List<AbstractSchemaASTWrapper> anyOf;
    public final List<AbstractSchemaASTWrapper> oneOf;
    public final Optional<AbstractSchemaASTWrapper> notSchema;

    public ConditionalASTWrapper( Optional<AbstractSchemaASTWrapper> ifSchema,
                                  Optional<AbstractSchemaASTWrapper> thenSchema,
                                  Optional<AbstractSchemaASTWrapper> elseSchema,
                                  List<AbstractSchemaASTWrapper> allOf,
                                  List<AbstractSchemaASTWrapper> anyOf,
                                  List<AbstractSchemaASTWrapper> oneOf,
                                  Optional<AbstractSchemaASTWrapper> notSchema ) {
        this.ifSchema = ifSchema;
        this.thenSchema = thenSchema;
        this.elseSchema = elseSchema;
        this.allOf = allOf;
        this.anyOf = anyOf;
        this.oneOf = oneOf;
        this.notSchema = notSchema;
    }

    @SuppressWarnings( "unchecked" )
    public ConditionalAST unwrap( JsonSchemaParserContext context ) {
        return new ConditionalAST(
            ifSchema.map( w -> context.computeIfAbsent( w.id, () -> w.unwrap( context ) ) ),
            thenSchema.map( w -> context.computeIfAbsent( w.id, () -> w.unwrap( context ) ) ),
            elseSchema.map( w -> context.computeIfAbsent( w.id, () -> w.unwrap( context ) ) ),
            allOf.stream().map( w -> context.computeIfAbsent( w.id, () -> w.unwrap( context ) ) ).toList(),
            anyOf.stream().map( w -> context.computeIfAbsent( w.id, () -> w.unwrap( context ) ) ).toList(),
            oneOf.stream().map( w -> context.computeIfAbsent( w.id, () -> w.unwrap( context ) ) ).toList(),
            notSchema.map( w -> context.computeIfAbsent( w.id, () -> w.unwrap( context ) ) )
        );
    }
}
