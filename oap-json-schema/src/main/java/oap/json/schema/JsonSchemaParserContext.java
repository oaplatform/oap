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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JsonSchemaParserContext {
   public static final SchemaId ROOT_ID = new SchemaId( "" );
   public final Map<?, ?> node;
   public final String schemaType;
   public final Function<JsonSchemaParserContext, SchemaASTWrapper> mapParser;
   public final Function<String, SchemaASTWrapper> urlParser;
   public final String path;
   public final HashMap<SchemaId, SchemaASTWrapper> ast;

   public JsonSchemaParserContext(
      Map<?, ?> node,
      String schemaType,
      Function<JsonSchemaParserContext, SchemaASTWrapper> mapParser,
      Function<String, SchemaASTWrapper> urlParser, String path, HashMap<SchemaId, SchemaASTWrapper> ast ) {
      this.node = node;
      this.schemaType = schemaType;
      this.mapParser = mapParser;
      this.urlParser = urlParser;
      this.path = path;
      this.ast = ast;
   }

   public final JsonSchemaParserContext withNode( String field, Object mapObject ) {
      if( !( mapObject instanceof Map<?, ?> ) )
         throw new IllegalArgumentException( "object expected, but " + mapObject );
      Map<?, ?> map = ( Map<?, ?> ) mapObject;

      Object schemaType = map.get( "type" );

      if( schemaType instanceof String ) {
         return new JsonSchemaParserContext( map, ( String ) schemaType, mapParser, urlParser,
            path.length() > 0 ? path + "." + field : field, ast );
      } else {
         throw new UnknownTypeValidationSyntaxException(
            "Unknown type" + ( schemaType == null ? "nothing" : schemaType.getClass() )
         );
      }
   }

   public <T extends SchemaASTWrapper> T createWrapper( Function<SchemaId, T> creator ) {
      final T w = creator.apply( getId() );
      ast.put( w.id, w );
      return w;
   }

   public SchemaId getId() {
      return new SchemaId( path );
   }

   public SchemaASTWrapper getRoot() {
      final SchemaASTWrapper root = ast.get( ROOT_ID );
      if( root == null )
         throw new IllegalStateException( "root not found" );
      return root;
   }
}
