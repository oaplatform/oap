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

import oap.etl.accumulator.AccumulatorType;
import oap.util.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Created by Admin on 01.06.2016.
 */
public class AggregatorConfigurationBuilder {
   private final HashMap<String, Aggregator> joins = new HashMap<>();
   private final ArrayList<Aggregator.Aggregate> aggregates = new ArrayList<>();
   private String table;

   public static AggregatorConfigurationBuilder custom() {
      return new AggregatorConfigurationBuilder();
   }

   public AggregatorConfigurationBuilder from( String table ) {
      this.table = table;
      return this;
   }

   public AggregatorConfigurationBuilderSelect select( String name, AccumulatorType type, String columnName ) {
      return new AggregatorConfigurationBuilderSelect().select( name, type, columnName );
   }

   public AggregatorConfigurationBuilderSelect select( String name, AccumulatorType type ) {
      return new AggregatorConfigurationBuilderSelect().select( name, type );
   }

   public Aggregator build() {
      return new Aggregator( table, aggregates, joins );
   }

   public AggregatorConfigurationBuilder join( String name, Aggregator aggregator ) {
      this.joins.put( name, aggregator );

      return this;
   }

   public class AggregatorConfigurationBuilderSelect {
      private final ArrayList<Accumulator> accumulators = new ArrayList<>();
      private List<String> groupBy;
      private String export;

      public AggregatorConfigurationBuilderSelect() {
      }

      public AggregatorConfigurationBuilderSelect withFilter( String name, String operation, Object value ) {
         Lists.last( accumulators ).filter = Optional.of( new Accumulator.Filter( name, operation, value ) );
         return this;
      }

      public Aggregator build() {
         sync();

         return AggregatorConfigurationBuilder.this.build();
      }

      public AggregatorConfigurationBuilderSelect select( String name, AccumulatorType type, String columnName ) {
         this.accumulators.add( new Accumulator( name, type, Optional.of( columnName ), Optional.empty() ) );

         return this;
      }

      private void sync() {
         assert groupBy != null;
         AggregatorConfigurationBuilder.this.aggregates.add( new Aggregator.Aggregate( accumulators, groupBy, export ) );
      }

      public AggregatorConfigurationBuilderSelect groupBy( String... columns ) {
         groupBy = Arrays.asList( columns );
         return this;
      }

      public AggregatorConfigurationBuilderSelect export( String export ) {
         this.export = export;
         return this;
      }

      public AggregatorConfigurationBuilderSelect select( String name, AccumulatorType type ) {
         this.accumulators.add( new Accumulator( name, type, Optional.empty(), Optional.empty() ) );
         return this;
      }
   }
}
