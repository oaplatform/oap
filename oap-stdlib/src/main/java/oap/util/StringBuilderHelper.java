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

package oap.util;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;

public final class StringBuilderHelper {

    private static final Field COUNT_FIELD;
    private static final Field VALUE_FIELD;

    static {
        try {
            VALUE_FIELD = StringBuilder.class.getSuperclass().getDeclaredField( "value" );
            VALUE_FIELD.setAccessible( true );

            COUNT_FIELD = StringBuilder.class.getSuperclass().getDeclaredField( "count" );
            COUNT_FIELD.setAccessible( true );
        } catch( final NoSuchFieldException e ) {
            throw new RuntimeException( e );
        }
    }

    private StringBuilderHelper() {
    }

    public static void write( final StringBuilder stringBuilder, final Writer writer ) throws IOException {
        try {
            final char[] value = ( char[] ) VALUE_FIELD.get( stringBuilder );
            final int count = COUNT_FIELD.getInt( stringBuilder );

            writer.write( value, 0, count );
        } catch( final IllegalAccessException e ) {
            throw new RuntimeException( e );
        }
    }

    public static void append( final StringBuilder sbValue, final StringBuilder sbTo ) {
        try {
            final char[] value = ( char[] ) VALUE_FIELD.get( sbValue );
            final int count = COUNT_FIELD.getInt( sbValue );

            sbTo.append( value, 0, count );
        } catch( final IllegalAccessException e ) {
            throw new RuntimeException( e );
        }
    }

    public static StringBuilder clear( final StringBuilder stringBuilder ) {
        try {
            COUNT_FIELD.set( stringBuilder, 0 );

            return stringBuilder;
        } catch( IllegalAccessException e ) {
            throw new RuntimeException( e );
        }
    }

}
