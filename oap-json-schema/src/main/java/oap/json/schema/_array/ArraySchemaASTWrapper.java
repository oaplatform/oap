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

package oap.json.schema._array;

import oap.json.schema.ContainerSchemaASTWrapper;
import oap.json.schema.JsonSchemaParserContext;
import oap.json.schema.SchemaASTWrapper;
import oap.json.schema.SchemaId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class ArraySchemaASTWrapper extends SchemaASTWrapper<ArraySchemaAST> implements ContainerSchemaASTWrapper {

   public SchemaASTWrapper items;
   Optional<Integer> minItems;
   Optional<Integer> maxItems;
   Optional<String> idField;

   public ArraySchemaASTWrapper( SchemaId id ) {
      super( id );
   }

   @Override
   public ArraySchemaAST unwrap( JsonSchemaParserContext context ) {
      return new ArraySchemaAST( common, minItems, maxItems, idField, context.computeIfAbsent( items.id, () -> items.unwrap( context ) ), id.toString() );
   }

   @Override
   public Map<String, List<SchemaASTWrapper>> getChildren() {
      return new HashMap<String, List<SchemaASTWrapper>>() {{
         put( "items", singletonList( items ) );
      }};
   }
}
