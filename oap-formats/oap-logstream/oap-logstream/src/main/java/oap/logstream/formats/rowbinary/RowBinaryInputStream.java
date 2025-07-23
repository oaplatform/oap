package oap.logstream.formats.rowbinary;

import com.google.common.base.Preconditions;
import oap.template.Types;
import org.joda.time.DateTime;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.joda.time.DateTimeZone.UTC;

/**
 * https://clickhouse.com/docs/interfaces/formats/RowBinary
 */
public class RowBinaryInputStream extends InputStream {
    private final InputStream in;
    public final String[] headers;
    private final byte[][] types;
    protected byte[] readBuffer = new byte[8];

    public RowBinaryInputStream( InputStream in, boolean readHeaders ) throws IOException {
        this( in, readHeaders, null, null );
    }

    public RowBinaryInputStream( InputStream in, String[] headers, byte[][] types ) throws IOException {
        this( in, false, headers, types );
    }

    protected RowBinaryInputStream( InputStream in, boolean readHeaders, String[] headers, byte[][] types ) throws IOException {
        this.in = in;

        if( readHeaders ) {
            int count = readVarInt();
            this.headers = new String[count];
            for( int i = 0; i < count; i++ ) {
                this.headers[i] = readString();
            }
        } else if( types != null ) {
            this.headers = headers;
        } else {
            throw new IllegalArgumentException( "unknown headers" );
        }

        this.types = types;
    }

    public byte readByte() throws IOException {
        return ( byte ) in.read();
    }

    public boolean readBoolean() throws IOException {
        return readByte() == 1;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    public short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if( ( ch1 | ch2 ) < 0 )
            throw new EOFException();
        return ( short ) ( ( ch2 << 8 ) + ( ch1 << 0 ) );
    }

    public int readInt() throws IOException {
        readFully( readBuffer, 0, 4 );

        return ( ( ( 0xFF & readBuffer[0] ) ) << 0 )
            + ( ( ( 0xFF & readBuffer[1] ) ) << 8 )
            + ( ( ( 0xFF & readBuffer[2] ) ) << 16 )
            + ( ( ( 0xFF & readBuffer[3] ) ) << 24 );
    }

    public long readLong() throws IOException {
        readFully( readBuffer, 0, 8 );

        return ( ( readBuffer[0] & 0xFFL ) << 0 )
            + ( ( readBuffer[1] & 0xFFL ) << 8 )
            + ( ( readBuffer[2] & 0xFFL ) << 16 )
            + ( ( readBuffer[3] & 0xFFL ) << 24 )
            + ( ( readBuffer[4] & 0xFFL ) << 32 )
            + ( ( readBuffer[5] & 0xFFL ) << 40 )
            + ( ( readBuffer[6] & 0xFFL ) << 48 )
            + ( ( readBuffer[7] & 0xFFL ) << 56 );
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat( readInt() );
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble( readLong() );
    }

    public DateTime readDateTime() throws IOException {
        return new DateTime( readInt() * 1000L, UTC );
    }

    public Date readDate() throws IOException {
        return new Date( readShort() * 24L * 60L * 60L * 1000L );
    }

    public String readString() throws IOException {
        int length = readVarInt();
        if( length == 0 ) {
            return "";
        } else {
            byte[] buf = new byte[length];
            readFully( buf, 0, length );
            return new String( buf, UTF_8 );
        }
    }

    public <T> List<T> readList( Class<T> clazz ) throws IOException {
        int size = readVarInt();

        ArrayList<T> list = new ArrayList<>( size );

        for( int i = 0; i < size; i++ ) {
            T v = readObject( clazz );
            list.add( v );
        }

        return list;
    }

    @SuppressWarnings( "unchecked" )
    private <T> T readObject( Class<T> clazz ) throws IOException {
        if( clazz == String.class ) {
            return ( T ) readString();
        } else if( clazz == byte.class ) {
            return ( T ) ( Byte ) readByte();
        } else if( clazz == Byte.class ) {
            return ( T ) ( Byte ) readByte();
        } else if( clazz == short.class ) {
            return ( T ) ( Short ) readShort();
        } else if( clazz == Short.class ) {
            return ( T ) ( Short ) readShort();
        } else if( clazz == int.class ) {
            return ( T ) ( Integer ) readInt();
        } else if( clazz == Integer.class ) {
            return ( T ) ( Integer ) readInt();
        } else if( clazz == long.class ) {
            return ( T ) ( Long ) readLong();
        } else if( clazz == Long.class ) {
            return ( T ) ( Long ) readLong();
        } else if( clazz == float.class ) {
            return ( T ) ( Float ) readFloat();
        } else if( clazz == Float.class ) {
            return ( T ) ( Float ) readFloat();
        } else if( clazz == double.class ) {
            return ( T ) ( Double ) readDouble();
        } else if( clazz == Double.class ) {
            return ( T ) ( Double ) readDouble();
        } else if( clazz == DateTime.class ) {
            return ( T ) readDateTime();
        } else if( clazz == Date.class ) {
            return ( T ) readDate();
        } else {
            throw new IllegalArgumentException( "unknown class " + clazz );
        }
    }

    protected void readFully( byte[] b, int off, int len ) throws IOException {
        int n = 0;
        while( n < len ) {
            int count = in.read( b, off + n, len - n );
            if( count < 0 )
                throw new EOFException();
            n += count;
        }
    }

    protected int readVarInt() throws IOException {
        int value = 0;

        for( int i = 0; i < 10; i++ ) {
            byte b = readByteOrEof();
            value |= ( b & 0x7F ) << ( 7 * i );

            if( ( b & 0x80 ) == 0 ) {
                break;
            }
        }

        return value;
    }

    private byte readByteOrEof() throws IOException {
        int b = in.read();
        if( b < 0 ) {
            throw new EOFException( "End of stream reached before reading all data" );
        }
        return ( byte ) b;
    }

    public List<Object> readRow() throws IOException {
        try {
            Preconditions.checkNotNull( types );
            Preconditions.checkNotNull( headers );

            ArrayList<Object> row = new ArrayList<>( headers.length );

            for( int i = 0; i < headers.length; i++ ) {
                String header = headers[i];
                byte[] bytes = types[i];
                Types types = Types.valueOf( bytes[0] );

                row.add( switch( types ) {
                    case DATETIME -> readDateTime();
                    case DATE -> readDate();
                    case BYTE -> readByte();
                    case SHORT -> readShort();
                    case INTEGER -> readInt();
                    case LONG -> readLong();
                    case FLOAT -> readFloat();
                    case DOUBLE -> readDouble();
                    case STRING -> readString();
                    case BOOLEAN -> readBoolean();
                    case LIST -> {
                        Types listItemType = Types.valueOf( bytes[1] );
                        yield readList( listItemType.clazz );
                    }
                    default -> throw new IllegalArgumentException( "unknown type " + types );
                } );
            }

            return row;
        } catch( EOFException e ) {
            return null;
        }
    }
}
