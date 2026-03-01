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

import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

import java.util.Date;

public enum Types {
    EOL( 0, null ),
//    RAW( 1, null ),
    DATETIME( 2, DateTime.class ),
    DATE( 3, Date.class ),
    BOOLEAN( 4, Boolean.class ),
    BYTE( 5, Byte.class ),
    SHORT( 6, Short.class ),
    INTEGER( 7, Integer.class ),
    LONG( 8, Long.class ),
    FLOAT( 9, Float.class ),
    DOUBLE( 10, Double.class ),
    STRING( 11, String.class ),
    LIST( 12, null );

    public final byte id;
    public final Class<?> clazz;

    Types( int id, Class<?> clazz ) {
        Preconditions.checkArgument( id == ( id & 0xFF ) );

        this.id = ( byte ) id;
        this.clazz = clazz;
    }

    public static Types valueOf( byte type ) {
        for( var v : values() ) {
            if( v.id == type ) return v;
        }

        throw new IllegalArgumentException( "Unknown id " + type );
    }
}
