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

import com.fasterxml.jackson.annotation.JsonCreator;
import oap.io.Files;
import oap.io.Resources;
import oap.json.Binder;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class DictionaryParserTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void serialize() {
        var path = TestDirectoryFixture.testPath( "test/test.json" );
        DictionaryParser.serialize( Dictionaries.getDictionary( "test-dictionary" ), path, true );

        assertThat( Files.readString( path ) ).isEqualTo( """
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
        assertThat( dictionary.getOrDefault( 0, "not found" ) ).isEqualTo( "-" );
        assertThat( dictionary.getOrDefault( 'I', "not found" ) ).isEqualTo( "IMAGE" );

    }


    @Test
    public void testJsonParse() {
        var dictionary = Binder.hoconWithoutSystemProperties.unmarshal( DictionaryRoot.class, getClass().getResource( "/dictionary/test-dictionary.conf" ) );

        assertThat( dictionary.getId() ).isEqualTo( "test-dictionary" );
        assertThat( dictionary.getValues() ).hasSize( 3 );
    }

    @Test
    public void testDictionaryEnum() {
        var test1 = Binder.hoconWithoutSystemProperties.unmarshal( TestDictionaryContainer.class, "{value = TEST1}" );

        assertThat( test1.value ).isEqualTo( TestDictionary.TEST1 );
    }

    public enum TestDictionary implements Dictionary {
        TEST1, TEST2;

        @Override
        public int getOrDefault( String id, int defaultValue ) {
            return 0;
        }

        @Override
        public String getOrDefault( int externlId, String defaultValue ) {
            return null;
        }

        @Override
        public Integer get( String id ) {
            return null;
        }

        @Override
        public boolean containsValueWithId( String id ) {
            return false;
        }

        @Override
        public List<String> ids() {
            return null;
        }

        @Override
        public int[] externalIds() {
            return new int[0];
        }

        @Override
        public Map<String, Object> getProperties() {
            return null;
        }

        @Override
        public Optional<? extends Dictionary> getValueOpt( String name ) {
            return Optional.empty();
        }

        @Override
        public Dictionary getValue( String name ) {
            return null;
        }

        @Override
        public Dictionary getValue( int externalId ) {
            return null;
        }

        @Override
        public List<? extends Dictionary> getValues() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public <T> Optional<T> getProperty( String name ) {
            return Optional.empty();
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public int getExternalId() {
            return 0;
        }

        @Override
        public boolean containsProperty( String name ) {
            return false;
        }

        @Override
        public Dictionary cloneDictionary() {
            return null;
        }
    }

    public static class TestDictionaryContainer {
        public TestDictionary value;

        @JsonCreator
        public TestDictionaryContainer( TestDictionary value ) {
            this.value = value;
        }
    }
}
