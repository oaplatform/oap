package oap.application.remote;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyMapSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptySetSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonListSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonMapSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonSetSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ArrayTableSerializer;
import de.javakaffee.kryoserializers.guava.HashBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableTableSerializer;
import de.javakaffee.kryoserializers.guava.LinkedHashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ReverseListSerializer;
import de.javakaffee.kryoserializers.guava.TreeBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.TreeMultimapSerializer;
import de.javakaffee.kryoserializers.guava.UnmodifiableNavigableSetSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaDateTimeSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalDateSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalDateTimeSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalTimeSerializer;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.objenesis.strategy.SerializingInstantiatorStrategy;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentLinkedDeque;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class KryoConsts {
    private static final ConcurrentLinkedDeque<Kryo> queue = new ConcurrentLinkedDeque<>();

    private static Kryo newInstance() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired( false );
        kryo.setReferences( true );
        kryo.setInstantiatorStrategy( new DefaultInstantiatorStrategy( new SerializingInstantiatorStrategy() ) );


        // joda
        kryo.register( DateTime.class, new JodaDateTimeSerializer() );
        kryo.register( LocalDate.class, new JodaLocalDateSerializer() );
        kryo.register( LocalDateTime.class, new JodaLocalDateTimeSerializer() );
        kryo.register( LocalDateTime.class, new JodaLocalTimeSerializer() );

        // java
        kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );
        kryo.register( Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer() );
        kryo.register( Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer() );
        kryo.register( Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer() );
        kryo.register( Collections.singletonList( "" ).getClass(), new CollectionsSingletonListSerializer() );
        kryo.register( Collections.singleton( "" ).getClass(), new CollectionsSingletonSetSerializer() );
        kryo.register( Collections.singletonMap( "", "" ).getClass(), new CollectionsSingletonMapSerializer() );
        kryo.register( GregorianCalendar.class, new GregorianCalendarSerializer() );
        kryo.register( InvocationHandler.class, new JdkProxySerializer() );
        UnmodifiableCollectionsSerializer.registerSerializers( kryo );
        SynchronizedCollectionsSerializer.registerSerializers( kryo );

        // guava
        ImmutableListSerializer.registerSerializers( kryo );
        ImmutableSetSerializer.registerSerializers( kryo );
        ImmutableMapSerializer.registerSerializers( kryo );
        ImmutableMultimapSerializer.registerSerializers( kryo );
        ImmutableTableSerializer.registerSerializers( kryo );
        ReverseListSerializer.registerSerializers( kryo );
        UnmodifiableNavigableSetSerializer.registerSerializers( kryo );

        ArrayListMultimapSerializer.registerSerializers( kryo );
        HashMultimapSerializer.registerSerializers( kryo );
        LinkedHashMultimapSerializer.registerSerializers( kryo );
        LinkedListMultimapSerializer.registerSerializers( kryo );
        TreeMultimapSerializer.registerSerializers( kryo );
        ArrayTableSerializer.registerSerializers( kryo );
        HashBasedTableSerializer.registerSerializers( kryo );
        TreeBasedTableSerializer.registerSerializers( kryo );

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
