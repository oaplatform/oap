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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

import static oap.testng.Asserts.assertFile;

public class TsvTest {
    @DataProvider( name = "files" )
    public Object[][] files() {
        return new Object[][] {
            { "1.tsv" },
            { "1.tsv.gz" },
            { "1.tsv.zip" }
        };
    }

    @Test( dataProvider = "files" )
    public void loadTsv( String file ) {
        TypedListModel model = Model.typedList( false )
            .s( "c1", 1 )
            .i( "c3", 3 )
            .filterColumnCount( 4 );
        Path path = Env.deployTestData( getClass() );

        Stream<List<Object>> tsv = Tsv.tsv.fromPath( path.resolve( file ), model );
        assertFile( path.resolve( "result.tsv" ) ).hasContent( Tsv.print( tsv ) );
    }

}
