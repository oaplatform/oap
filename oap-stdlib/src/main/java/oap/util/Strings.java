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

import com.google.common.base.CharMatcher;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.util.Pair.__;

public final class Strings {
    public static final String UNDEFINED = "UNDEFINED";
    public static final String UNKNOWN = "UNKNOWN";
    private static String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

    private Strings() {}

    public static String substringAfter( String s, String delimiter ) {
        return s != null && s.contains( delimiter )
            ? s.substring( s.indexOf( delimiter ) + delimiter.length() ) : "";
    }

    public static String substringAfterLast( String s, String delimiter ) {
        return s != null && s.contains( delimiter )
            ? s.substring( s.lastIndexOf( delimiter ) + delimiter.length() ) : "";
    }

    public static String substringBefore( String s, String delimiter ) {
        return s != null && s.contains( delimiter )
            ? s.substring( 0, s.indexOf( delimiter ) ) : s;
    }

    public static String substringBeforeLast( String s, String delimiter ) {
        return s != null && s.contains( delimiter )
            ? s.substring( 0, s.lastIndexOf( delimiter ) ) : s;
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

    public static String toHexString( byte[] bytes ) {
        if( bytes == null ) return "";
        String result = "";
        for( byte b : bytes ) {
            int masked = b & 0xFF;
            result += masked < 16 ? "0" + hex[masked] : hex[masked >> 4] + hex[masked & 0x0F];
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

    /**
     * <p>Removes all occurrences of characters from within the source string.</p>
     */
    public static String remove( String str, char... characters ) {
        if( StringUtils.indexOfAny( str, characters ) < 0 ) return str;

        char[] output = new char[str.length()];
        int i = 0;

        next_ch:
        for( char ch : str.toCharArray() ) {
            for( char s : characters ) {
                if( ch == s ) continue next_ch;
            }
            output[i++] = ch;
        }

        return new String( output, 0, i );
    }

    public static String fill( String content, int times ) {
        final char[] charArray = content.toCharArray();
        final StringBuilder sb = new StringBuilder( content.length() * times + 1 );
        for( int i = 0; i < times; i++ ) {
            sb.append( charArray );
        }
        return sb.toString();
    }


    public static String regex( String s, String regex ) {
        Matcher matcher = Pattern.compile( regex, Pattern.MULTILINE ).matcher( s );
        return matcher.find() ? matcher.group( 1 ) : null;
    }

    public static List<String> regexAll( String s, String regex ) {
        Matcher matcher = Pattern.compile( regex, Pattern.MULTILINE ).matcher( s );
        List<String> result = new ArrayList<>();
        while( matcher.find() )
            for( int i = 0; i < matcher.groupCount(); i++ )
                result.add( matcher.group( i + 1 ) );
        return result;
    }

    public static int indexOfAny( String value, String any ) {
        return indexOfAny( value, any, 0 );
    }

    public static int indexOfAny( String value, String any, int start ) {
        try {
            int length = value.length();

            for( int i = start; i < length; ++i ) {
                if( any.indexOf( value.charAt( i ) ) >= 0 ) {
                    return i;
                }
            }

            return -1;
        } catch( StringIndexOutOfBoundsException var5 ) {
            return -1;
        }
    }

    public static boolean isGuid( String s ) {
        return s != null && s.length() == 36
            && check( s, 0, 8 ) && s.charAt( 8 ) == '-'
            && check( s, 9, 4 ) && s.charAt( 13 ) == '-'
            && check( s, 14, 4 ) && s.charAt( 18 ) == '-'
            && check( s, 19, 4 ) && s.charAt( 23 ) == '-'
            && check( s, 24, 12 );

    }

    private static boolean check( String idfa, int start, int length ) {
        for( int i = start; i < start + length; i++ ) {
            char ch = idfa.charAt( i );
            if( !( ( ch >= '0' && ch <= '9' )
                || ( ch >= 'A' && ch <= 'F' )
                || ( ch >= 'a' && ch <= 'f' ) ) ) return false;
        }

        return true;
    }

    public static String deepToString( Object value ) {
        if( value == null ) return "null";
        if( value instanceof Object[] ) return java.util.Arrays.deepToString( ( Object[] ) value );
        if( value instanceof int[] ) return java.util.Arrays.toString( ( int[] ) value );
        if( value instanceof long[] ) return java.util.Arrays.toString( ( long[] ) value );
        if( value instanceof short[] ) return java.util.Arrays.toString( ( short[] ) value );
        if( value instanceof byte[] ) return java.util.Arrays.toString( ( byte[] ) value );
        if( value instanceof boolean[] ) return java.util.Arrays.toString( ( boolean[] ) value );
        if( value instanceof char[] ) return java.util.Arrays.toString( ( char[] ) value );
        if( value instanceof float[] ) return java.util.Arrays.toString( ( float[] ) value );
        if( value instanceof double[] ) return java.util.Arrays.toString( ( double[] ) value );
        return value.toString();
    }

    public static String sortLines( CharSequence string ) {
        String[] lines = String.valueOf( string ).split( "\\n" );
        java.util.Arrays.sort( lines );
        return Strings.join( "\n", Lists.of( lines ) );
    }


    public static String toQuotedPrintable( String string ) {
        StringBuilder result = new StringBuilder();
        char[] chars = string.toCharArray();
        for( int i = 0; i < chars.length; i++ ) {
            byte[] bytes = String.valueOf( chars[i] ).getBytes( UTF_8 );
            result.append( "=?UTF-8?Q?" );
            for( byte b : bytes )
                result.append( "=" ).append( Integer.toHexString( b & 0xff ).toUpperCase() );

            result.append( "?=" );
            if( i < chars.length - 1 ) result.append( ' ' );
        }

        return result.toString();
    }
}
