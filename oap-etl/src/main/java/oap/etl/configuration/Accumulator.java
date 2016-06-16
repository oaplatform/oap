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

package oap.etl.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.etl.accumulator.AccumulatorType;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@ToString
@EqualsAndHashCode
public class Accumulator {
   public String name;
   public AccumulatorType type;
   public Optional<String> field = Optional.empty();
   public Optional<Filter> filter = Optional.empty();

   public Accumulator( String name,
                       @JsonProperty( "type" ) AccumulatorType type,
                       @JsonProperty( "field" ) Optional<String> field,
                       @JsonProperty( "filter" ) Optional<Filter> filter ) {
      this.name = name;
      this.type = type;
      this.field = field;
      this.filter = filter;
   }

   public static class Filter {
      public String field;
      public String operation;
      public Object value;

      public Filter( String field, String operation, Object value ) {
         this.field = field;
         this.operation = operation;
         this.value = value;
      }

      public Predicate getFunction() {
         switch( operation ) {
            case "=":
            case "==":
               return ( v ) -> Objects.equals( v, value );
            case "!=":
            case "<>":
               return ( v ) -> !Objects.equals( v, value );
            default:
               throw new IllegalArgumentException( "Unknown operation " + operation );
         }
      }
   }
}
