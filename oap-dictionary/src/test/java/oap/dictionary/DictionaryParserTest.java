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

package oap.dictionary;

import oap.io.Resources;
import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import oap.util.Maps;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;

public class DictionaryParserTest extends Fixtures {
    {
        fixture( TestDirectory.FIXTURE );
    }

    @Test
    public void serialize() {
        Path path = Env.tmpPath( "test/test.json" );
        DictionaryParser.serialize( Dictionaries.getDictionary( "test-dictionary" ), path );

        DictionaryRoot dictionary = DictionaryParser.parse( path );

        Assertions.<Dictionary>assertThat( dictionary.getValues() ).contains( new DictionaryLeaf( "id2", true, '2',
            Maps.of( __( "title", "title2" ) ) )
        );

        Assertions.<Dictionary>assertThat( dictionary.getValues().get( 0 ).getValues() ).contains(
            new DictionaryLeaf( "id11", true, 11, Maps.of( __( "title", "title11" ) ) )
        );

        assertThat( dictionary.getProperty( "version" ) ).contains( 1L );
    }

    @Test(
        expectedExceptions = { DictionaryError.class },
        expectedExceptionsMessageRegExp = "duplicate eid: path: /id1; eid: 11; one: id11; two: id12, path: /id1/id12; eid: 50; one: id2; two: id3"
    )
    public void invalidEid() {
        Optional<URL> url = Resources.url( getClass(), getClass().getSimpleName() + "/" + "invalid-eid-dictionary.conf" );

        assertThat( url ).isPresent();

        DictionaryParser.parse( url.get() );
    }

    @Test
    public void zeroStringEid() {
        DictionaryRoot dictionary = Dictionaries.getDictionary( "test-dictionary2" );
        assertThat( dictionary.getOrDefault( 0, "not found" ) ).isEqualTo( "-" );
        assertThat( dictionary.getOrDefault( 'I', "not found" ) ).isEqualTo( "IMAGE" );

    }
}
