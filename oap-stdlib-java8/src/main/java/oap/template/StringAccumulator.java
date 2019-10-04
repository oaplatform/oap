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

package oap.template;

public class StringAccumulator implements Accumulator<String> {
    private final StringBuilder sb;

    public StringAccumulator( StringBuilder sb ) {
        this.sb = sb;
    }

    @Override
    public String build() {
        return sb.toString();
    }

    @Override
    public Accumulator accept( Object o ) {
        sb.append( o );
        return this;
    }

    @Override
    public Accumulator accept( String str ) {
        sb.append( str );
        return this;
    }

    @Override
    public Accumulator accept( boolean value ) {
        sb.append( value );
        return this;
    }

    @Override
    public Accumulator accept( char value ) {
        sb.append( value );
        return this;
    }

    @Override
    public Accumulator accept( int value ) {
        sb.append( value );
        return this;
    }

    @Override
    public Accumulator accept( long value ) {
        sb.append( value );
        return this;
    }

    @Override
    public Accumulator accept( float value ) {
        sb.append( value );
        return this;
    }

    @Override
    public Accumulator accept( double value ) {
        sb.append( value );
        return this;
    }

    @Override
    public Accumulator accept( StringBuffer value ) {
        sb.append( value );
        return this;
    }

    @Override
    public Accumulator accept( CharSequence value ) {
        sb.append( value );
        return this;
    }
}
