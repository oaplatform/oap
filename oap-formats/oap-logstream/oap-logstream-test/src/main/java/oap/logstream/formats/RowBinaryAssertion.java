package oap.logstream.formats;

import lombok.SneakyThrows;
import lombok.ToString;
import oap.io.Files;
import oap.io.IoStreams;
import oap.logstream.formats.rowbinary.RowBinaryInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static oap.io.content.ContentReader.ofBytes;
import static org.assertj.core.api.Assertions.assertThat;

public class RowBinaryAssertion extends AbstractAssert<RowBinaryAssertion, RowBinaryAssertion.RowBinaryData> {
    protected RowBinaryAssertion( RowBinaryData rowBinaryData ) {
        super( rowBinaryData, RowBinaryAssertion.class );
    }

    public static RowBinaryAssertion assertRowBinaryFile( Path file, byte[][] types, IoStreams.Encoding encoding ) {
        return assertRowBinaryFile( file, null, types, encoding );
    }

    public static RowBinaryAssertion assertRowBinaryFile( Path file, @Nullable String[] headers, byte[][] types, IoStreams.Encoding encoding ) {
        Assertions.assertThatPath( file ).exists();

        return new RowBinaryAssertion( new RowBinaryData( headers, types, Files.read( file, encoding, ofBytes() ) ) );
    }

    @SneakyThrows
    public ListAssert<List<Object>> content( String... header ) {
        List<List<Object>> ret = new ArrayList<>();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( this.actual.data );
        RowBinaryInputStream rowBinaryInputStream = new RowBinaryInputStream( byteArrayInputStream, this.actual.headers == null, this.actual.headers, this.actual.types );

        List<Object> objects;
        while( ( objects = rowBinaryInputStream.readRow() ) != null ) {
            ArrayList<Object> filtered = new ArrayList<>();
            for( int i = 0; i < rowBinaryInputStream.headers.length; i++ ) {
                if( header.length == 0 || ArrayUtils.contains( rowBinaryInputStream.headers, header[i] ) ) {
                    filtered.add( objects.get( i ) );
                }
            }


            ret.add( filtered );
        }

        return assertThat( ret );
    }

    @ToString
    public static class RowBinaryData {
        public final String[] headers;
        public final byte[][] types;
        public final byte[] data;

        public RowBinaryData( String[] headers, byte[][] types, byte[] data ) {
            this.headers = headers;
            this.types = types;
            this.data = data;
        }
    }
}
