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

import org.apache.commons.lang3.builder.Builder;

/**
 * Created by macchiatow on 07.06.17.
 */
public interface Accumulator<T> extends Builder<T> {
    default Accumulator accept( String str ) {
        return accept( ( Object ) str );
    }

    default Accumulator accept( boolean value ) {
        return accept( ( Object ) value );
    }

    default Accumulator accept( char value ) {
        return accept( ( Object ) value );
    }

    default Accumulator accept( int value ) {
        return accept( ( Object ) value );
    }

    default Accumulator accept( long value ) {
        return accept( ( Object ) value );
    }

    default Accumulator accept( float value ) {
        return accept( ( Object ) value );
    }

    default Accumulator accept( double value ) {
        return accept( ( Object ) value );
    }

    default Accumulator accept( StringBuffer value ) {
        return accept( ( Object ) value );
    }

    default Accumulator accept( CharSequence value ) {
        return accept( ( Object ) value );
    }

    Accumulator accept( Object o );

}
