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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ObjectSchemaAST extends AbstractSchemaAST<ObjectSchemaAST> {
    public final Optional<Boolean> additionalProperties;
    public final Optional<String> extendsValue;
    public final Optional<Boolean> nested;
    public final Optional<Dynamic> dynamic;
    public final LinkedHashMap<String, AbstractSchemaAST> properties;
    public final List<String> required;
    public final Optional<AbstractSchemaAST> ifSchema;
    public final Optional<AbstractSchemaAST> thenSchema;
    public final Optional<AbstractSchemaAST> elseSchema;

    public ObjectSchemaAST( CommonSchemaAST common, Optional<Boolean> additionalProperties,
                            Optional<String> extendsValue, Optional<Boolean> nested, Optional<Dynamic> dynamic,
                            LinkedHashMap<String, AbstractSchemaAST> properties,
                            List<String> required,
                            Optional<AbstractSchemaAST> ifSchema,
                            Optional<AbstractSchemaAST> thenSchema,
                            Optional<AbstractSchemaAST> elseSchema,
                            String path ) {
        super( common, path );
        this.additionalProperties = additionalProperties;
        this.extendsValue = extendsValue;
        this.nested = nested;
        this.dynamic = dynamic;
        this.properties = properties;
        this.required = required;
        this.ifSchema = ifSchema;
        this.thenSchema = thenSchema;
        this.elseSchema = elseSchema;
    }

    @Override
    public ObjectSchemaAST merge( ObjectSchemaAST cs ) {
        return new ObjectSchemaAST(
            common.merge( cs.common ),
            additionalProperties.isPresent() ? additionalProperties : cs.additionalProperties,
            extendsValue.isPresent() ? extendsValue : cs.extendsValue,
            nested.isPresent() ? nested : cs.nested,
            dynamic.isPresent() ? dynamic : cs.dynamic,
            merge( properties, cs.properties ),
            Stream.concat( required.stream(), cs.required.stream() ).distinct().toList(),
            ifSchema.isPresent() ? ifSchema : cs.ifSchema,
            thenSchema.isPresent() ? thenSchema : cs.thenSchema,
            elseSchema.isPresent() ? elseSchema : cs.elseSchema,
            path
        );
    }

    @SuppressWarnings( "unchecked" )
    private LinkedHashMap<String, AbstractSchemaAST> merge( LinkedHashMap<String, AbstractSchemaAST> parentProperties, LinkedHashMap<String, AbstractSchemaAST> current ) {
        final LinkedHashMap<String, AbstractSchemaAST> result = new LinkedHashMap<>();

        current.entrySet().stream().filter( e -> !parentProperties.containsKey( e.getKey() ) ).forEach( e -> result.put( e.getKey(), e.getValue() ) );

        parentProperties.forEach( ( k, v ) -> {
            var cs = current.get( k );
            if( cs == null || !v.common.schemaType.equals( cs.common.schemaType ) )
                result.put( k, v );
            else {
                result.put( k, ( AbstractSchemaAST ) v.merge( cs ) );
            }
        } );

        return result;
    }
}
