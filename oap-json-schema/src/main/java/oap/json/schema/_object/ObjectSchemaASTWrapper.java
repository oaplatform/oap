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

package oap.json.schema._object;

import oap.json.schema.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ObjectSchemaASTWrapper
   extends SchemaASTWrapper<ObjectSchemaAST, ObjectSchemaASTWrapper>
   implements ContainerSchemaASTWrapper {

   Optional<ObjectSchemaASTWrapper> parentSchema;
   LinkedHashMap<String, SchemaASTWrapper> declaredProperties;
   Optional<Boolean> additionalProperties;
   Optional<String> extendsValue;

   public ObjectSchemaASTWrapper( SchemaId id ) {
      super( id );
   }

   @Override
   public ObjectSchemaAST unwrap( JsonSchemaParserContext context ) {
      final LinkedHashMap<String, SchemaAST> p = new LinkedHashMap<>();
      declaredProperties.forEach( ( key, value ) -> p.put( key, value.unwrap( context ) ) );
      final ObjectSchemaAST objectSchemaAST = new ObjectSchemaAST( common, additionalProperties, extendsValue, p, id.toString() );
      return parentSchema.map( ps -> objectSchemaAST.merge( ps.unwrap( context ) ) ).orElse( objectSchemaAST );
   }

   @Override
   public Map<String, SchemaASTWrapper> getChildren() {
      if( parentSchema.isPresent() ) {
         final LinkedHashMap<String, SchemaASTWrapper> map = new LinkedHashMap<>();
         parentSchema.get().getChildren().forEach( map::put );
         declaredProperties.forEach( map::put );
         return map;
      } else return declaredProperties;
   }
}
