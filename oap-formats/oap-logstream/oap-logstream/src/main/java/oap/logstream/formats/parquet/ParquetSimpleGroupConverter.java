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

import org.apache.parquet.example.data.Group;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.Type;

public class ParquetSimpleGroupConverter extends GroupConverter {
    private final ParquetSimpleGroupConverter parent;
    private final int index;
    protected Group current;
    private Converter[] converters;

    ParquetSimpleGroupConverter( ParquetSimpleGroupConverter parent, int index, GroupType schema ) {
        this.parent = parent;
        this.index = index;

        converters = new Converter[schema.getFieldCount()];

        for( int i = 0; i < converters.length; i++ ) {
            final Type type = schema.getType( i );
            if( type.isPrimitive() ) {
                converters[i] = new ParquetSimplePrimitiveConverter( this, i );
            } else {
                converters[i] = new ParquetSimpleGroupConverter( this, i, type.asGroupType() );
            }

        }
    }

    @Override
    public void start() {
        current = parent.getCurrentRecord().addGroup( index );
    }

    @Override
    public Converter getConverter( int fieldIndex ) {
        return converters[fieldIndex];
    }

    @Override
    public void end() {
    }

    public Group getCurrentRecord() {
        return current;
    }
}
