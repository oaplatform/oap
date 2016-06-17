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

import oap.util.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class FilteredEnumFunction implements EnumFunction {
   private final Function<Object, List<Object>> source;
   private final Optional<Pair<Function<Object, List<Object>>, OperationFunction>> filtered;
   private final OperationFunction of;

   public FilteredEnumFunction( Function<Object, List<Object>> source,
                                OperationFunction of,
                                Optional<Pair<Function<Object, List<Object>>, OperationFunction>> filtered ) {
      this.source = source;
      this.filtered = filtered;
      this.of = of;
   }

   public FilteredEnumFunction( Function<Object, List<Object>> source, OperationFunction of ) {
      this( source, of, Optional.empty() );
   }

   public FilteredEnumFunction( Function<Object, List<Object>> source, OperationFunction of,
                                Pair<Function<Object, List<Object>>, OperationFunction> filtered ) {
      this( source, of, Optional.of( filtered ) );
   }

   @Override
   public List<Object> apply( Object rootJson, Optional<String> currentPath ) {
      final List<Object> values = source.apply( rootJson )
         .stream()
         .filter( v -> of.apply( rootJson, currentPath, v ) )
         .collect( toList() );

      if( !filtered.isPresent() ) {
         return values;
      } else {
         final Pair<Function<Object, List<Object>>, OperationFunction> functionOperationFunctionPair = filtered.get();
         final Function<Object, List<Object>> filteredSource = functionOperationFunctionPair._1;
         final OperationFunction filteredOf = functionOperationFunctionPair._2;
         final Optional<Object> any = filteredSource.apply( rootJson )
            .stream()
            .filter( v -> filteredOf.apply( rootJson, currentPath, v ) )
            .findAny();
         return any.isPresent() ? values : emptyList();
      }
   }
}
