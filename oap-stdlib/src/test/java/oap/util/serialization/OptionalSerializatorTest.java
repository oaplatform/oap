package oap.util.serialization;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OptionalSerializatorTest {

    @EqualsAndHashCode
    @Builder
    public static class MubatGolron implements Serializable {
        @Serial
        private static final long serialVersionUID = 3905684370416034790L;

        private Optional<String> field = Optional.of( "value" );
    }

    @EqualsAndHashCode
    @Builder
    public static class AlvatRellin implements Serializable, OptionalSerializator {
        @Serial
        private static final long serialVersionUID = 3905684370416034790L;

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
        private static final long serialVersionUID = 6505621370410634270L;

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
        private static final long serialVersionUID = 3905684370416034790L;

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

    @Test
    public void serializeDeserilizeBad() throws Exception {
        MubatGolron mubatGolron = MubatGolron.builder()
                .field( Optional.of( "value 4" ) )
                .build();

        assertThatThrownBy( () -> serializeDeserialize( mubatGolron ) )
                .isInstanceOf( NotSerializableException.class )
                .hasMessageContaining( "java.util.Optional" );
    }

    @Test
    public void serializeDeserilizeOk() throws Exception {
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
    public void serializeDeserilizeInheritanceOk() throws Exception {
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

    private <V> V  serializeDeserialize( V value ) throws IOException, ClassNotFoundException {
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
