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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

/**
 * Created by Admin on 01.06.2016.
 */
public class AggregatorConfigurationBuilder {
   private final ArrayList<Accumulator> accumulators = new ArrayList<>();
   private final HashMap<String, Join> joins = new HashMap<>();
   private final HashMap<String, List<String>> aggregates = new HashMap<>();
   private String export;
   private String table;

   public static AggregatorConfigurationBuilder custom() {
      return new AggregatorConfigurationBuilder();
   }

   public AggregatorConfigurationBuilder table( String table ) {
      this.table = table;
      return this;
   }

   public AggregatorConfigurationAggregatorBuilder aggregator( String name ) {
      return new AggregatorConfigurationAggregatorBuilder( name );
   }

   public AggregatorConfigurationAccumulatorBuilder accumulator( String name ) {
      return new AggregatorConfigurationAccumulatorBuilder( name, accumulators::add );
   }

   public Aggregator build() {
      return new Aggregator( table, aggregates, accumulators, joins, export );
   }

   public AggregatorConfigurationJoinBuilder join( String name ) {
      return new AggregatorConfigurationJoinBuilder( name );
   }

   public AggregatorConfigurationBuilder export( String export ) {
      this.export = export;
      return this;
   }

   public class AggregatorConfigurationAggregatorBuilder {
      private final String name;

      public AggregatorConfigurationAggregatorBuilder( String name ) {
         this.name = name;
      }

      public AggregatorConfigurationBuilder fields( String... fields ) {
         AggregatorConfigurationBuilder.this.aggregates.put( name, asList( fields ) );

         return AggregatorConfigurationBuilder.this;
      }
   }

   public class AggregatorConfigurationAccumulatorBuilder {
      private final String name;
      private final Consumer<Accumulator> c;
      private AccumulatorType type;
      private Optional<String> field = Optional.empty();
      private Optional<Accumulator.Filter> filter = Optional.empty();
      private Object defaultValue;

      public AggregatorConfigurationAccumulatorBuilder( String name, Consumer<Accumulator> c ) {
         this.name = name;
         this.c = c;
      }

      public AggregatorConfigurationAccumulatorBuilder operation( AccumulatorType type ) {
         this.type = type;
         return this;
      }

      public AggregatorConfigurationAccumulatorBuilder accumulator( String name ) {
         add();

         return new AggregatorConfigurationAccumulatorBuilder( name, c );
      }

      public AggregatorConfigurationBuilder export( String export ) {
         add();

         return AggregatorConfigurationBuilder.this.export( export );
      }

      private void add() {
         assert type != null;
         assert defaultValue != null;

         c.accept( new Accumulator( name, type, field, filter, defaultValue ) );
      }

      public AggregatorConfigurationAccumulatorBuilder field( String field ) {
         this.field = Optional.of( field );

         return this;
      }

      public AggregatorConfigurationAccumulatorBuilder filter( String field, String operation, String value ) {
         this.filter = Optional.of( new Accumulator.Filter( field, operation, value ) );

         return this;
      }

      public AggregatorConfigurationAccumulatorBuilder withDefault( Object value ) {
         this.defaultValue = value;

         return this;
      }

      public Aggregator build() {
         add();

         return AggregatorConfigurationBuilder.this.build();
      }
   }

   public class AggregatorConfigurationJoinBuilder {
      public final String name;
      private final ArrayList<Accumulator> accumulators = new ArrayList<>();
      private String table;
      private String field;

      public AggregatorConfigurationJoinBuilder( String name ) {
         this.name = name;
      }

      public AggregatorConfigurationJoinBuilder table( String table ) {
         this.table = table;

         return this;
      }

      public AggregatorConfigurationJoinBuilder field( String field ) {
         this.field = field;

         return this;
      }

      public AggregatorConfigurationJoinBuilder join( String name ) {
         add();

         return new AggregatorConfigurationJoinBuilder( name );
      }

      private void add() {
         assert table != null;
         assert field != null;

         AggregatorConfigurationBuilder.this.joins.put( name, new Join(table, field, accumulators) );
      }

      public AggregatorConfigurationAccumulatorBuilder accumulator( String name ) {
         add();

         return new AggregatorConfigurationAccumulatorBuilder( name, accumulators::add );
      }
   }
}
