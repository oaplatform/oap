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

import oap.json.schema.SchemaAST;

import java.util.LinkedHashMap;
import java.util.Optional;

public class ObjectSchemaAST extends SchemaAST<ObjectSchemaAST> {
   public final Optional<Boolean> additionalProperties;
   public final Optional<String> extendsValue;
   public final LinkedHashMap<String, SchemaAST> properties;

   public ObjectSchemaAST( CommonSchemaAST common, Optional<Boolean> additionalProperties,
                           Optional<String> extendsValue, LinkedHashMap<String, SchemaAST> properties,
                           String path) {
      super( common, path );
      this.additionalProperties = additionalProperties;
      this.extendsValue = extendsValue;
      this.properties = properties;
   }

   @Override
   public ObjectSchemaAST merge( ObjectSchemaAST cs ) {
      return new ObjectSchemaAST(
         common.merge( cs.common ),
         additionalProperties.isPresent() ? additionalProperties : cs.additionalProperties,
         extendsValue.isPresent() ? extendsValue : cs.extendsValue,
         merge( properties, cs.properties ),
         path
      );
   }

   @SuppressWarnings( "unchecked" )
   private LinkedHashMap<String, SchemaAST> merge( LinkedHashMap<String, SchemaAST> parentProperties, LinkedHashMap<String, SchemaAST> current ) {
      final LinkedHashMap<String, SchemaAST> result = new LinkedHashMap<>();

      current.entrySet().stream().filter( e -> !parentProperties.containsKey( e.getKey() ) ).forEach( e -> result.put( e.getKey(), e.getValue() ) );

      parentProperties.forEach( ( k, v ) -> {
         final SchemaAST cs = current.get( k );
         if( cs == null || !v.common.schemaType.equals( cs.common.schemaType ) )
            result.put( k, v );
         else
            result.put( k, v.merge( cs ) );
      } );

      return result;
   }
}
