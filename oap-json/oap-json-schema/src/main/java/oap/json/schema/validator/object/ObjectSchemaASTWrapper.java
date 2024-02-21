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

package oap.json.schema.validator.object;

import oap.json.schema.AbstractSchemaAST;
import oap.json.schema.AbstractSchemaASTWrapper;
import oap.json.schema.ContainerSchemaASTWrapper;
import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.SchemaId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ObjectSchemaASTWrapper extends AbstractSchemaASTWrapper<ObjectSchemaAST> implements ContainerSchemaASTWrapper {

    Optional<ObjectSchemaASTWrapper> extendsSchema;
    LinkedHashMap<String, AbstractSchemaASTWrapper<?>> declaredProperties;
    Optional<Boolean> additionalProperties;
    Optional<String> extendsValue;
    Optional<Boolean> nested;
    Optional<Dynamic> dynamic;

    public ObjectSchemaASTWrapper( SchemaId id ) {
        super( id );
    }

    @Override
    public ObjectSchemaAST unwrap( JsonSchemaParserContext context ) {
        final LinkedHashMap<String, AbstractSchemaAST> p = new LinkedHashMap<>();
        declaredProperties.forEach( ( key, value ) -> p.put( key, context.computeIfAbsent( value.id, () -> value.unwrap( context ) ) ) );

        final ObjectSchemaAST objectSchemaAST = new ObjectSchemaAST( common, additionalProperties, extendsValue, nested, dynamic, p, id.toString() );
        return extendsSchema.map( es -> objectSchemaAST.merge( es.unwrap( context ) ) ).orElse( objectSchemaAST );
    }

    @Override
    public Map<String, List<AbstractSchemaASTWrapper>> getChildren() {
        final LinkedHashMap<String, List<AbstractSchemaASTWrapper>> map = new LinkedHashMap<>();

        extendsSchema.ifPresent( ps ->
            ps.getChildren().forEach( ( key, value ) -> map.computeIfAbsent( key, k -> new ArrayList<>() ).addAll( value ) )
        );

        declaredProperties.forEach( ( key, value ) -> map.computeIfAbsent( key, k -> new ArrayList<>() ).add( value ) );

        return map;
    }
}
