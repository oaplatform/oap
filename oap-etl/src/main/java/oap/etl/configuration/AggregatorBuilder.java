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
import org.apache.commons.lang3.ArrayUtils;
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

   public AggregatorBuilder withModel( DictionaryModel dictionaryModel ) {
      for( val table : dictionaryModel.getTables() ) {
         tables.put( table, new TableModel( dictionaryModel.toModel( table ), dictionaryModel.toMap( table ) ) );
      }

      return this;
   }

   public AggregatorBuilder withConfiguration( Aggregator configuration ) {
      this.configuration = configuration;

      return this;
   }

   public void build() {
      Table.GroupByStream groupByStream = build( configuration );

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
      final int offset = tableModel.model.size();

      for( val joinEntry : configuration.getJoins().entrySet() ) {
         table = join( table, tableModel.mapping, offset, joinEntry.getKey(), joinEntry.getValue() );
      }


      final ArrayList<GroupBy> result = new ArrayList<>();

      for( val groupByFields : configuration.getAggregates().values() ) {
         val groupByPosition = nameToIndex( groupByFields, tableModel );

         val acs = configuration.getAccumulators();
         val accumulators = new oap.etl.accumulator.Accumulator[acs.size()];

         for( int i = 0; i < acs.size(); i++ ) {
            val ac = acs.get( i );

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

         final GroupBy groupBy = new GroupBy( groupByPosition, accumulators );
         result.add( groupBy );
      }

      return table.groupBy( result.toArray( new GroupBy[result.size()] ) );
   }

   private oap.etl.accumulator.Accumulator getAccumulator( TableModel tableModel, Accumulator ac, Optional<Pair<Integer, Model.ColumnType>> fieldInfo ) {
      val accumulator = AccumulatorFactory.create( ac.type, fieldInfo );

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
            final Integer v = tableModel.mapping.get( f );
            if( v == null )
               throw new IllegalArgumentException( "Unknown aggregate field " + f + ", model: " + tableModel.mapping.keySet() );
            return v;
         } )
         .toArray();
   }

   private Table join( Table table, Map<String, Integer> tableMapping,
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

         model.column( AccumulatorFactory.create( ac.type, fieldInfo ).getModelType(), i + offset );
         mapping.put( ac.name, i + offset );
      }

      for( int index : indexes ) {
         model.column( fromModel.getColumn( index ) );
      }

      joinTableModel.mapping.forEach( ( key, value ) -> {
         if( ArrayUtils.contains( groupByStream.fields[0], value ) ) {
            mapping.put( key, value );
         }
      } );

      tables.put( joinName, new TableModel( model, mapping ) );

      final Map<String, List<Object>> map = groupByStream.getMaps( objs -> objs[0].toString() )[0];
      final int keyPos = tableMapping.get( Maps.head( join.getAggregates() ).getValue().get( 0 ) );
      return table.join(
         keyPos,
         ( Join ) key -> {
            List<Object> line = map.get( key );
            if( line == null ) line = join.getDefaultLine();
            return line;
         }
      );
   }

   private int fieldPathToIndex( String field, TableModel tableModel ) {
      final String[] split = StringUtils.split( field, '.' );

      final TableModel fieldTableModel = split.length == 1 ? tableModel : getTable( split[0] );
      final Integer fieldIndex = split.length == 1 ? fieldTableModel.mapping.get( field ) : fieldTableModel.mapping.get( split[1] );
      if( fieldIndex == null )
         throw new IllegalArgumentException( "Filter: unknown field " + field + ", model: " + fieldTableModel.mapping.keySet() );

      return fieldIndex;
   }

   private TableModel fieldPathToTableModel( String field, TableModel tableModel ) {
      final String[] split = StringUtils.split( field, '.' );

      return split.length == 1 ? tableModel : getTable( split[0] );
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
