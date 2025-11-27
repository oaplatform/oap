package oap.application.remote;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayOutputStream;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class KryoConsts {
    public static Kryo kryo;

    static {
        kryo = new Kryo();
        kryo.setRegistrationRequired( false );
        kryo.setReferences( true );
    }

    public static byte[] writeClassAndObject( Object obj ) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try( Output output = new Output( byteArrayOutputStream ) ) {
            kryo.writeClassAndObject( output, obj );
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static Object readClassAndObject( byte[] bytes ) {
        Input input = new Input( bytes );

        return kryo.readClassAndObject( input );
    }
}
