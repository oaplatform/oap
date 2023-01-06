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

import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Date;
import java.util.function.Supplier;

/**
 * Created by igor.petrenko on 2020-07-13.
 *
 * @todo refactor this to factory!
 */
public interface TemplateAccumulator<T, TMutable, TTemplateAccumulator extends TemplateAccumulator<T, TMutable, TTemplateAccumulator>> extends Supplier<T> {
    void acceptText( String text );

    void accept( String text );

    default void accept( Boolean b ) {
        accept( b.booleanValue() );
    }

    void accept( boolean b );

    default void accept( Byte b ) {
        accept( b.byteValue() );
    }

    void accept( byte b );

    default void accept( Short s ) {
        accept( s.shortValue() );
    }

    void accept( short s );

    default void accept( Integer i ) {
        accept( i.intValue() );
    }

    void accept( int i );

    default void accept( Long l ) {
        accept( l.longValue() );
    }

    void accept( long l );

    default void accept( Float f ) {
        accept( f.floatValue() );
    }

    void accept( float f );

    default void accept( Double d ) {
        accept( d.doubleValue() );
    }

    void accept( DateTime jodaDateTime );

    default void accept( Date javaDateTime ) {
        accept( new DateTime( javaDateTime ) );
    }

    void accept( double d );

    void accept( Enum<?> e );

    void accept( Collection<?> list );

    void accept( TTemplateAccumulator acc );

    void accept( Object obj );

    boolean isEmpty();

    TTemplateAccumulator newInstance();

    TTemplateAccumulator newInstance( TMutable mutable );

    String getTypeName();

    String delimiter();

    byte[] getBytes();

    TTemplateAccumulator addEol( boolean eol );

    void reset();
}
