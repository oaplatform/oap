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
import oap.tsv.DictionaryModel;
import oap.tsv.Model;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static oap.util.Pair.__;

@Slf4j
public class AggregatorBuilder {
   private final HashMap<String, TableModel> tables = new HashMap<>();
   private Aggregator configuration;
   private HashMap<String, Function<String, Export>> exports = new HashMap<>();

   public static AggregatorBuilder custom() {
      return new AggregatorBuilder();
   }

   public AggregatorBuilder withModel( String name, Model model ) {
      tables.put( name, new TableModel( model ) );

      return this;
   }

   public AggregatorBuilder withModel( DictionaryModel dictionaryModel ) {
      for( val table : dictionaryModel.getTables() ) {
         tables.put( table, new TableModel( dictionaryModel.toModel( table ) ) );
      }

      return this;
   }

   public AggregatorBuilder withConfiguration( Aggregator configuration ) {
      this.configuration = configuration;

      return this;
   }

   public void build() {
      Table.GroupByStream groupByStream = build( configuration );

      final List<Pair<String, Table>> tables = groupByStream.getTables();

      for( int i = 0; i < tables.size(); i++ ) {
         final Pair<String, Table> tn = tables.get( i );

         final Function<String, Export> exportFunction = exports.get( configuration.getExport() );
         if( exportFunction == null )
            throw new IllegalStateException( "Unknown export function " + configuration.getExport() );
         tn._2
            .sort( groupByStream.fields[i] )
            .export( exportFunction.apply( tn._1 ) )
            .compute();
      }
   }

   private Table.GroupByStream build( IAggregator configuration ) {
      final TableModel tableModel = getTable( configuration.getTable() );
      Table table = tableModel.table;
      int offset = tableModel.model.size();

      for( val joinEntry : configuration.getJoins().entrySet() ) {
         Pair<Table, Integer> p = join( table, tableModel.model, offset, joinEntry.getKey(), joinEntry.getValue() );
         table = p._1;
         offset = p._2;
      }


      final ArrayList<GroupBy> result = new ArrayList<>();

      configuration.getAggregates().forEach( ( name, groupByFields ) -> {
         final int[] groupByPosition = nameToIndex( groupByFields, tableModel );

         final List<Accumulator> acs = configuration.getAccumulators();
         final oap.etl.accumulator.Accumulator[] accumulators = new oap.etl.accumulator.Accumulator[acs.size()];

         for( int i = 0; i < acs.size(); i++ ) {
            final Accumulator ac = acs.get( i );

            final Optional<Pair<Integer, Model.ColumnType>> fieldInfo = ac.field.map( f -> {
               final int index = fieldPathToIndex( f, tableModel );
               final TableModel fieldTableModel = fieldPathToTableModel( f, tableModel );

               return __( index, fieldTableModel.model.getTypeOpt( index )
                  .orElseThrow( () -> new IllegalArgumentException( "column = " + f + ", accumulator = " + ac.name ) ) );
            } );

            log.trace( "[{}/{}] looking for accumulator type = {}, fields = {}, fi = {}",
               configuration.getTable(), ac.name, ac.type, ac.field, fieldInfo );

            accumulators[i] = getAccumulator( tableModel, ac, fieldInfo );
         }

         final GroupBy groupBy = new GroupBy( name, groupByPosition, accumulators );
         result.add( groupBy );
      } );

      return table.groupBy( result.toArray( new GroupBy[result.size()] ) );
   }

   private oap.etl.accumulator.Accumulator getAccumulator( TableModel tableModel, Accumulator ac, Optional<Pair<Integer, Model.ColumnType>> fieldInfo ) {
      val accumulator = AccumulatorFactory.create( ac, fieldInfo );

      final Accumulator.Filter filter = ac.filter.orElse( null );
      if( filter != null ) {
         final int index = fieldPathToIndex( filter.field, tableModel );

         return new Filter<>( accumulator, index, filter.getFunction() );
      }

      return accumulator;
   }

   private int[] nameToIndex( List<String> groupByFields, TableModel tableModel ) {
      return groupByFields
         .stream()
         .mapToInt( f -> {
            final Integer v = tableModel.model.getOffset( f );
            if( v == null )
               throw new IllegalArgumentException( "Unknown aggregate field " + f + ", model: " + tableModel.model.names() );
            return v;
         } )
         .toArray();
   }

   private Pair<Table, Integer> join( Table table, Model joinModel,
                                      int offset, String joinName, oap.etl.configuration.Join join ) {
      final TableModel joinTableModel = getTable( join.getTable() );

      Table.GroupByStream groupByStream = build( join );

      val fromModel = joinTableModel.model;

      val model = new Model( false );
      Map<String, Integer> mapping = new HashMap<>();

      final ArrayList<Integer> indexes = Lists.of( groupByStream.fields[0] );

      val accumulators = join.getAccumulators();
      for( int i = 0; i < accumulators.size(); i++ ) {
         final Accumulator ac = accumulators.get( i );

         final Optional<Pair<Integer, Model.ColumnType>> fieldInfo = ac.field.map( f -> {
            final int index = fieldPathToIndex( f, joinTableModel );

            return __( index, joinTableModel.model.getType( index ) );
         } );

         model.column( ac.name, AccumulatorFactory.create( ac, fieldInfo ).getModelType(), i + offset );
         mapping.put( ac.name, i + offset );
      }

      for( int index : indexes ) {
         model.column( fromModel.getColumn( index ) );
      }

      final TableModel newTableModel = new TableModel( model );
      tables.put( joinName, newTableModel );

      final Map<String, List<Object>> map = groupByStream.getMaps( objs -> objs[0].toString() )[0];
      final int keyPos = joinModel.getOffset( Maps.head( join.getAggregates() ).getValue().get( 0 ) );
      return __( table.join(
         keyPos,
         ( Join ) key -> {
            List<Object> line = map.get( key );
            if( line == null ) line = join.getDefaultLine();
            return line;
         }
      ), offset + newTableModel.model.size() - 1 );
   }

   private int fieldPathToIndex( String reference, TableModel tableModel ) {
      final TableModel fieldTableModel = FieldUtils.getTable( reference ).map( this::getTable ).orElse( tableModel );
      final Integer fieldIndex = fieldTableModel.model.getOffset( FieldUtils.getField( reference ) );
      if( fieldIndex == null )
         throw new IllegalArgumentException( "Filter: unknown reference " + reference + ", model: " + fieldTableModel.model.names() );

      return fieldIndex;
   }

   private TableModel fieldPathToTableModel( String reference, TableModel tableModel ) {
      return FieldUtils.getTable( reference ).map( this::getTable ).orElse( tableModel );
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

   public AggregatorBuilder withExport( String name, Function<String, Export> export ) {
      exports.put( name, export );
      return this;
   }

   private static class TableModel {
      private Model model;
      private Table table;

      public TableModel( Model model ) {
         this.model = model;
      }
   }
}
