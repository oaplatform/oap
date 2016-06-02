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
package oap.etl;

import oap.etl.accumulator.Accumulator;
import oap.tsv.Model;
import oap.tsv.Tsv;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

public class Table {
   private Stream<List<Object>> lines;
   private List<Runnable> closeHandlers = new ArrayList<>();

   private Table( Stream<List<Object>> lines ) {
      this.lines = lines;
   }

   public static Optional<Table> fromResource( Class<?> contextClass, String name, Model model ) {
      return Tsv.fromResource( contextClass, name, model ).map( Table::new );
   }

   public static Table fromString( String tsv, Model model ) {
      return new Table( Tsv.fromString( tsv, model ) );
   }

   public static Table fromPath( Path path, Model model ) {
      return new Table( Tsv.fromPath( path, model ) );
   }

   public static Table fromPaths( List<Path> paths, Model.Complex complexModel ) {
      return new Table( Tsv.fromPaths( paths, complexModel ) );
   }

   public static Table fromPaths( List<Path> paths, Model model ) {
      return new Table( Tsv.fromPaths( paths, model ) );
   }

   @SuppressWarnings( "unchecked" )
   public Table sort( int[] fields ) {
      this.lines = lines.sorted( ( l1, l2 ) -> {
         for( int field : fields ) {
            int result = ( ( Comparable ) l1.get( field ) ).compareTo( l2.get( field ) );
            if( result != 0 ) return result;
         }
         return 0;
      } );
      return this;
   }

   public Table export( Export export ) {
      closeHandlers.add( export::close );
      return transform( export::line );
   }

   public Table progress( long step, LongConsumer report ) {
      AtomicLong total = new AtomicLong( 0 );
      closeHandlers.add( () -> report.accept( total.get() ) );
      return transform( l -> {
         if( total.incrementAndGet() % step == 0 ) report.accept( total.get() );
      } );
   }

   public Table transform( Consumer<List<Object>> consumer ) {
      this.lines = this.lines.map( l -> {
         consumer.accept( l );
         return l;
      } );
      return this;
   }

   public GroupByStream groupBy( GroupBy... groups ) {
      final HashMap<HashCodeCache, Data>[] agg = new HashMap[groups.length];
      final Object[][] keys = new Object[groups.length][];
      final HashCodeCache[] hashCodeCache = new HashCodeCache[groups.length];

      for( int i = 0; i < groups.length; i++ ) {
         final GroupBy gb = groups[i];

         agg[i] = new HashMap<>();
         keys[i] = new Object[gb.fields.length];
         hashCodeCache[i] = new HashCodeCache();
      }

      final Iterator<List<Object>> iterator = lines.iterator();
      while( iterator.hasNext() ) {
         final List<Object> row = iterator.next();

         for( int i = 0; i < groups.length; i++ ) {
            final GroupBy gb = groups[i];
            final int[] fields = gb.fields;
            final Accumulator[] accumulators = gb.accumulators;

            final HashCodeCache gHashCodeCache = hashCodeCache[i];
            final Object[] gkeys = keys[i];
            fillKey( fields, row, gkeys );
            gHashCodeCache.reset( gkeys );

            final HashMap<HashCodeCache, Data> map = agg[i];
            Data d = map.get( gHashCodeCache );
            if( d == null ) {
               d = new Data( gkeys, accumulators );
               map.put( gHashCodeCache, d );
               hashCodeCache[i] = new HashCodeCache();
               keys[i] = new Object[fields.length];
            }

            for( Accumulator accumulator : d.accumulators )
               accumulator.accumulate( row );
         }

      }

      return new GroupByStream( agg, Stream.of( groups ).map( g -> g.fields ).toArray( int[][]::new ) );
   }

   private void fillKey( int[] fields, List<Object> row, Object[] key ) {
      for( int i = 0; i < fields.length; i++ ) {
         key[i] = row.get( fields[i] );
      }
   }

   public Table join( int keyPos, List<Join> joins ) {
      return transform( line -> {
         for( Join join : joins ) line.addAll( join.on( ( String ) line.get( keyPos ) ) );
      } );
   }

   public Table join( int keyPos, Join... joins ) {
      return join( keyPos, Arrays.asList( joins ) );
   }

   public void compute() {
      lines.drain();
      closeHandlers.forEach( Runnable::run );
   }

   public static class GroupBy {
      public final int[] fields;
      public final Accumulator[] accumulators;

      public GroupBy( int[] fields, Accumulator... accumulators ) {
         this.fields = fields;
         this.accumulators = accumulators;
      }
   }


   private static class HashCodeCache {
      public Object[] values;
      private int hashCode;

      public void reset( Object[] values ) {
         this.values = values;

         int result = 1;

         for( Object element : values )
            result = 31 * result + element.hashCode();

         hashCode = result;
      }

      @Override
      public boolean equals( Object obj ) {
         final HashCodeCache obj1 = ( HashCodeCache ) obj;
         final Object[] values = obj1.values;

         for( int i = 0; i < values.length; i++ ) {
            if( !values[i].equals( this.values[i] ) ) return false;
         }

         return true;
      }

      @Override
      public int hashCode() {
         return hashCode;
      }
   }

   private static class Data {
      final Object[] keys;
      final Accumulator[] accumulators;

      public Data( Object[] keys, Accumulator[] accumulators ) {
         this.keys = keys;
         this.accumulators = new Accumulator[accumulators.length];
         for( int i = 0; i < accumulators.length; i++ ) {
            this.accumulators[i] = accumulators[i].clone();
         }
      }

      public List<Object> values() {
         final ArrayList<Object> result = new ArrayList<>( keys.length + accumulators.length );
         Collections.addAll( result, keys );
         for( Accumulator accumulator : accumulators ) result.add( accumulator.result() );
         return result;
      }

      public List<Object> calculatedValues() {
         final ArrayList<Object> result = new ArrayList<>( accumulators.length );
         for( Accumulator accumulator : accumulators ) result.add( accumulator.result() );
         return result;
      }
   }


   public static class GroupByStream {
      public final int[][] fields;
      private final HashMap<HashCodeCache, Data>[] agg;

      public GroupByStream( HashMap<HashCodeCache, Data>[] agg, int[][] fields ) {
         this.agg = agg;
         this.fields = fields;
      }

      public List<Table> getTables() {
         final ArrayList<Table> result = new ArrayList<>( agg.length );

         for( int i = 0; i < agg.length; i++ ) {
            final Collection<Data> values = agg[i].values();
            result.add( new Table( Stream.of( values.stream().map( Data::values ) ) ) );
         }

         return result;
      }

      public Map<String, List<Object>>[] getMaps( Function<Object[], String> str ) {
         @SuppressWarnings( "unchecked" )
         final Map<String, List<Object>>[] maps = new Map[agg.length];

         for( int i = 0; i < agg.length; i++ ) {
            final HashMap<String, List<Object>> map = new HashMap<>();
            maps[i] = map;

            agg[i].forEach( ( hc, value ) -> map.put( str.apply( hc.values ), value.calculatedValues() ) );
         }


         return maps;
      }
   }
}
