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

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Created by Igor Petrenko on 19.04.2016.
 */
public class SchemaWrapperPath {
   private final String[] paths;

   public SchemaWrapperPath( String path ) {
      paths = StringUtils.split( path, '.' );
   }

   private static Optional<SchemaASTWrapper> traverse( SchemaASTWrapper schema, String[] paths, int index ) {
      if( index >= paths.length ) return Optional.of( schema );

      if( schema instanceof ContainerSchemaASTWrapper ) {
         final String property = paths[index];
         final List<SchemaASTWrapper> list = ( ( ContainerSchemaASTWrapper ) schema ).getChildren().get( property );
         if( list == null ) return Optional.empty();
         return list.stream().map( s -> traverse( s, paths, index + 1 ) ).filter( Optional::isPresent ).findFirst().map( Optional::get );
      } else return Optional.empty();
   }

   public final Optional<SchemaASTWrapper> traverse( SchemaASTWrapper schema ) {
      return traverse( schema, paths, 0 );
   }
}
