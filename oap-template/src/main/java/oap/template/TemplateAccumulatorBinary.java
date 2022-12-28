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

import lombok.SneakyThrows;
import oap.util.FastByteArrayOutputStream;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Date;

import static org.joda.time.DateTimeZone.UTC;

public class TemplateAccumulatorBinary implements TemplateAccumulator<byte[], FastByteArrayOutputStream, TemplateAccumulatorBinary> {
    public static final byte[] BYTES = new byte[0];
    protected final FastByteArrayOutputStream baos;
    private final BinaryOutputStream bos;

    public TemplateAccumulatorBinary( FastByteArrayOutputStream baos ) {
        this.baos = baos;
        this.bos = new BinaryOutputStream( baos );
    }

    public TemplateAccumulatorBinary() {
        this( new FastByteArrayOutputStream() );
    }

    @SneakyThrows
    @Override
    public void acceptText( String text ) {
        bos.writeString( text );
    }

    @SneakyThrows
    @Override
    public void accept( String text ) {
        bos.writeString( text );
    }

    @SneakyThrows
    @Override
    public void accept( boolean b ) {
        bos.writeBoolean( b );
    }

    @SneakyThrows
    @Override
    public void accept( byte b ) {
        bos.writeByte( b );
    }

    @SneakyThrows
    @Override
    public void accept( short s ) {
        bos.writeShort( s );
    }

    @SneakyThrows
    @Override
    public void accept( int i ) {
        bos.writeInt( i );
    }

    @SneakyThrows
    @Override
    public void accept( long l ) {
        bos.writeLong( l );
    }

    @SneakyThrows
    @Override
    public void accept( float f ) {
        bos.writeFloat( f );
    }

    @SneakyThrows
    @Override
    public void accept( double d ) {
        bos.writeDouble( d );
    }

    @SneakyThrows
    @Override
    public void accept( DateTime jodaDateTime ) {
        bos.writeDateTime( jodaDateTime );
    }

    @SneakyThrows
    @Override
    public void accept( Date javaDate ) {
        bos.writeDateTime( new DateTime( javaDate, UTC ) );
    }

    @SneakyThrows
    @Override
    public void accept( Enum<?> e ) {
        bos.writeString( e.name() );
    }

    @SneakyThrows
    @Override
    public void accept( Collection<?> list ) {
        bos.writeList( list );
    }

    @SneakyThrows
    @Override
    public void accept( TemplateAccumulatorBinary acc ) {
        FastByteArrayOutputStream baos = acc.baos;

        bos.write( baos.array, 0, baos.length );
    }

    @Override
    public void accept( Object obj ) {
        if( obj instanceof String s ) accept( s );
        else if( obj instanceof Byte b ) accept( b );
        else if( obj instanceof Short s ) accept( s );
        else if( obj instanceof Integer i ) accept( i );
        else if( obj instanceof Long l ) accept( l );
        else if( obj instanceof Float f ) accept( f );
        else if( obj instanceof Double d ) accept( d );
        else if( obj instanceof Enum<?> e ) accept( e );
        else if( obj instanceof DateTime dt ) accept( dt );
        else if( obj instanceof Date d ) accept( d );
        else if( obj instanceof Collection<?> c ) accept( c );
        else if( obj instanceof TemplateAccumulatorBinary tab ) accept( tab );

        throw new IllegalArgumentException( "Unknown type " + obj.getClass() );
    }

    @Override
    public boolean isEmpty() {
        return baos.length == 0;
    }

    @Override
    public TemplateAccumulatorBinary newInstance() {
        return new TemplateAccumulatorBinary();
    }

    @Override
    public TemplateAccumulatorBinary newInstance( FastByteArrayOutputStream mutable ) {
        return new TemplateAccumulatorBinary( mutable );
    }

    @Override
    public String getTypeName() {
        return "byte[]";
    }

    @Override
    public String delimiter() {
        return "";
    }

    @SneakyThrows
    @Override
    public TemplateAccumulatorBinary addEol( boolean eol ) {
        if( eol ) bos.write( Types.EOL.id );
        return this;
    }

    @Override
    public void reset() {
        baos.reset();
    }

    @SneakyThrows
    @Override
    public byte[] get() {
        return baos.toByteArray();
    }

    @Override
    public byte[] getBytes() {
        return get();
    }
}
