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

import oap.io.Files;
import oap.io.Resources;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;

public class DictionaryParserTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void serialize() {
        var path = TestDirectoryFixture.testPath( "test/test.json" );
        DictionaryParser.serialize( Dictionaries.getDictionary( "test-dictionary" ), path, true );

        assertString( Files.readString( path ) ).isEqualTo( """
            {
              "name" : "test-dictionary",
              "version" : 1,
              "values" : [ {
                "id" : "id1",
                "eid" : 49,
                "values" : [ {
                  "id" : "id11",
                  "eid" : 11,
                  "title" : "title11"
                }, {
                  "id" : "id12",
                  "eid" : 12,
                  "title" : "title12"
                } ],
                "title" : "title1"
              }, {
                "id" : "id2",
                "eid" : 50,
                "property1" : "val1",
                "title" : "title2"
              }, {
                "id" : "id3",
                "eid" : 51,
                "tags" : [ "tag1", "tag2" ]
              } ]
            }""" );
    }

    @Test(
        expectedExceptions = { DictionaryError.class },
        expectedExceptionsMessageRegExp = "duplicate eid: path: /id1; eid: 11; one: id11; two: id12, path: /id1/id12; eid: 50; one: id2; two: id3"
    )
    public void invalidEid() {
        var url = Resources.url( getClass(), getClass().getSimpleName() + "/" + "invalid-eid-dictionary.conf" );

        assertThat( url ).isPresent();

        DictionaryParser.parse( url.get() );
    }

    @Test
    public void zeroStringEid() {
        var dictionary = Dictionaries.getDictionary( "test-dictionary2" );
        assertString( dictionary.getOrDefault( 0, "not found" ) ).isEqualTo( "-" );
        assertString( dictionary.getOrDefault( 'I', "not found" ) ).isEqualTo( "IMAGE" );

    }
}
