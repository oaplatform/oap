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

import oap.testng.Env;
import oap.util.Stream;
import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static oap.testng.Asserts.assertFile;
import static oap.tsv.TypedListModel.ColumnType.INT;
import static oap.tsv.TypedListModel.ColumnType.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public class TypedListModelTest {
    private Path path;

    @BeforeMethod
    public void setUp() {
        path = Env.deployTestData( getClass() );
    }

    @Test
    public void load() {
        List<Path> paths = Stream.of( "1.tsv", "2.tsv", "3.tsv" ).map( path::resolve ).toList();
        Model.Complex<List<Object>> complexModel = TypedListModel.complex( file -> {
            switch( FilenameUtils.getName( file ) ) {
                case "1.tsv":
                    return Model.typedList( false ).s( "c0", 1 ).i( "c1", 3 );
                case "2.tsv":
                    return Model.typedList( false ).s( "c0", 1 ).i( "c1", 4 );
                case "3.tsv":
                    return Model.typedList( true ).s( "c0", 1 ).v( "c1", INT, 0 );
                default:
                    throw new IllegalArgumentException();

            }
        } );
        assertFile( path.resolve( "result.tsv" ) ).hasContent( Tsv.print( Tsv.tsv.fromPaths( paths, complexModel ) ) );
    }

    @Test
    public void datatypes() {
        TypedListModel model = Model.typedList( false ).b( "c0", 0 ).i( "c1", 1 ).d( "c2", 2 ).s( "c3", 3 ).l( "c4", 4 );
        Path datatypesTsv = path.resolve( Paths.get( "datatypes.tsv" ) );
        Tsv.tsv.fromPath( datatypesTsv, model ).forEach( row -> {
            assertEquals( true, row.get( 0 ) );
            assertEquals( 1, row.get( 1 ) );
            assertEquals( 1.6, row.get( 2 ) );
            assertEquals( "Some value", row.get( 3 ) );
            assertEquals( 9223312036854775807L, row.get( 4 ) );
        } );
    }

    @Test
    public void filter() {
        TypedListModel model = Model.typedList( false ).b( "c0", 0 ).i( "c1", 1 ).d( "c2", 2 ).s( "c3", 3 ).l( "c4", 4 );
        final TypedListModel newModel = model.filter( "c1", "c3" );

        assertThat( newModel.size() ).isEqualTo( 2 );
        assertThat( newModel.getColumn( 0 ).name ).isEqualTo( "c1" );
        assertThat( newModel.getColumn( 1 ).name ).isEqualTo( "c3" );

    }

    @Test
    public void syncOffsetToIndex() {
        TypedListModel model = Model.typedList( false ).b( "c0", 10 ).i( "c1", 2 ).v( "c2", STRING, "str" );
        TypedListModel newModel = model.syncOffsetToIndex();

        assertThat( newModel.size() ).isEqualTo( 3 );
        assertThat( newModel.getOffset( "c0" ) ).isEqualTo( 0 );
        assertThat( newModel.getOffset( "c1" ) ).isEqualTo( 1 );
        assertThat( newModel.getOffset( "c2" ) ).isEqualTo( 2 );

    }
}
