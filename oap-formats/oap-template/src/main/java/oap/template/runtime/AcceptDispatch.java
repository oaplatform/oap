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

package oap.template.runtime;

import oap.template.TemplateAccumulator;
import oap.template.TemplateAccumulatorString;
import oap.template.render.FieldType;
import oap.template.render.TemplateType;
import oap.util.Dates;
import oap.util.Strings;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Routes a runtime {@code value} + {@link TemplateType} to the correct typed
 * {@link TemplateAccumulator#accept} overload — doing at runtime what the code-generator
 * resolves statically.
 */
@SuppressWarnings( { "rawtypes", "unchecked" } )
public final class AcceptDispatch {

    private AcceptDispatch() {}

    /**
     * Accept a live field value into the accumulator, applying numeric coercion
     * when {@code castType} is set (mirrors {@code AstRenderPrintField.format()}).
     */
    public static void accept( TemplateAccumulator acc, @Nullable Object value,
                                TemplateType type, @Nullable FieldType castType ) {
        if( value == null ) {
            acc.acceptNull( type.getTypeClass() );
            return;
        }
        if( value instanceof TemplateAccumulator subAcc ) {
            acc.accept( subAcc );
            return;
        }
        if( castType != null ) {
            TemplateType effective = new TemplateType( castType.type );
            Class<?> tc = effective.isOptional() ? effective.getActualTypeArguments0().getTypeClass() : effective.getTypeClass();
            dispatchValue( acc, coerce( value, tc ), tc );
        } else {
            dispatchValue( acc, value, value.getClass() );
        }
    }

    /**
     * Accept a literal default value into the accumulator
     * (mirrors {@code AstRenderPrintValue.format()}).
     */
    public static void acceptDefaultValue( TemplateAccumulator acc,
                                            TemplateType type, @Nullable FieldType castType,
                                            String defaultValue ) {
        TemplateType effective = castType != null ? new TemplateType( castType.type ) : type;
        Class<?> tc = effective.isOptional() ? effective.getActualTypeArguments0().getTypeClass() : effective.getTypeClass();
        Object parsed = parseDefault( tc, type, defaultValue );
        if( castType != null && parsed instanceof Number n ) parsed = coerce( n, castType.type );
        if( castType != null ) {
            acc.accept( parsed );
        } else {
            dispatchValue( acc, parsed, parsed != null ? parsed.getClass() : tc );
        }
    }

    private static Object coerce( Object value, Class<?> target ) {
        if( value instanceof Number n ) {
            if( byte.class.equals( target ) || Byte.class.isAssignableFrom( target ) ) return n.byteValue();
            if( short.class.equals( target ) || Short.class.isAssignableFrom( target ) ) return n.shortValue();
            if( int.class.equals( target ) || Integer.class.isAssignableFrom( target ) ) return n.intValue();
            if( long.class.equals( target ) || Long.class.isAssignableFrom( target ) ) return n.longValue();
            if( float.class.equals( target ) || Float.class.isAssignableFrom( target ) ) return n.floatValue();
            if( double.class.equals( target ) || Double.class.isAssignableFrom( target ) ) return n.doubleValue();
        }
        return value;
    }

    private static void dispatchValue( TemplateAccumulator acc, Object value, Class<?> tc ) {
        if( value instanceof TemplateAccumulator subAcc ) {
            acc.accept( subAcc );
            return;
        }
        if( boolean.class.equals( tc ) || Boolean.class.isAssignableFrom( tc ) ) {
            acc.accept( ( Boolean ) value );
        } else if( byte.class.equals( tc ) || Byte.class.isAssignableFrom( tc ) ) {
            acc.accept( ( ( Number ) value ).byteValue() );
        } else if( short.class.equals( tc ) || Short.class.isAssignableFrom( tc ) ) {
            acc.accept( ( ( Number ) value ).shortValue() );
        } else if( int.class.equals( tc ) || Integer.class.isAssignableFrom( tc ) ) {
            acc.accept( ( ( Number ) value ).intValue() );
        } else if( long.class.equals( tc ) || Long.class.isAssignableFrom( tc ) ) {
            acc.accept( ( ( Number ) value ).longValue() );
        } else if( float.class.equals( tc ) || Float.class.isAssignableFrom( tc ) ) {
            acc.accept( ( ( Number ) value ).floatValue() );
        } else if( double.class.equals( tc ) || Double.class.isAssignableFrom( tc ) ) {
            acc.accept( ( ( Number ) value ).doubleValue() );
        } else if( Enum.class.isAssignableFrom( tc ) ) {
            acc.accept( ( Enum<?> ) value );
        } else if( Collection.class.isAssignableFrom( tc ) ) {
            acc.accept( ( Collection<?> ) value );
        } else if( value instanceof DateTime dt ) {
            acc.accept( dt );
        } else if( value instanceof String s ) {
            acc.accept( s );
        } else {
            acc.accept( value );
        }
    }

    private static Object parseDefault( Class<?> tc, TemplateType type, String defaultValue ) {
        if( String.class.equals( tc ) ) return defaultValue;
        if( byte.class.equals( tc ) || Byte.class.isAssignableFrom( tc ) )
            return Byte.parseByte( defaultValue );
        if( short.class.equals( tc ) || Short.class.isAssignableFrom( tc ) )
            return Short.parseShort( defaultValue );
        if( int.class.equals( tc ) || Integer.class.isAssignableFrom( tc ) )
            return Integer.parseInt( defaultValue );
        if( long.class.equals( tc ) || Long.class.isAssignableFrom( tc ) )
            return Long.parseLong( defaultValue );
        if( float.class.equals( tc ) || Float.class.isAssignableFrom( tc ) )
            return Float.parseFloat( defaultValue );
        if( double.class.equals( tc ) || Double.class.isAssignableFrom( tc ) )
            return Double.parseDouble( defaultValue );
        if( boolean.class.equals( tc ) || Boolean.class.isAssignableFrom( tc ) )
            return Boolean.parseBoolean( defaultValue );
        if( Collection.class.isAssignableFrom( tc ) ) return List.of();
        if( Enum.class.isAssignableFrom( tc ) ) {
            Class<?> enumClass = ( tc == Enum.class ) ? type.getTypeClass() : tc;
            String name = defaultValue.isEmpty() ? Strings.UNKNOWN : defaultValue;
            return Enum.valueOf( ( Class<Enum> ) enumClass, name );
        }
        if( Object.class.equals( tc ) ) {
            if( "true".equals( defaultValue ) || "false".equals( defaultValue ) ) return Boolean.parseBoolean( defaultValue );
            try {
                return Long.parseLong( defaultValue );
            } catch( NumberFormatException ignored ) { }
            try {
                return Double.parseDouble( defaultValue );
            } catch( NumberFormatException ignored ) { }
            return defaultValue;
        }
        if( DateTime.class.equals( tc ) )
            return Dates.PARSER_MULTIPLE_DATETIME.parseDateTime( defaultValue );
        return defaultValue;
    }

    /**
     * Convert a live value to its String representation using a temporary
     * {@link TemplateAccumulatorString}. Used by function nodes when the first
     * parameter expects {@code String} but the current value is not a String.
     */
    public static String toStringViaAcc( Object value, TemplateType type, TemplateAccumulator parentAcc ) {
        TemplateAccumulatorString tmp = new TemplateAccumulatorString();
        accept( tmp, value, type, null );
        return tmp.get();
    }
}
