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

import oap.util.Lists;
import oap.util.Maps;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static oap.dictionary.DictionaryParser.INCREMENTAL_ID_STRATEGY;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testng.Assert.assertTrue;

public class DictionaryTest {

    @Test
    public void parse() {
        Assertions.assertThat( Dictionaries.getDictionary( "test-dictionary" ).name ).isEqualTo( "test-dictionary" );
        List<? extends Dictionary> dictValues = Dictionaries.getDictionary( "test-dictionary" ).getValues();
        Assertions.<Dictionary>assertThat( dictValues ).contains( new DictionaryValue( "id1", true, '1',
                Lists.of(
                    new DictionaryLeaf( "id11", true, 11, Map.of( "title", "title11" ) ),
                    new DictionaryLeaf( "id12", true, 12, Map.of( "title", "title12" ) )
                ),
                Maps.of( __( "title", "title1" ) ) ),
            new DictionaryLeaf( "id2", true, 50, Map.of( "title", "title2", "property1", "val1" ) )
        );
        assertTrue( dictValues.get( 2 ).getTags().contains( "tag1" ) );
        assertTrue( dictValues.get( 2 ).getTags().contains( "tag2" ) );
    }

    @Test
    public void extend() {
        var values = Dictionaries
            .getDictionary( "test-dictionary-extends" )
            .getValue( "id2" )
            .getValues();

        assertThat( values ).hasSize( 3 );
        assertThat( values.get( 0 ).getId() ).isEqualTo( "id111" );
        assertThat( values.get( 1 ).getId() ).isEqualTo( "id112" );
        assertThat( values.get( 2 ).getId() ).isEqualTo( "id22" );

        assertThat( values.get( 0 ).getExternalId() ).isEqualTo( 111 );
        assertThat( values.get( 1 ).getExternalId() ).isEqualTo( 112 );
        assertThat( values.get( 2 ).getExternalId() ).isEqualTo( 113 );
    }

    @Test
    public void testChainExtend() {
        var values = Dictionaries
            .getDictionary( "test-dictionary-chain-extends", INCREMENTAL_ID_STRATEGY )
            .getValue( "DICT2" )
            .getValues();

        assertThat( values ).hasSize( 3 );
        assertThat( values.get( 0 ).getId() ).isEqualTo( "id1" );
        assertThat( values.get( 1 ).getId() ).isEqualTo( "id2" );
        assertThat( values.get( 2 ).getId() ).isEqualTo( "my-id" );
    }

    @Test
    public void extendDuplicate() {
        assertThatThrownBy( () ->
            Dictionaries
                .getDictionary( "test-dictionary-extends-duplicate" )
                .getValue( "id2" )
                .getValues() );
    }

    @Test
    public void extendIgnoreDuplicate() {
        var values = Dictionaries
            .getDictionary( "test-dictionary-extends-ignore-duplicate", INCREMENTAL_ID_STRATEGY )
            .getValue( "id2" )
            .getValues();

        assertThat( values ).hasSize( 3 );
        assertThat( values.get( 0 ).getId() ).isEqualTo( "id111" );
        assertThat( values.get( 1 ).getId() ).isEqualTo( "id22" );
        assertThat( values.get( 2 ).getId() ).isEqualTo( "id112" );
        assertThat( values.get( 2 ).getProperty( "type" ) ).contains( "test_new" );
    }

    @Test
    public void extendFilter() {
        var values = Dictionaries
            .getDictionary( "test-dictionary-extends-filter" )
            .getValue( "id2" )
            .getValues();

        assertThat( values ).hasSize( 2 );
        assertThat( values.get( 0 ).getId() ).isEqualTo( "id111" );
        assertThat( values.get( 1 ).getId() ).isEqualTo( "id22" );

        assertThat( values.get( 0 ).getExternalId() ).isEqualTo( 111 );
        assertThat( values.get( 1 ).getExternalId() ).isEqualTo( 113 );
    }
}
