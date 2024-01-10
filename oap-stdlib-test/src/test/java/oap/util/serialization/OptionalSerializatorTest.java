package oap.util.serialization;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import oap.reflect.Reflection;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OptionalSerializatorTest {

    @EqualsAndHashCode
    @Builder
    public static class MubatGolron implements Serializable {
        @Serial
        private static final long serialVersionUID = 12345;

        private Optional<String> field = Optional.of( "value" );
    }

    @EqualsAndHashCode
    @Builder
    public static class AlvatRellin implements Serializable, OptionalSerializator {
        @Serial
        private static final long serialVersionUID = 12345;

        private static String field1 = "value1";
        private Serializable field2 = "value2";
        private transient String field3 = "value3";
        private Optional<String> field4 = Optional.of( "value4" );
        private String field5 = "value5";

        private void writeObject( ObjectOutputStream oos ) throws IOException {
            writeObjectTemplate( oos );
        }

        private void readObject( ObjectInputStream ois ) throws IOException, ClassNotFoundException {
            readObjectTemplate( ois );
        }
    }

    @EqualsAndHashCode
    public static class Person implements Serializable, OptionalSerializator {
        @Serial
        private static final long serialVersionUID = 12345;

        protected Serializable field2 = "value2";
        protected transient String field3 = "value3";
        protected Optional<String> field4 = Optional.of( "value4" );
        protected String field5 = "value5";

        private void writeObject( ObjectOutputStream oos ) throws IOException {
            writeObjectTemplate( oos );
        }

        private void readObject( ObjectInputStream ois ) throws IOException, ClassNotFoundException {
            readObjectTemplate( ois );
        }
    }

    @EqualsAndHashCode
    public static class OgarPeltew extends Person implements Serializable, OptionalSerializator {
        @Serial
        private static final long serialVersionUID = 12345;

        private Serializable field21 = "value2";
        private transient String field31 = "value3";
        private Optional<String> field41 = Optional.of( "value4" );
        private String field51 = "value5";

        public OgarPeltew( Person person ) {
            super();
        }

        private void writeObject( ObjectOutputStream oos ) throws IOException {
            writeObjectTemplate( oos );
        }

        private void readObject( ObjectInputStream ois ) throws IOException, ClassNotFoundException {
            readObjectTemplate( ois );
        }
    }

    /**
     * This class is an example how to work with collections like Map<String, Optional<String>>.
     * Note: first of all we process with this map, then with all other fields which are not required non-standard procedure.
     */
    public static class PerditaDurango implements Serializable, OptionalSerializator {
        @Serial
        private static final long serialVersionUID = 12345;
        private Map<String, Optional<String>> cons = new HashMap();
        private String field1 = "value1";

        public boolean fieldShouldBeSkipped( Reflection.Field field ) {
            return field.isTransient() || field.isStatic() || field.name().equals( "cons" );
        }

        private void writeObject( @NotNull ObjectOutputStream oos ) throws IOException {
            Map<String, String> copy = new HashMap<>();
            this.cons.forEach( ( key, value ) -> copy.put( key, value.orElse( null ) ) );
            oos.writeObject( copy );
            writeObjectTemplate( oos ); // it's important to write other fields AFTER manually processed
            oos.flush();
            oos.close();
        }

        private void readObject( @NotNull ObjectInputStream ois ) throws IOException, ClassNotFoundException {
            Map<String, String> copy = ( Map ) ois.readObject();
            Map<String, Optional<String>> original = new HashMap<>();
            copy.forEach( ( key, value ) -> original.put( key, Optional.ofNullable( value ) ) );
            this.cons = original;
            readObjectTemplate( ois ); // it's important to read other fields AFTER manually processed
            ois.close();
        }
    }

    @Test
    public void serializeDeserializeBad() throws Exception {
        MubatGolron mubatGolron = MubatGolron.builder()
                .field( Optional.of( "value 4" ) )
                .build();

        assertThatThrownBy( () -> serializeDeserialize( mubatGolron ) )
                .isInstanceOf( NotSerializableException.class )
                .hasMessageContaining( "java.util.Optional" );
    }

    @Test
    public void serializeDeserializeOk() throws Exception {
        AlvatRellin alvatRellin = AlvatRellin.builder()
                .field3( "value 3" )
                .field4( Optional.of( "value 4" ) )
                .field5( "value 5" )
                .build();

        AlvatRellin clone = serializeDeserialize( alvatRellin );

        assertThat( clone ).isNotNull();
        assertThat( clone.field2 ).isEqualTo( alvatRellin.field2 );
        assertThat( clone.field3 ).isNull();
        assertThat( clone.field4 ).isEqualTo( alvatRellin.field4 );
        assertThat( clone.field4 ).isInstanceOf( Optional.class );
        assertThat( clone.field5 ).isEqualTo( alvatRellin.field5 );
    }

    @Test
    public void serializeDeserializeInheritanceOk() throws Exception {
        OgarPeltew ogarPeltew = new OgarPeltew( new Person() );
        ogarPeltew.field31 = "value 3";
        ogarPeltew.field41 = Optional.of( "value 4" );
        ogarPeltew.field51 = "value 5";

        OgarPeltew clone = serializeDeserialize( ogarPeltew );

        assertThat( clone ).isNotNull();
        assertThat( clone.field31 ).isNull();
        assertThat( clone.field41 ).isEqualTo( ogarPeltew.field41 );
        assertThat( clone.field41 ).isInstanceOf( Optional.class );
        assertThat( clone.field51 ).isEqualTo( ogarPeltew.field51 );
    }

    @Test
    public void serializationExtOk() throws IOException, ClassNotFoundException {
        PerditaDurango perditaDurango = new PerditaDurango();
        perditaDurango.cons.put( "claws", Optional.of( "1978" ) );
        perditaDurango.cons.put( "to thaw", Optional.empty() );
        perditaDurango.field1 = "value 1";

        PerditaDurango clone = serializeDeserialize( perditaDurango );

        assertThat( clone ).isNotNull();
        assertThat( clone.cons.get( "claws" ) ).isEqualTo( Optional.of( "1978" ) );
        assertThat( clone.cons.get( "to thaw" ) ).isEqualTo( Optional.empty() );
        assertThat( clone.field1 ).isEqualTo( "value 1" );
    }

    private <V> V serializeDeserialize( V value ) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream( outputStream );
        objectOutputStream.writeObject( value );
        objectOutputStream.flush();
        objectOutputStream.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream( outputStream.toByteArray() );
        ObjectInputStream objectInputStream = new ObjectInputStream( inputStream );
        V result = ( V ) objectInputStream.readObject();
        objectInputStream.close();
        return result;
    }
}
