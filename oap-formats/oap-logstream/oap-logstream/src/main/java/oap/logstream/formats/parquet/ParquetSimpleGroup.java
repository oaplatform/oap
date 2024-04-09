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

package oap.logstream.formats.parquet;

import lombok.extern.slf4j.Slf4j;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.BinaryValue;
import org.apache.parquet.example.data.simple.BooleanValue;
import org.apache.parquet.example.data.simple.Int96Value;
import org.apache.parquet.example.data.simple.NanoTime;
import org.apache.parquet.example.data.simple.Primitive;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.Type;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ParquetSimpleGroup extends Group {

    private final GroupType schema;
    private final List<Object>[] data;

    @SuppressWarnings( "unchecked" )
    public ParquetSimpleGroup( GroupType schema ) {
        this.schema = schema;
        this.data = new List[schema.getFields().size()];
        for( int i = 0; i < schema.getFieldCount(); i++ ) {
            this.data[i] = new ArrayList<>();
        }
    }

    @Override
    public String toString() {
        return toString( "" );
    }

    private StringBuilder appendToString( StringBuilder builder, String indent ) {
        int i = 0;
        for( Type field : schema.getFields() ) {
            String name = field.getName();
            List<Object> values = data[i];
            ++i;
            if( values != null && !values.isEmpty() ) {
                for( Object value : values ) {
                    builder.append( indent ).append( name );
                    if( value == null ) {
                        builder.append( ": NULL\n" );
                    } else if( value instanceof Group ) {
                        builder.append( '\n' );
                        ( ( ParquetSimpleGroup ) value ).appendToString( builder, indent + "  " );
                    } else {
                        builder.append( ": " ).append( value ).append( '\n' );
                    }
                }
            }
        }
        return builder;
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public String toString( String indent ) {
        StringBuilder builder = new StringBuilder();
        appendToString( builder, indent );
        return builder.toString();
    }

    @Override
    public Group addGroup( int fieldIndex ) {
        ParquetSimpleGroup g = new ParquetSimpleGroup( schema.getType( fieldIndex ).asGroupType() );
        add( fieldIndex, g );
        return g;
    }

    @Override
    public Group getGroup( int fieldIndex, int index ) {
        return ( Group ) getValue( fieldIndex, index );
    }

    public Object getValue( int fieldIndex, int index ) {
        List<Object> list;
        try {
            list = data[fieldIndex];
        } catch( IndexOutOfBoundsException e ) {
            throw new RuntimeException( "not found " + fieldIndex + "(" + schema.getFieldName( fieldIndex ) + ") in group:\n" + this );
        }
        try {
            return list.get( index );
        } catch( IndexOutOfBoundsException e ) {
            throw new RuntimeException( "not found " + fieldIndex + "(" + schema.getFieldName( fieldIndex ) + ") element number " + index + " in group:\n" + this );
        }
    }

    private void add( int fieldIndex, Primitive value ) {
        Type type = schema.getType( fieldIndex );
        List<Object> list = data[fieldIndex];
        if( !type.isRepetition( Type.Repetition.REPEATED )
            && !list.isEmpty() ) {
            throw new IllegalStateException( "field " + fieldIndex + " (" + type.getName() + ") can not have more than one value: " + list );
        }
        list.add( value );
    }

    @Override
    public int getFieldRepetitionCount( int fieldIndex ) {
        List<Object> list = data[fieldIndex];
        return list == null ? 0 : list.size();
    }

    @Override
    public String getValueToString( int fieldIndex, int index ) {
        return String.valueOf( getValue( fieldIndex, index ) );
    }

    @Override
    public String getString( int fieldIndex, int index ) {
        return ( ( BinaryValue ) getValue( fieldIndex, index ) ).getString();
    }

    @Override
    public int getInteger( int fieldIndex, int index ) {
        return ( ( ParquetNumberValue ) getValue( fieldIndex, index ) ).getInteger();
    }

    @Override
    public long getLong( int fieldIndex, int index ) {
        return ( ( ParquetNumberValue ) getValue( fieldIndex, index ) ).getLong();
    }

    @Override
    public double getDouble( int fieldIndex, int index ) {
        return ( ( ParquetNumberValue ) getValue( fieldIndex, index ) ).getDouble();
    }

    @Override
    public float getFloat( int fieldIndex, int index ) {
        return ( ( ParquetNumberValue ) getValue( fieldIndex, index ) ).getFloat();
    }

    @Override
    public boolean getBoolean( int fieldIndex, int index ) {
        Object value = getValue( fieldIndex, index );
        if( value instanceof BooleanValue booleanValue ) return booleanValue.getBoolean();
        return ( ( ParquetNumberValue ) value ).getBoolean();
    }

    @Override
    public Binary getBinary( int fieldIndex, int index ) {
        return ( ( BinaryValue ) getValue( fieldIndex, index ) ).getBinary();
    }

    public NanoTime getTimeNanos( int fieldIndex, int index ) {
        return NanoTime.fromInt96( ( Int96Value ) getValue( fieldIndex, index ) );
    }

    @Override
    public Binary getInt96( int fieldIndex, int index ) {
        return ( ( Int96Value ) getValue( fieldIndex, index ) ).getInt96();
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    @Override
    public void add( int fieldIndex, int value ) {
        add( fieldIndex, new ParquetNumberValue( value ) );
    }

    @Override
    public void add( int fieldIndex, long value ) {
        add( fieldIndex, new ParquetNumberValue( value ) );
    }

    @Override
    public void add( int fieldIndex, String value ) {
        add( fieldIndex, new BinaryValue( Binary.fromString( value ) ) );
    }

    @Override
    public void add( int fieldIndex, NanoTime value ) {
        add( fieldIndex, value.toInt96() );
    }

    @Override
    public void add( int fieldIndex, boolean value ) {
        add( fieldIndex, new BooleanValue( value ) );
    }

    @Override
    public void add( int fieldIndex, Binary value ) {
        switch( getType().getType( fieldIndex ).asPrimitiveType().getPrimitiveTypeName() ) {
            case BINARY, FIXED_LEN_BYTE_ARRAY -> add( fieldIndex, new BinaryValue( value ) );
            case INT96 -> add( fieldIndex, new Int96Value( value ) );
            default -> throw new UnsupportedOperationException(
                getType().asPrimitiveType().getName() + " not supported for Binary" );
        }
    }

    @Override
    public void add( int fieldIndex, float value ) {
        add( fieldIndex, new ParquetNumberValue( value ) );
    }

    @Override
    public void add( int fieldIndex, double value ) {
        add( fieldIndex, new ParquetNumberValue( value ) );
    }

    @Override
    public void add( int fieldIndex, Group value ) {
        data[fieldIndex].add( value );
    }

    @Override
    public GroupType getType() {
        return schema;
    }

    @Override
    public void writeValue( int field, int index, RecordConsumer recordConsumer ) {
        try {
            ( ( Primitive ) getValue( field, index ) ).writeValue( recordConsumer );
        } catch( Exception e ) {
            log.error( "field {} name {} index {}: {}", field, schema.getFieldName( index ), index, e.getMessage() );
            throw new RuntimeException( e );
        }
    }
}
