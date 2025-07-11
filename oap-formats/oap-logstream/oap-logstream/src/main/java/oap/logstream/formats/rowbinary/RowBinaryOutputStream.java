package oap.logstream.formats.rowbinary;

import oap.dictionary.Dictionary;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.joda.time.DateTimeZone.UTC;

/**
 * https://clickhouse.com/docs/interfaces/formats/RowBinary
 */
public class RowBinaryOutputStream extends OutputStream {
    protected final byte[] writeBuffer = new byte[9];

    private final OutputStream out;

    public RowBinaryOutputStream( OutputStream out ) {
        this.out = out;
    }

    public RowBinaryOutputStream( OutputStream out, List<String> headers ) throws IOException {
        this( out );

        writeVarInt( headers.size() );
        for( String header : headers ) {
            writeString( header );
        }
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private void writeVarInt( long value ) throws IOException {
        for( int i = 0; i < 9; i++ ) {
            byte b = ( byte ) ( value & 0x7F );

            if( value > 0x7F ) {
                b |= 0x80;
            }

            value >>= 7;
            out.write( b );

            if( value == 0 ) {
                return;
            }
        }
    }

    @Override
    public void write( int b ) throws IOException {
        out.write( b );
    }

    @Override
    public void write( byte[] b ) throws IOException {
        out.write( b );
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException {
        out.write( b, off, len );
    }

    public void writeByte( byte v ) throws IOException {
        out.write( v );
    }

    public void writeBoolean( boolean v ) throws IOException {
        out.write( v ? 1 : 0 );
    }

    public void writeShort( short s ) throws IOException {
        writeBuffer[0] = ( byte ) ( s >>> 0 );
        writeBuffer[1] = ( byte ) ( s >>> 8 );

        out.write( writeBuffer, 0, 2 );
    }

    public void writeInt( int v ) throws IOException {
        writeBuffer[0] = ( byte ) ( 0xFF & ( v >> 0 ) );
        writeBuffer[1] = ( byte ) ( 0xFF & ( v >> 8 ) );
        writeBuffer[2] = ( byte ) ( 0xFF & ( v >> 16 ) );
        writeBuffer[3] = ( byte ) ( 0xFF & ( v >> 24 ) );

        out.write( writeBuffer, 0, 4 );
    }

    public void writeLong( long v ) throws IOException {
        writeBuffer[0] = ( byte ) ( 0xFF & ( v >> 0 ) );
        writeBuffer[1] = ( byte ) ( 0xFF & ( v >> 8 ) );
        writeBuffer[2] = ( byte ) ( 0xFF & ( v >> 16 ) );
        writeBuffer[3] = ( byte ) ( 0xFF & ( v >> 24 ) );
        writeBuffer[4] = ( byte ) ( 0xFF & ( v >> 32 ) );
        writeBuffer[5] = ( byte ) ( 0xFF & ( v >> 40 ) );
        writeBuffer[6] = ( byte ) ( 0xFF & ( v >> 48 ) );
        writeBuffer[7] = ( byte ) ( 0xFF & ( v >> 56 ) );

        out.write( writeBuffer, 0, 8 );
    }

    public void writeFloat( float f ) throws IOException {
        writeInt( Float.floatToIntBits( f ) );
    }

    public void writeDouble( double d ) throws IOException {
        writeLong( Double.doubleToLongBits( d ) );
    }

    public void writeDateTime( DateTime jodaDateTime ) throws IOException {
        writeInt( ( int ) ( jodaDateTime.getMillis() / 1000 ) );
    }

    public void writeDate( Date javaDate ) throws IOException {
        writeShort( ( short ) ( javaDate.toInstant().toEpochMilli() / 1000 / 60 / 60 / 24 ) );
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public void writeString( String s ) throws IOException {
        if( s.isEmpty() ) {
            out.write( 0 );
        } else {
            byte[] bytes = s.getBytes( UTF_8 );
            writeVarInt( bytes.length );
            out.write( bytes );
        }
    }

    public void writeList( Collection<?> list ) throws IOException {
        writeVarInt( list.size() );
        for( Object o : list ) {
            writeObject( o );
        }
    }

    public void writeObject( Object v ) throws IOException {
        switch( v ) {
            case String s -> writeString( s );
            case Boolean b -> writeBoolean( b );
            case Byte b -> writeByte( b );
            case Short s -> writeShort( s );
            case Integer i -> writeInt( i );
            case Long l -> writeLong( l );
            case Float f -> writeFloat( f );
            case Enum<?> e -> writeString( e.name() );
            case Dictionary d -> writeString( d.getId() );
            case Double d -> writeDouble( d );
            case DateTime dt -> writeDateTime( dt );
            case Date d -> writeDateTime( new DateTime( d, UTC ) );
            case Collection<?> c -> writeList( c );
            case null, default -> throw new IllegalArgumentException( "Unknown type " + v.getClass() );
        }
    }
}
