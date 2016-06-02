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
package oap.tsv;

import lombok.ToString;
import lombok.val;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.joining;
import static oap.tsv.Model.ColumnType.BOOLEAN;
import static oap.tsv.Model.ColumnType.DOUBLE;
import static oap.tsv.Model.ColumnType.INT;
import static oap.tsv.Model.ColumnType.LONG;
import static oap.tsv.Model.ColumnType.STRING;

public class Model {
   public final boolean withHeader;
   private Predicate<List<String>> filter;
   private List<ColumnFunction> columns = new ArrayList<>();

   public Model( boolean withHeader ) {
      this.withHeader = withHeader;
   }

   public static Model withoutHeader() {
      return new Model( false );
   }

   public static Model withHeader() {
      return new Model( true );
   }

   public static Complex complex( Function<Path, Model> modelBuilder ) {
      return new Complex( modelBuilder );
   }

   public ColumnFunction getColumn( int index ) {
      return columns.get( index );
   }

   public Model column( ColumnType type, int index, int... more ) {
      this.columns.add( new Column( index, type ) );
      return column( type, more );
   }

   public Model columnValue( ColumnType type, Object value ) {
      this.columns.add( new Value( value, type ) );
      return this;
   }

   public Model column( ColumnType type, int[] indices ) {
      for( int i : indices ) this.columns.add( new Column( i, type ) );
      return this;
   }

   public List<Object> convert( List<String> line ) {
      final ArrayList<Object> result = new ArrayList<>( line.size() );

      for( val column : columns ) {
         try {
            result.add( column.apply( line ) );
         } catch( IndexOutOfBoundsException e ) {
            String lineToPrint = "[" + Stream.of( line ).collect( joining( "|" ) ) + "]";
            throw new TsvException(
               "line does not contain a column with index " + column + ": " + lineToPrint, e );
         } catch( Exception e ) {
            throw new TsvException( "at column " + column + " " + e, e );
         }
      }

      return result;
   }

   public Model s( int[] indices ) {
      return column( STRING, indices );
   }

   public Model s( int index, int... more ) {
      return column( STRING, index, more );
   }

   public Model i( int index, int... more ) {
      return column( INT, index, more );
   }

   public Model l( int index, int... more ) {
      return column( LONG, index, more );
   }

   public Model d( int index, int... more ) {
      return column( DOUBLE, index, more );
   }

   public Model b( int index, int... more ) {
      return column( BOOLEAN, index, more );
   }

   public Model v( ColumnType type, Object value ) {
      return columnValue( type, value );
   }

   public Model filtered( Predicate<List<String>> filter ) {
      this.filter = this.filter == null ? filter : this.filter.and( filter );
      return this;
   }

   public Model filterColumnCount( int count ) {
      return filtered( l -> l.size() == count );
   }

   public int size() {
      return columns.size();
   }

   public Model join( Model model ) {
      this.columns.addAll( model.columns );
      return this;
   }

   public Predicate<? super List<String>> filter() {
      return this.filter == null ? l -> true : this.filter;
   }

   public ColumnType getType( int index ) {
      return getTypeOpt( index ).get();
   }

   public Optional<ColumnType> getTypeOpt( int index ) {
      for( ColumnFunction f : columns ) {
         if( !( f instanceof Column ) ) return Optional.empty();

         if( ( ( Column ) f ).index == index ) return Optional.of( f.type );
      }
      return Optional.empty();
   }

   public void column( ColumnFunction function ) {
      columns.add( function );
   }

   public enum ColumnType {
      INT, LONG, DOUBLE, BOOLEAN, STRING
   }

   public static class Complex {
      private Function<Path, Model> modelBuilder;

      private Complex( Function<Path, Model> modelBuilder ) {
         this.modelBuilder = modelBuilder;
      }

      public Model modelFor( Path path ) {
         return modelBuilder.apply( path );
      }

   }

   private static abstract class ColumnFunction implements Function<List<String>, Object> {
      public final ColumnType type;

      public ColumnFunction( ColumnType type ) {
         this.type = type;
      }
   }

   @ToString
   private static class Column extends ColumnFunction {
      int index;

      public Column( int index, ColumnType type ) {
         super( type );

         this.index = index;
      }

      @Override
      public Object apply( List<String> line ) {
         final String value = line.get( index );
         switch( type ) {
            case BOOLEAN:
               return Boolean.parseBoolean( value );
            case STRING:
               return value;
            case INT:
               return Integer.parseInt( value );
            case LONG:
               return Long.parseLong( value );
            case DOUBLE:
               return Double.parseDouble( value );
            default:
               throw new IllegalStateException( "Unknown column type " + type );
         }
      }
   }

   @ToString
   private static class Value extends ColumnFunction {
      Object value;

      public Value( Object value, ColumnType type ) {
         super( type );
         this.value = value;
      }

      @Override
      public Object apply( List<String> strings ) {
         return value;
      }
   }
}
