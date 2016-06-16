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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@ToString
@EqualsAndHashCode
public class Aggregator implements IAggregator {
   public final Map<String, List<String>> aggregates;
   public final String export;
   private final String table;
   private final Map<String, Join> joins;
   private final List<Accumulator> accumulators;


   public Aggregator(
      String table, Map<String, List<String>> aggregates,
      List<Accumulator> accumulators, Map<String, Join> joins, String export ) {
      this.table = table;
      this.accumulators = accumulators;
      this.aggregates = aggregates;
      this.joins = joins;
      this.export = export;
   }

   @Override
   public String getTable() {
      return table;
   }

   @Override
   public Map<String, Join> getJoins() {
      return joins;
   }

   @Override
   public List<Accumulator> getAccumulators() {
      return accumulators;
   }

   @Override
   public Map<String, List<String>> getAggregates() {
      return aggregates;
   }

   public String getExport() {
      return export;
   }
}
