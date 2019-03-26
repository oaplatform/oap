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
import oap.util.Sets;
import oap.util.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static oap.tsv.TypedListModel.ColumnType.BOOLEAN;
import static oap.tsv.TypedListModel.ColumnType.DOUBLE;
import static oap.tsv.TypedListModel.ColumnType.INT;
import static oap.tsv.TypedListModel.ColumnType.LONG;
import static oap.tsv.TypedListModel.ColumnType.STRING;

public class TypedListModel extends Model<List<Object>> {
    private List<ColumnFunction> columns = new ArrayList<>();
    private Map<String, Integer> nameToIndexMap = new HashMap<>();

    protected TypedListModel( boolean withHeader ) {
        super( withHeader );
    }

    private void add( ColumnFunction f ) {
        int index = this.columns.size();
        this.columns.add( f );
        this.nameToIndexMap.put( f.name, index );
    }

    public ColumnFunction getColumn( int index ) {
        return columns.get( index );
    }

    public TypedListModel column( String name, ColumnType type, int index ) {
        add( new Column( name, index, type ) );
        return this;
    }

    public TypedListModel columnValue( String name, ColumnType type, Object value ) {
        add( new Value( name, value, type ) );
        return this;
    }

    public List<Object> map( List<String> line ) {
        final ArrayList<Object> result = new ArrayList<>( line.size() );

        for( var column : columns ) {
            try {
                result.add( column.apply( line ) );
            } catch( IndexOutOfBoundsException e ) {
                throw new TsvException( "line does not contain a column with index " + column + ": " + Strings.join( "|", line, "[", "]" ), e );
            } catch( Exception e ) {
                throw new TsvException( "at column " + column + " " + e, e );
            }
        }

        return result;
    }

    public TypedListModel s( String name, int index ) {
        return column( name, STRING, index );
    }

    public TypedListModel i( String name, int index ) {
        return column( name, INT, index );
    }

    public TypedListModel l( String name, int index ) {
        return column( name, LONG, index );
    }

    public TypedListModel d( String name, int index ) {
        return column( name, DOUBLE, index );
    }

    public TypedListModel b( String name, int index ) {
        return column( name, BOOLEAN, index );
    }

    public TypedListModel v( String name, ColumnType type, Object value ) {
        return columnValue( name, type, value );
    }

    public TypedListModel filtered( Predicate<List<String>> filter ) {
        this.filter = this.filter == null ? filter : this.filter.and( filter );
        return this;
    }

    public TypedListModel filterColumnCount( int count ) {
        return filtered( l -> l.size() == count );
    }

    public int size() {
        return columns.size();
    }

    public TypedListModel join( TypedListModel model ) {
        model.columns.forEach( model::add );
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

    public final void column( ColumnFunction function ) {
        columns.add( function );
    }

    public final Integer getOffset( String name ) {
        final Integer index = nameToIndexMap.get( name );
        if( index == null ) return null;

        final ColumnFunction columnFunction = columns.get( index );
        if( columnFunction instanceof Column ) return ( ( Column ) columnFunction ).index;

        return index;
    }

    public final Set<String> names() {
        return nameToIndexMap.keySet();
    }

    public final TypedListModel filter( String... columns ) {
        return filter( Sets.of( columns ) );
    }

    public final TypedListModel filter( Set<String> columns ) {
        final TypedListModel model = new TypedListModel( withHeader );

        this.columns
            .stream()
            .filter( c -> columns.contains( c.name ) )
            .forEach( model::add );

        return model;
    }

    public final TypedListModel syncOffsetToIndex() {
        final TypedListModel model = new TypedListModel( withHeader );
        for( int i = 0; i < columns.size(); i++ ) {
            final ColumnFunction columnFunction = columns.get( i );
            if( columnFunction instanceof Column ) {
                final Column column = ( Column ) columnFunction;
                column.index = i;
                model.add( new Column( column.name, i, column.type ) );
            } else {
                model.add( columnFunction );
            }
        }

        return model;
    }

    public int maxOffset() {
        int max = 0;

        for( int i = 0; i < columns.size(); i++ ) {
            ColumnFunction columnFunction = columns.get( i );
            if( columnFunction instanceof Column )
                max = Math.max( max, ( ( Column ) columnFunction ).index + 1 );
            else max = Math.max( max, i + 1 );
        }

        return max;
    }

    public enum ColumnType {
        INT, LONG, DOUBLE, BOOLEAN, STRING
    }

    abstract static class ColumnFunction implements Function<List<String>, Object> {
        public final String name;
        public final ColumnType type;

        public ColumnFunction( String name, ColumnType type ) {
            this.name = name;
            this.type = type;
        }
    }

    @ToString
    private static class Column extends ColumnFunction {
        int index;

        public Column( String name, int index, ColumnType type ) {
            super( name, type );

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

        public Value( String name, Object value, ColumnType type ) {
            super( name, type );
            this.value = value;
        }

        @Override
        public Object apply( List<String> strings ) {
            return value;
        }
    }
}
