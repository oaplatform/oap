package oap.application.remote;

import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class FstConsts {
    private static final ConcurrentLinkedDeque<FSTConfiguration> queue = new ConcurrentLinkedDeque<>();

    private static FSTConfiguration newInstance() {
        FSTConfiguration configuration = FSTConfiguration.createDefaultConfiguration();

        configuration.registerClass( RemoteInvocation.class );
        configuration.registerSerializer( Optional.class, new FSTOptionalSerializer(), false );

        return configuration;
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T readObject( DataInputStream dis, int size ) throws IOException {
        byte[] bytes = new byte[size];
        dis.readFully( bytes );

        return ( T ) asObject( bytes );
    }

    public static Object asObject( byte[] bytes ) throws IOException {
        FSTConfiguration configuration = queue.poll();
        if( configuration == null ) {
            configuration = newInstance();
        }
        try {
            return configuration.asObject( bytes );
        } finally {
            queue.push( configuration );
        }
    }

    public static <T> T readObjectWithSize( DataInputStream is ) throws IOException {
        int size = is.readInt();
        return readObject( is, size );
    }

    public static void writeObjectWithSize( DataOutputStream dataOutputStream, Object obj ) throws IOException {
        byte[] sv = asByteArray( obj );
        dataOutputStream.writeInt( sv.length );
        dataOutputStream.write( sv );
    }

    public static byte[] asByteArray( Object obj ) {
        FSTConfiguration configuration = queue.poll();
        if( configuration == null ) {
            configuration = newInstance();
        }
        try {
            return configuration.asByteArray( obj );
        } finally {
            queue.push( configuration );
        }
    }

    private static class FSTOptionalSerializer extends FSTBasicObjectSerializer {
        @Override
        public void writeObject( FSTObjectOutput out, Object o, FSTClazzInfo fstClazzInfo, FSTClazzInfo.FSTFieldInfo fstFieldInfo, int i ) throws IOException {
            Optional<?> opt = ( Optional<?> ) o;
            if( opt.isPresent() ) out.writeObject( opt.get() );
            else out.writeObject( null );
        }

        @Override
        public void readObject( FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy ) throws Exception {
        }

        @Override
        public Object instantiate( Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition ) throws Exception {
            return Optional.ofNullable( in.readObject() );
        }
    }
}
