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

import oap.util.Dates;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Collection;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TemplateAccumulatorString implements TemplateAccumulator<String, StringBuilder, TemplateAccumulatorString> {
    protected final StringBuilder sb;
    private final DateTimeFormatter dateTimeFormat;

    public TemplateAccumulatorString( StringBuilder sb ) {
        this( sb, Dates.PATTERN_FORMAT_DATE );
    }

    public TemplateAccumulatorString() {
        this( new StringBuilder(), Dates.PATTERN_FORMAT_SIMPLE );
    }

    public TemplateAccumulatorString( String dateTimeFormat ) {
        this( new StringBuilder(), dateTimeFormat );
    }

    public TemplateAccumulatorString( StringBuilder sb, String dateTimeFormat ) {
        this.sb = sb;

        this.dateTimeFormat = DateTimeFormat
            .forPattern( dateTimeFormat )
            .withZoneUTC();
    }

    @Override
    public void acceptText( String text ) {
        if( text != null )
            sb.append( text );
    }

    @Override
    public void accept( String text ) {
        if( text != null ) {
            sb.append( text );
        }
    }

    @Override
    public void accept( boolean b ) {
        sb.append( b );
    }

    @Override
    public void accept( byte b ) {
        sb.append( b );
    }

    @Override
    public void accept( short s ) {
        sb.append( s );
    }

    @Override
    public void accept( int i ) {
        sb.append( i );
    }

    @Override
    public void accept( long l ) {
        sb.append( l );
    }

    @Override
    public void accept( float f ) {
        sb.append( f );
    }

    @Override
    public void accept( DateTime jodaDateTime ) {
        sb.append( dateTimeFormat.print( jodaDateTime ) );
    }

    @Override
    public void accept( double d ) {
        sb.append( d );
    }

    @Override
    public void accept( Enum<?> e ) {
        sb.append( e.name() );
    }

    @Override
    public void accept( Collection<?> list ) {
        if( list == null ) {
            sb.append( "[]" );
            return;
        }

        sb.append( '[' );
        boolean first = true;
        for( var item : list ) {
            if( !first ) {
                sb.append( ',' );
            } else {
                first = false;
            }
            if( item instanceof Collection ) {
                accept( ( Collection<?> ) item );
            } else if( item instanceof String s ) {
                acceptStringWithSingleQuote( s );
            } else if( item instanceof Enum<?> e ) {
                acceptStringWithSingleQuote( e.name() );
            } else if( item instanceof DateTime dt ) {
                acceptStringWithSingleQuote( dateTimeFormat.print( dt ) );
            } else {
                accept( item );
            }
        }
        sb.append( ']' );
    }

    @Override
    public void accept( TemplateAccumulatorString acc ) {
        sb.append( acc.sb );
    }

    @Override
    public void accept( Object obj ) {
        if( obj instanceof DateTime dt ) accept( dt );
        else if( obj instanceof Date d ) accept( d );
        else if( obj instanceof Collection<?> c ) accept( c );
        else accept( String.valueOf( obj ) );
    }

    @Override
    public boolean isEmpty() {
        return sb.length() == 0;
    }

    @Override
    public TemplateAccumulatorString newInstance() {
        return new TemplateAccumulatorString();
    }

    @Override
    public TemplateAccumulatorString newInstance( StringBuilder mutable ) {
        return new TemplateAccumulatorString( mutable );
    }

    @Override
    public String getTypeName() {
        return "String";
    }

    @Override
    public String delimiter() {
        return "\t";
    }

    @Override
    public String get() {
        return sb.toString();
    }

    @Override
    public byte[] getBytes() {
        return get().getBytes( UTF_8 );
    }

    public void acceptStringWithSingleQuote( String item ) {
        String escapeItem = item;
        escapeItem = StringUtils.replace( escapeItem, "\\", "\\\\" );
        escapeItem = StringUtils.replace( escapeItem, "'", "\\'" );

        sb.append( '\'' )
            .append( escapeItem )
            .append( '\'' );
    }

    @Override
    public TemplateAccumulatorString addEol( boolean eol ) {
        if( eol ) sb.append( '\n' );
        return this;
    }

    @Override
    public void reset() {
        sb.delete( 0, sb.length() );
    }
}
