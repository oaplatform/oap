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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.etl.Export;
import oap.etl.Join;
import oap.etl.Table;
import oap.etl.Table.GroupBy;
import oap.etl.accumulator.Filter;
import oap.tsv.Model;
import oap.util.Arrays;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.util.Pair.__;

@Slf4j
public class AggregatorBuilder {
   private final HashMap<String, TableModel> tables = new HashMap<>();
   private Aggregator configuration;
   private HashMap<String, Export> exports = new HashMap<>();

   public static AggregatorBuilder custom() {
      return new AggregatorBuilder();
   }

   public AggregatorBuilder withModel( String name, Model model, Map<String, Integer> mapping ) {
      tables.put( name, new TableModel( model, mapping ) );

      return this;
   }

   public AggregatorBuilder withConfiguration( Aggregator configuration ) {
      this.configuration = configuration;

      return this;
   }

   public void build() {
      val groupByStream = build( configuration );

      final List<Table> tables = groupByStream.getTables();

      for( int i = 0; i < tables.size(); i++ ) {
         tables
            .get( i )
            .sort( groupByStream.fields[i] )
            .export( exports.get( configuration.getExport() ) )
            .compute();
      }
   }

   private Table.GroupByStream build( IAggregator configuration ) {
      final TableModel tableModel = getTable( configuration.getTable() );
      Table table = tableModel.table;

      for( val joinEntry : configuration.getJoins().entrySet() ) {
         val joinName = joinEntry.getKey();
         val aggregator = joinEntry.getValue();

         val groupByStream = build( aggregator );

         final Model fromModel = tableModel.model;

         final Model model = new Model( false );
         val mapping = new HashMap<String, Integer>();

         final ArrayList<Integer> indexes = Lists.of( groupByStream.fields[0] );
         val offset = tableModel.model.size();
         final List<Accumulator> accumulators = aggregator.getAccumulators();
         for( int i = 0; i < accumulators.size(); i++ ) {
            final Accumulator ac = accumulators.get( i );

            final Optional<Pair<Integer, Model.ColumnType>> fieldInfo = ac.field.map( f -> {
               final String[] split = StringUtils.split( f, '.' );

               final Integer index = split.length == 1 ? tableModel.mapping.get( f ) : getTable( split[0] ).mapping.get( split[1] );
               if( index == null ) throw new IllegalArgumentException( "Unknown column " + f );
               return __( index, tableModel.model.getType( index ) );
            } );

            model.column( AccumulatorFactory.create( ac.type, fieldInfo ).getModelType(), i + offset );
            mapping.put( ac.name, i + offset );
         }

         for( int index : indexes ) {
            model.column( fromModel.getColumn( index ) );
         }

         tableModel.mapping.forEach( ( key, value ) -> {
            if( Arrays.contains( key, groupByStream.fields[0] ) ) {
               mapping.put( key, value );
            }
         } );

         tables.put( joinName, new TableModel( model, mapping ) );

         final Map<String, List<Object>> map = groupByStream.getMaps( objs -> objs[0].toString() )[0];
         table = table.join( tableModel.mapping.get( Maps.head( joinEntry.getValue().getAggregates() ).getValue().get( 0 ) ), ( Join ) map::get );
      }


      final ArrayList<GroupBy> result = new ArrayList<>();

      for( val entry : configuration.getAggregates().entrySet() ) {
         final int[] groupByPosition = entry.getValue().stream().mapToInt( tableModel.mapping::get ).toArray();

         final ArrayList<oap.etl.accumulator.Accumulator> accumulators = new ArrayList<>();

         for( Accumulator ac : configuration.getAccumulators() ) {
            final Optional<Pair<Integer, Model.ColumnType>> fieldInfo = ac.field.map( f -> {
               final String[] split = StringUtils.split( f, '.' );

               final TableModel fieldTableModel = split.length == 1 ? tableModel : getTable( split[0] );

               final Integer index = split.length == 1 ? fieldTableModel.mapping.get( f ) : fieldTableModel.mapping.get( split[1] );
               if( index == null ) throw new IllegalArgumentException( "Unknown column " + f );
               return __( index, fieldTableModel.model.getTypeOpt( index )
                  .orElseThrow( () -> new IllegalArgumentException( "column = " + f + ", accumulator = " + ac.name ) ) );
            } );

            log.trace( "[{}/{}] looking for accumulator type = {}, fields = {}, fi = {}",
               configuration.getTable(), ac.name, ac.type, ac.field, fieldInfo );

            oap.etl.accumulator.Accumulator accumulator = AccumulatorFactory.create( ac.type, fieldInfo );

            final Accumulator.Filter filter = ac.filter.orElse( null );
            if( filter != null ) {
               final Integer fieldIndex = tableModel.mapping.get( filter.field );
               if( fieldIndex == null ) throw new IllegalArgumentException( "Filter: unknown fields " + filter.field );

               accumulator = new Filter( accumulator, fieldIndex, filter.getFunction() );
            }

            accumulators.add( accumulator );
         }

         final GroupBy groupBy = new GroupBy( groupByPosition, accumulators.toArray( new oap.etl.accumulator.Accumulator[accumulators.size()] ) );
         result.add( groupBy );
      }

      final Table.GroupByStream groupByStream = table
         .groupBy( result.toArray( new GroupBy[result.size()] ) );


      return groupByStream;
   }

   private TableModel getTable( String key ) {
      final TableModel tableModel = tables.get( key );
      if( tableModel == null ) throw new IllegalArgumentException( "Unknown table " + key );
      return tableModel;
   }

   public AggregatorBuilder withTable( String name, Table table ) {
      getTable( name ).table = table;
      return this;
   }

   public AggregatorBuilder withExport( String name, Export export ) {
      exports.put( name, export );
      return this;
   }

   private static class TableModel {
      private Model model;
      private Map<String, Integer> mapping;
      private Table table;

      public TableModel( Model model, Map<String, Integer> mapping ) {
         this.model = model;
         this.mapping = mapping;
      }
   }
}
