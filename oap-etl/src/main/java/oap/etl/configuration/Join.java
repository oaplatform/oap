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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@ToString
@EqualsAndHashCode
public class Join implements IAggregator {
   public final String field;
   private final List<Accumulator> accumulators;
   private final String table;
   private final List<Object> defaultLine;

   public Join(
      String table,
      String field,
      List<Accumulator> accumulators ) {

      this.table = table;
      this.field = field;
      this.accumulators = accumulators;
      this.defaultLine = accumulators.stream().map(a -> a.defaultValue).collect( toList() );
   }

   @Override
   public String getTable() {
      return table;
   }

   @Override
   public Map<String, ? extends IAggregator> getJoins() {
      return emptyMap();
   }

   @Override
   public List<Accumulator> getAccumulators() {
      return accumulators;
   }

   @Override
   @JsonIgnore
   public Map<String, List<String>> getAggregates() {
      throw new IllegalAccessError();
   }

   @Override
   @JsonIgnore
   public String getExport() {
      throw new IllegalAccessError();
   }

   @Override
   public List<Object> getDefaultLine() {
      return defaultLine;
   }
}
