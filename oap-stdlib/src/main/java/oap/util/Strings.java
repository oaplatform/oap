/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

import com.google.common.base.CharMatcher;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.util.Pair.__;

public class Strings {
    public static final String UNDEFINED = "UNDEFINED";
    public static final String UNKNOWN = "UNKNOWN";

    public static String substringAfter( String s, String delimiter ) {
        return s != null && s.contains( delimiter ) ?
            s.substring( s.indexOf( delimiter ) + delimiter.length() ) : "";
    }

    public static String substringAfterLast( String s, String delimiter ) {
        return s != null && s.contains( delimiter ) ?
            s.substring( s.lastIndexOf( delimiter ) + delimiter.length() ) : "";
    }

    public static String substringBefore( String s, String delimiter ) {
        return s != null && s.contains( delimiter ) ?
            s.substring( 0, s.indexOf( delimiter ) ) : s;
    }

    public static boolean isEmpty( String s ) {
        return s == null || s.equals( "" );
    }

    public static Pair<String, String> split( String s, String delimiter ) {
        String[] split = StringUtils.splitByWholeSeparatorPreserveAllTokens( s, delimiter, 2 );
        return split.length == 2 ? __( split[0], split[1] ) : __( split[0], null );
    }

    public static byte[] toByteArray( String s ) {
        return s == null ? new byte[0] : s.getBytes( UTF_8 );
    }

    public static String toString( byte[] bytes ) {
        return bytes == null ? "" : new String( bytes, UTF_8 );
    }

    public static String readString( InputStream is ) {
        try {
            return toString( ByteStreams.toByteArray( is ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static String readString( URL url ) {
        try( InputStream is = url.openStream() ) {
            return Strings.readString( is );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static boolean isUndefined( String s ) {
        return UNDEFINED.equals( s );
    }


    private static String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

    public static String toHexString( byte[] bytes ) {
        if( bytes == null ) return "";
        String result = "";
        for( byte b : bytes ) {
            int masked = b & 0xFF;
            result += masked < 17 ? "0" + hex[masked] : hex[masked >> 4] + hex[masked & 0x0F];
        }
        return result;
    }

    @SafeVarargs
    public static String substitute( String s, Pair<String, Object>... map ) {
        return new StrSubstitutor( Maps.ofStrings( map ) ).replace( s );
    }

    public static String substitute( String s, Function<String, Object> mapper ) {
        return new StrSubstitutor( new StrLookup<Object>() {
            @Override
            public String lookup( String key ) {
                Object value = mapper.apply( key );
                return value == null ? "" : String.valueOf( value );
            }
        } ).replace( s );
    }

    public static String join( Collection<?> list ) {
        return join( ",", list );
    }


    public static String join( String delimiter, Collection<?> items ) {
        return join( delimiter, items, "", "" );
    }

    public static String join( String delimiter, Collection<?> items, String prefix, String suffix ) {
        StringJoiner joiner = new StringJoiner( delimiter, prefix, suffix );
        items.forEach( e -> joiner.add( String.valueOf( e ) ) );
        return joiner.toString();
    }

    public static void join( StringBuilder builder, Collection<?> items ) {
        join( builder, ",", items );
    }

    public static void join( StringBuilder builder, String delimiter, Collection<?> items ) {
        boolean first = true;
        for( Object value : items ) {
            if( first ) first = false;
            else builder.append( delimiter );
            builder.append( String.valueOf( value ) );
        }
    }

    public static String removeControl( String s ) {
        return s == null ? null : CharMatcher.JAVA_ISO_CONTROL.removeFrom( s );
    }

    public static String fill( String content, int times ) {
        String result = "";
        for( int i = 0; i < times; i++ ) result += content;
        return result;
    }
}
