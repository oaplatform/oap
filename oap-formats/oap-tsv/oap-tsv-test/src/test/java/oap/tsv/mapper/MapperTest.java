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

package oap.tsv.mapper;

import oap.io.content.ContentReader;
import oap.tsv.Tsv;
import org.testng.annotations.Test;

import static oap.io.content.ContentReader.ofJson;
import static oap.testng.Asserts.contentOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class MapperTest {
    @Test
    public void readConfiguration() {
        assertThat( contentOfTestResource( getClass(), "config.json", ofJson( Configuration.class ) ) )
            .isEqualTo( new Configuration(
                new Configuration.Column( 0, "a" ),
                new Configuration.Column( 1, "b" ) )
                .withColumnsNumber( 3 )
                .withValidateInput( true ) );
    }

    @Test
    public void mapToObj() {
        assertThat( ContentReader.read( "a\tb\tc\n1\t2\t3\n4\t5\t6", Tsv.tsv.ofSeparatedValues() )
            .withHeaders()
            .mapToObj( Mapper.of( Bean.class,
                contentOfTestResource( getClass(), "config.json", ofJson( Configuration.class ) ) ) ) )
            .containsExactly( new Bean( 1, 2 ), new Bean( 4, 5 ) );
    }

    @Test
    public void mapToObjValidations() {
        Configuration config = contentOfTestResource( getClass(), "config.json", ofJson( Configuration.class ) );
        assertThat( config.configure( ContentReader.read( "a\tb\tc\n1\t\n1\t2\t3\n4\t5\t6", Tsv.tsv.ofSeparatedValues() ) )
            .mapToObj( Mapper.of( Bean.class, config ) ) )
            .containsExactly( new Bean( 1, 2 ), new Bean( 4, 5 ) );
    }

    record Bean( int a, int b ) {}
}
