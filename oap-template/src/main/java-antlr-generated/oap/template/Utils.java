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

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import oap.util.Dates;
import oap.util.Strings;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;

@Slf4j
public class Utils {
    @SuppressWarnings( "unchecked" )
    public static boolean canConvert( String value, Class<?> to, Class<?> from ) {
        if( to.equals( Long.class ) )
            return Longs.tryParse( value ) != null;
        else if( to.equals( Integer.class ) ) {
            return Ints.tryParse( value ) != null;
        } else if( to.equals( Short.class ) ) {
            Integer res = Ints.tryParse( value );
            return res != null && ( int ) res.shortValue() == res;
        } else if( to.equals( Byte.class ) ) {
            Integer res = Ints.tryParse( value );
            return res != null && ( int ) res.byteValue() == res;
        } else if( to.equals( Float.class ) ) {
            return Floats.tryParse( value ) != null;
        } else if( to.equals( Double.class ) ) {
            return Doubles.tryParse( value ) != null;
        } else if( to.equals( Boolean.class ) ) {
            return "false".equalsIgnoreCase( value ) || "true".equalsIgnoreCase( value );
        } else if( to.equals( String.class ) ) {
            return true;
        } else if( to.equals( DateTime.class ) ) {
            if( value.length() <= 2 ) return false;

            try {
                Dates.FORMAT_SIMPLE_CLEAN.parseDateTime( value.substring( 1, value.length() - 1 ) );
                return true;
            } catch( IllegalArgumentException e ) {
                return false;
            }
        } else if( Enum.class.isAssignableFrom( to ) ) {
            if( value.length() < 2 ) return false;

            String enumValue = value.substring( 1, value.length() - 1 );
            try {
                Enum.valueOf( ( Class<Enum> ) to, enumValue );
                return true;
            } catch( IllegalArgumentException e ) {
                if( "".equals( enumValue ) ) {
                    try {
                        Enum.valueOf( ( Class<Enum> ) to, Strings.UNKNOWN );
                        return true;
                    } catch( IllegalArgumentException ignored ) {
                        log.error( e.getMessage(), e );
                        return false;
                    }
                }
                log.error( e.getMessage(), e );
                return false;
            }
        } else if( Collection.class.isAssignableFrom( to ) ) {
            return from.equals( List.class );
        } else {
            throw new IllegalArgumentException( "Unknown type " + to );
        }
    }
}
