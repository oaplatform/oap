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

import org.apache.parquet.example.data.simple.Primitive;
import org.apache.parquet.io.api.RecordConsumer;

public class ParquetNumberValue extends Primitive {
    private final Number value;

    public ParquetNumberValue( int value ) {
        this.value = value;
    }

    public ParquetNumberValue( boolean value ) {
        this.value = value ? 1 : 0;
    }

    public ParquetNumberValue( Number value ) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf( value );
    }

    @Override
    public int getInteger() {
        return value.intValue();
    }

    @Override
    public long getLong() {
        return value.longValue();
    }

    @Override
    public boolean getBoolean() {
        return value.intValue() == 1;
    }

    @Override
    public float getFloat() {
        return value.floatValue();
    }

    @Override
    public double getDouble() {
        return value.doubleValue();
    }

    @Override
    public void writeValue( RecordConsumer recordConsumer ) {
        if( value instanceof Integer integerValue ) recordConsumer.addInteger( integerValue );
        else if( value instanceof Long longValue ) recordConsumer.addLong( longValue );
        else if( value instanceof Float floatValue ) recordConsumer.addFloat( floatValue );
        else recordConsumer.addDouble( value.doubleValue() );
    }
}
