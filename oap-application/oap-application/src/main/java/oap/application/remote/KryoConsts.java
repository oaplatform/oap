package oap.application.remote;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.SerializingInstantiatorStrategy;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentLinkedDeque;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class KryoConsts {
    private static final ConcurrentLinkedDeque<Kryo> queue = new ConcurrentLinkedDeque<>();

    private static Kryo newInstance() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired( false );
        kryo.setReferences( true );
        kryo.setInstantiatorStrategy( new DefaultInstantiatorStrategy( new SerializingInstantiatorStrategy() ) );

        return kryo;
    }

    public static byte[] writeClassAndObject( Object obj ) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try( Output output = new Output( byteArrayOutputStream ) ) {
            writeClassAndObject( output, obj );
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static void writeClassAndObject( Output output, Object obj ) {
        Kryo kryo = queue.poll();
        if( kryo == null ) {
            kryo = newInstance();
        }
        try {
            kryo.writeClassAndObject( output, obj );
        } finally {
            queue.push( kryo );
        }
    }

    public static Object readClassAndObject( byte[] bytes ) {
        Input input = new Input( bytes );

        return readClassAndObject( input );
    }

    public static Object readClassAndObject( Input input ) {
        Kryo kryo = queue.poll();
        if( kryo == null ) {
            kryo = newInstance();
        }
        try {
            return kryo.readClassAndObject( input );
        } finally {
            queue.push( kryo );
        }

    }
}
