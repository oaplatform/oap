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

package oap.ws.file;


import oap.io.content.ContentReader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static oap.testng.Asserts.contentOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class DataTest {

    @DataProvider
    public Object[][] jsons() {
        return new Object[][] {
            { "data-single.json", null },
            { "data-complex.json", "file.txt" }
        };
    }

    @Test( dataProvider = "jsons" )
    public void unmarshalSingle( String file, String name ) {
        Data data = contentOfTestResource( getClass(), file, ContentReader.ofJson( Data.class ) );
        assertThat( data.mimeType ).isEqualTo( "text/plain" );
        assertThat( data.extension() ).isEqualTo( "txt" );
        assertThat( data.decoded() ).isEqualTo( "test".getBytes() );
        assertThat( data.name ).isEqualTo( name );
    }
}
