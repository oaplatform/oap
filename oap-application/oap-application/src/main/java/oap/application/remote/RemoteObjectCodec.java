package oap.application.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Optional;

public final class RemoteObjectCodec {
    private RemoteObjectCodec() {}

    @SuppressWarnings( "unchecked" )
    public static <T> T readObject( DataInputStream input, int size ) throws IOException {
        if( size < 0 ) throw new IOException( "Negative RPC payload size: " + size );
        byte[] bytes = new byte[size];
        input.readFully( bytes );
        return ( T ) asObject( bytes );
    }

    public static Object asObject( byte[] bytes ) throws IOException {
        try( var input = new ObjectInputStream( new ByteArrayInputStream( bytes ) ) ) {
            return input.readObject();
        } catch( ClassNotFoundException e ) {
            throw new IOException( "Cannot resolve RPC payload class", e );
        }
    }

    public static <T> T readObjectWithSize( DataInputStream input ) throws IOException {
        return readObject( input, input.readInt() );
    }

    public static void writeObjectWithSize( DataOutputStream output, Object value ) throws IOException {
        byte[] bytes = asByteArray( value );
        output.writeInt( bytes.length );
        output.write( bytes );
    }

    public static byte[] asByteArray( Object value ) {
        var bytes = new ByteArrayOutputStream();
        try( var output = new OptionalObjectOutputStream( bytes ) ) {
            output.writeObject( value );
        } catch( IOException e ) {
            throw new UncheckedIOException( "Cannot serialize RPC payload", e );
        }
        return bytes.toByteArray();
    }

    private static final class OptionalObjectOutputStream extends ObjectOutputStream {
        private OptionalObjectOutputStream( ByteArrayOutputStream output ) throws IOException {
            super( output );
            enableReplaceObject( true );
        }

        @Override
        protected Object replaceObject( Object value ) {
            return value instanceof Optional<?> optional ? new OptionalValue( optional.orElse( null ) ) : value;
        }
    }

    private record OptionalValue( Object value ) implements Serializable {
        private Object readResolve() {
            return Optional.ofNullable( value );
        }
    }
}
