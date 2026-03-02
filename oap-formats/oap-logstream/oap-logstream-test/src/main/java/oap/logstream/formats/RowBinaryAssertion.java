package oap.logstream.formats;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import oap.io.Files;
import oap.io.IoStreams;
import oap.logstream.formats.rowbinary.RowBinaryInputStream;
import oap.util.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static oap.io.content.ContentReader.ofBytes;
import static org.assertj.core.api.Assertions.assertThat;

public class RowBinaryAssertion extends AbstractAssert<RowBinaryAssertion, RowBinaryAssertion.RowBinaryData> {
    protected RowBinaryAssertion( RowBinaryData rowBinaryData ) {
        super( rowBinaryData, RowBinaryAssertion.class );
    }

    public static RowBinaryAssertion assertRowBinaryFile( Path file, IoStreams.Encoding encoding ) {
        Assertions.assertThatPath( file ).exists();

        return new RowBinaryAssertion( new RowBinaryData( null, null, new ByteArrayInputStream( Files.read( file, encoding, ofBytes() ) ) ) );
    }

    public static RowBinaryAssertion assertRowBinary( byte[] bytes ) {
        return new RowBinaryAssertion( new RowBinaryData( null, null, new ByteArrayInputStream( bytes ) ) );
    }

    public static Row row( Object... cols ) {
        return new Row( cols );
    }

    public static Header header( String... cols ) {
        return new Header( cols );
    }

    public RowBinaryAssertion hasHeaders( String... headers ) {
        assertThat( actual.headers ).contains( headers );
        return this;
    }

    public RowBinaryAssertion hasHeaders( Iterable<String> headers ) {
        assertThat( actual.headers ).containsAll( headers );
        return this;
    }

    public RowBinaryAssertion hasHeaders( Header header ) {
        assertThat( actual.headers ).containsAll( header.cols );
        return this;
    }

    public RowBinaryAssertion containOnlyHeaders( String... headers ) {
        assertThat( actual.headers ).containsOnly( headers );
        return this;
    }

    @SafeVarargs
    public final RowBinaryAssertion containsExactlyInAnyOrderEntriesOf( List<Object>... entries ) {
        assertThat( actual.data ).containsExactlyInAnyOrderElementsOf( List.of( entries ) );
        return this;
    }

    public RowBinaryAssertion containsExactlyInAnyOrderEntriesOf( Header header, Row... rows ) {
        hasHeaders( header );
        for( Row row : rows ) {
            assertThat( row.cols )
                .withFailMessage( "entries length doesnt match headers" )
                .hasSize( header.size() );
        }
        assertThat( actual.getCols( header ) )
            .containsExactlyInAnyOrderElementsOf( Lists.map( rows, r -> r.cols ) );

        return this;
    }

    public RowBinaryAssertion containsAnyEntriesOf( Header header, Row... rows ) {
        hasHeaders( header.cols );
        for( Row row : rows ) {
            assertThat( row.cols )
                .withFailMessage( "entries length doesnt match headers" )
                .hasSize( header.size() );
        }

        assertThat( actual.getCols( header ) )
            .containsAnyElementsOf( Lists.map( rows, r -> r.cols ) );
        return this;
    }

    public RowBinaryAssertion containsOnlyOnceEntriesOf( Header header, Row... rows ) {
        hasHeaders( header );
        for( Row row : rows ) {
            assertThat( row.cols )
                .withFailMessage( "entries length doesnt match headers" )
                .hasSize( header.size() );
        }
        assertThat( actual.getCols( header ) ).containsOnlyOnceElementsOf( Lists.map( rows, r -> r.cols ) );
        return this;
    }

    public RowBinaryAssertion doesNotContainAnyEntriesOf( Header header, Row... rows ) {
        hasHeaders( header );
        for( Row row : rows ) {
            assertThat( row.cols )
                .withFailMessage( "entries length doesnt match headers" )
                .hasSize( header.size() );
        }

        assertThat( actual.getCols( header ) ).doesNotContainAnyElementsOf( Lists.map( rows, r -> r.cols ) );
        return this;
    }

    public RowBinaryAssertion doesNotContainAnyEntriesOf( Row... rows ) {
        assertThat( actual.headers )
            .withFailMessage( "tsv must contain headers" )
            .isNotEmpty();

        for( Row row : rows ) {
            assertThat( row.cols )
                .withFailMessage( "entries length doesnt match headers" )
                .hasSameSizeAs( actual.headers );
        }
        assertThat( actual.data ).doesNotContainAnyElementsOf( Lists.map( rows, r -> r.cols ) );
        return this;
    }

    public RowBinaryAssertion isNotEmpty() {
        assertThat( actual.data ).isNotEmpty();
        return this;
    }

    @ToString
    public static class RowBinaryData {
        public final String[] headers;
        public final byte[][] types;
        public final ArrayList<List<Object>> data = new ArrayList<>();

        @SneakyThrows
        public RowBinaryData( String[] headers, byte[][] types, InputStream inputStream ) {
            RowBinaryInputStream rowBinaryInputStream = new RowBinaryInputStream( inputStream, headers, types );

            List<Object> objects;
            while( ( objects = rowBinaryInputStream.readRow() ) != null ) {
                ArrayList<Object> row = new ArrayList<>();
                for( int i = 0; i < rowBinaryInputStream.headers.length; i++ ) {
                    row.add( objects.get( i ) );
                }

                data.add( row );
            }

            this.headers = rowBinaryInputStream.headers;
            this.types = rowBinaryInputStream.types;
        }

        public List<List<Object>> getCols( Header headers ) {

            IntArrayList hIndexes = new IntArrayList();

            for( String header : headers.cols ) {
                int index = ArrayUtils.indexOf( this.headers, header );
                Preconditions.checkArgument( index >= 0, "header %s not found", header );
                hIndexes.add( index );
            }

            ArrayList<List<Object>> ret = new ArrayList<>();

            for( List<Object> row : data ) {
                ArrayList<Object> filtered = new ArrayList<>();
                for( int index : hIndexes ) {
                    filtered.add( row.get( index ) );
                }
                ret.add( filtered );
            }

            return ret;
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class Row {
        private final List<Object> cols;

        public Row( Object... cols ) {
            this.cols = List.of( cols );
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class Header {
        public final List<String> cols;

        public Header( String... cols ) {
            this.cols = List.of( cols );
        }

        public int size() {
            return cols.size();
        }
    }
}
