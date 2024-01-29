/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.tsv;

import oap.io.content.ContentReader;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static oap.testng.Asserts.assertString;
import static oap.tsv.test.TsvAssertion.assertTsv;
import static oap.tsv.test.TsvAssertion.header;
import static oap.tsv.test.TsvAssertion.row;
import static org.assertj.core.api.Assertions.assertThat;

public class TsvStreamTest {
    @Test
    public void csv() {
        String csv = """
            "1","2","3"
            "1","2","3"
            """;
        assertString( ContentReader.read( csv, Tsv.csv.ofSeparatedValues() )
            .toCsvString() )
            .isEqualTo( csv );
    }

    @Test
    public void csvUnquoted() {
        String csv = """
            1,2,3
            1,2,3
            """;
        assertString( ContentReader.read( csv, Tsv.csv.ofSeparatedValues() )
            .toCsvString( false ) )
            .isEqualTo( csv );
    }

    @Test
    public void toList() {
        assertThat( ContentReader.read( "1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .toList() )
            .containsExactly(
                List.of( "1", "2", "3" ),
                List.of( "1", "2", "3" ) );
    }

    @Test
    public void toStrng() {
        assertString( ContentReader.read( "1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .toTsvString() )
            .isEqualTo( "1\t2\t3\n1\t2\t3\n" );
    }

    @Test
    public void withHeaders() {
        assertThat( ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .withHeaders()
            .withHeaders()
            .withHeaders()
            .withHeaders()
            .headers() )
            .containsExactly( "a", "b", "c" );
        assertTsv( ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .withHeaders()
            .toTsv() )
            .containsExactlyInAnyOrderEntriesOf(
                header( "a", "b", "c" ),
                row( "1", "2", "3" ),
                row( "1", "2", "3" ) );
    }

    @Test
    public void withHeadersEmpty() {
        assertTsv( ContentReader.read( "", Tsv.tsv.ofSeparatedValues() ).withHeaders().toTsv() )
            .isEqualToTsv( "" );
    }

    @Test
    public void select() {
        assertTsv( ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .withHeaders()
            .select( 0, 2 )
            .toTsv() )
            .containsExactlyInAnyOrderEntriesOf(
                header( "a", "c" ),
                row( "1", "3" ),
                row( "1", "3" ) );
    }

    @Test
    public void toStream() {
        assertThat( ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .withHeaders()
            .select( 0, 2 )
            .toStream()
            .skip( 1 )
            .toList() )
            .containsExactly(
                List.of( "1", "3" ),
                List.of( "1", "3" ) );
    }

    @Test
    public void selectByHeaders() {
        assertTsv( ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .select( "a", "c" )
            .toTsv() )
            .containsExactlyInAnyOrderEntriesOf(
                header( "a", "c" ),
                row( "1", "3" ),
                row( "1", "3" ) );
        assertTsv( ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .withHeaders()
            .select( "a", "c" )
            .toTsv() )
            .containsExactlyInAnyOrderEntriesOf(
                header( "a", "c" ),
                row( "1", "3" ),
                row( "1", "3" ) );
    }

    @Test
    public void stripHeaders() {
        assertTsv( ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .stripHeaders()
            .toTsv() )
            .containsExactlyInAnyOrderEntriesOf(
                List.of( "1", "2", "3" ),
                List.of( "1", "2", "3" ) );
    }

    @Test
    public void filter() {
        assertTsv( ContentReader.read( "\n\na\tb\tc\n\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .filter( line -> line.size() == 3 )
            .withHeaders()
            .select( 0, 2 )
            .toTsv() )
            .containsExactlyInAnyOrderEntriesOf(
                header( "a", "c" ),
                row( "1", "3" ),
                row( "1", "3" ) );
    }

    @Test
    public void toTsvOutputStream() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .collect( TsvStream.Collectors.toTsvOutputStream( bytes ) );
        assertString( bytes.toString() ).isEqualTo( "a\tb\tc\n1\t2\t3\n1\t2\t3\n" );
    }

    @Test
    public void mapToObj() {
        assertThat( ContentReader.read( "a\tb\tc\n1\t2\t3\n1\t2\t3", Tsv.tsv.ofSeparatedValues() )
            .withHeaders()
            .mapToObj( l -> Integer.parseInt( l.get( 0 ) ) ) )
            .containsExactly( 1, 1 );
    }

}
