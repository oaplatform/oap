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

import oap.io.IoAsserts;
import oap.testng.Env;
import oap.util.Stream;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

public class ModelTest {
    @Test
    public void load() {
        Path path = Env.deployTestData( getClass() );
        List<Path> paths = Stream.of( "1.tsv", "2.tsv", "3.tsv" ).map( path::resolve ).toList();
        Model.Complex complexModel = Model.complex( file -> {
            switch( file.getFileName().toString() ) {
                case "1.tsv":
                    return Model.withoutHeader().s( 1 ).i( 3 );
                case "2.tsv":
                    return Model.withoutHeader().s( 1 ).i( 4 );
                case "3.tsv":
                    return Model.withHeader().s( 1 ).v( 0 );
                default:
                    throw new IllegalArgumentException();

            }
        } );
        Stream<List<Object>> tsv = Tsv.fromPaths( paths, complexModel );
        IoAsserts.assertFileContent( path.resolve( "result.tsv" ), Tsv.print( tsv ) );
    }
}
