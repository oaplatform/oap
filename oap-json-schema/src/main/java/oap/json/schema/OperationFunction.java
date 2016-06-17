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

import oap.util.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static oap.json.schema.OperationFunction.Condition.ANY;
import static oap.json.schema.OperationFunction.Condition.EQ;
import static oap.json.schema.OperationFunction.Condition.NE;

public class OperationFunction {
   public static final String EQ_OP = "eq";
   public static final String NE_OP = "ne";

   private final Condition condition;
   private final BiFunction<Object, Optional<String>, List<Object>> func;

   public OperationFunction( Condition condition, BiFunction<Object, Optional<String>, List<Object>> func ) {

      this.condition = condition;
      this.func = func;
   }

   public static OperationFunction parse( Map<?, ?> map ) {
      final Object eq = map.get( EQ_OP );
      final Object ne = map.get( NE_OP );

      final OperationFunction.Condition condition = map.containsKey( EQ_OP ) ?
         EQ :
         ( map.containsKey( NE_OP ) ? NE : ANY );

      final Object value = condition == EQ ? eq : ne;

      if( value instanceof Map ) {
         final Map valueMap = ( Map ) value;
         final String jsonPath = ( String ) valueMap.get( "json-path" );

         return new OperationFunction( condition, ( rootJson, currentPath ) -> new JsonPath( jsonPath, currentPath ).traverse( rootJson ) );
      } else {
         return new OperationFunction( condition, ( rootJson, currentPath ) -> Collections.singletonList( value ) );
      }
   }

   private Optional<Object> getValue( Object rootJson, Optional<String> currentPath ) {
      return Lists.headOpt( func.apply( rootJson, currentPath ) );
   }

   public boolean apply( Object rootJson, Optional<String> currentPath, Object value ) {
      return condition == ANY || ( condition == Condition.EQ ) == getValue( rootJson, currentPath ).map( v -> Objects.equals( v, value ) ).orElse( false );
   }

   public enum Condition {
      EQ, NE, ANY
   }
}
