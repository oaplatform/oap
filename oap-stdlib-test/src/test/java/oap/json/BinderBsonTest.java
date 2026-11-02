package oap.json;

import oap.util.FastByteArrayOutputStream;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.Serial;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

public class BinderBsonTest {
    @Test
    public void binaryUrlInput() throws java.io.IOException {
        var file = java.nio.file.Files.createTempFile( "binder", ".bson" );
        try {
            Bean expected = new Bean( "binary", 123, null );
            Binder.bson.marshal( file, expected );
            assertThat( Binder.bson.unmarshal( Bean.class, file.toUri().toURL() ) ).isEqualTo( expected );
        } finally {
            java.nio.file.Files.delete( file );
        }
    }

    @Test
    public void testBson() {
        Bean obj = new Bean( "1", 1, null );
        Bean clone = Binder.bson.clone( obj );
        assertThat( clone ).isEqualTo( obj );

        ByteArrayOutputStream streamBson = new ByteArrayOutputStream();
        Binder.bson.marshal( obj, streamBson );

        ByteArrayOutputStream streamJson = new ByteArrayOutputStream();
        Binder.json.marshal( obj, streamJson );
        assertThat( streamBson.size() ).isNotEqualTo( streamJson.size() );
    }

    @Test
    public void testMigration() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        Binder.bson.marshal( new A( "a", 1 ), stream );

        B b = Binder.bson.unmarshal( B.class, stream.getInputStream() );

        assertThat( b.a ).isEqualTo( "a" );
        assertThat( b.c ).isNull();
    }

    public static class A implements Serializable {
        @Serial
        private static final long serialVersionUID = -5515951683059792285L;

        public String a;
        public int b;

        public A( String a, int b ) {
            this.a = a;
            this.b = b;
        }
    }

    public static class B implements Serializable {
        @Serial
        private static final long serialVersionUID = -5515951683059792285L;

        public String a;
        public String c;

        public B( String a, String c ) {
            this.a = a;
            this.c = c;
        }
    }
}
