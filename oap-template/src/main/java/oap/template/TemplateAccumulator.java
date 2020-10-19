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

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Created by igor.petrenko on 2020-07-13.
 */
public interface TemplateAccumulator<T, TTemplateAccumulator extends TemplateAccumulator<T, TTemplateAccumulator>> extends Supplier<T> {
    void accept( String text );

    void accept( boolean b );

    void accept( char ch );

    void accept( byte b );

    void accept( short s );

    void accept( int i );

    void accept( long l );

    void accept( float f );

    void accept( double d );

    void accept( Enum<?> e );

    void accept( Collection<?> list );

    void accept( TTemplateAccumulator acc );

    void accept( Object obj );

    boolean isEmpty();

    TTemplateAccumulator newInstance();

    String getTypeName();
}
