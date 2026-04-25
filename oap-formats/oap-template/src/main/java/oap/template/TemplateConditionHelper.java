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

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class TemplateConditionHelper {
    private TemplateConditionHelper() {}

    public static boolean isTruthy( @Nullable Object value ) {
        if( value == null ) return false;
        if( value instanceof Boolean b ) return b;
        if( value instanceof String s ) return !s.isEmpty();
        if( value instanceof Collection<?> c ) return !c.isEmpty();
        if( value instanceof Map<?, ?> m ) return !m.isEmpty();
        if( value.getClass().isArray() ) return Array.getLength( value ) > 0;
        return true;
    }

    public static boolean compare( @Nullable Object left, String op, String literal ) {
        return switch( normalizeOp( op ) ) {
            case "==" -> compareEq( left, literal );
            case "!=" -> !compareEq( left, literal );
            case ">" -> compareTo( left, literal ) > 0;
            case "<" -> compareTo( left, literal ) < 0;
            case ">=" -> compareTo( left, literal ) >= 0;
            case "<=" -> compareTo( left, literal ) <= 0;
            case "eqi" -> compareEqi( left, literal );
            case "contains" -> compareContains( left, literal );
            default -> throw new TemplateException( "Unknown comparison operator: " + op );
        };
    }

    private static boolean compareEq( @Nullable Object left, String literal ) {
        if( left == null ) return false;
        if( left instanceof String s ) return s.equals( literal );
        if( left instanceof Integer i ) return i == Integer.parseInt( literal );
        if( left instanceof Long l ) return l == Long.parseLong( literal );
        if( left instanceof Double d ) return d == Double.parseDouble( literal );
        if( left instanceof Float f ) return f == Float.parseFloat( literal );
        if( left instanceof Boolean b ) return b == Boolean.parseBoolean( literal );
        if( left instanceof Short s ) return s == Short.parseShort( literal );
        if( left instanceof Byte b ) return b == Byte.parseByte( literal );
        return left.toString().equals( literal );
    }

    private static int compareTo( @Nullable Object left, String literal ) {
        if( left == null ) return -1;
        if( left instanceof Integer i ) return Integer.compare( i, Integer.parseInt( literal ) );
        if( left instanceof Long l ) return Long.compare( l, Long.parseLong( literal ) );
        if( left instanceof Double d ) return Double.compare( d, Double.parseDouble( literal ) );
        if( left instanceof Float f ) return Float.compare( f, Float.parseFloat( literal ) );
        if( left instanceof Short s ) return Short.compare( s, Short.parseShort( literal ) );
        if( left instanceof Byte b ) return Byte.compare( b, Byte.parseByte( literal ) );
        if( left instanceof String s ) return s.compareTo( literal );
        throw new TemplateException( "Cannot apply ordering operator to: " + left.getClass().getName() );
    }

    private static boolean compareEqi( @Nullable Object left, String literal ) {
        if( !( left instanceof String s ) )
            throw new TemplateException( "eqi operator requires String field, got: "
                + ( left == null ? "null" : left.getClass().getName() ) );
        return s.equalsIgnoreCase( literal );
    }

    private static boolean compareContains( @Nullable Object left, String literal ) {
        if( left instanceof List<?> list ) {
            if( list.isEmpty() ) return false;
            return list.contains( coerceLiteral( literal, list.get( 0 ) ) );
        }
        if( left instanceof Map<?, ?> map ) {
            if( map.isEmpty() ) return false;
            Object sampleKey = map.keySet().iterator().next();
            return map.containsKey( coerceLiteral( literal, sampleKey ) );
        }
        throw new TemplateException( "contains operator requires List or Map field, got: "
            + ( left == null ? "null" : left.getClass().getName() ) );
    }

    private static Object coerceLiteral( String literal, @Nullable Object sample ) {
        if( sample instanceof Integer ) return Integer.parseInt( literal );
        if( sample instanceof Long ) return Long.parseLong( literal );
        if( sample instanceof Double ) return Double.parseDouble( literal );
        if( sample instanceof Float ) return Float.parseFloat( literal );
        if( sample instanceof Short ) return Short.parseShort( literal );
        if( sample instanceof Byte ) return Byte.parseByte( literal );
        return literal;
    }

    private static String normalizeOp( String op ) {
        return switch( op ) {
            case "eq" -> "==";
            case "ne" -> "!=";
            default -> op;
        };
    }
}
