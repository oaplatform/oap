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

package oap.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AKHashTest {
    @Test
    public void hash() {
        assertThat( AKHash.hash( "j.smith@smith.com" ) ).isEqualTo( "SMVRLFSMTXJH" );
        assertThat( AKHash.hash( "j.smith@smith.com", 16 ) ).isEqualTo( "MWLFJHCSRMSHVHTX" );
        assertThat( AKHash.hash( "j@smith.com" ) ).isEqualTo( "SQNRMFCMNUJH" );
        assertThat( AKHash.hash( "a" ) ).isEqualTo( "VKUYJXLWMITZ" );
        assertThat( AKHash.hash( "A" ) ).isEqualTo( "PWOSXRVQUYNT" );
        assertThat( AKHash.hash( "b" ) ).isEqualTo( "DKCGLFJEIMBH" );
        assertThat( AKHash.hash( "/" ) ).isEqualTo( "XMWQLZNYOKVP" );
        assertThat( AKHash.hash( "@" ) ).isEqualTo( "OVNRWQUPTXMS" );
    }

    @Test
    public void collisions() {
        ListMultimap<String, String> collisions = ArrayListMultimap.create();
        int count = 0;
        for( int i = 100000; i < 1000000; i++ ) {
            String value = Integer.toString( i, 36 );
            String hash = AKHash.hash( value, 30 );
            collisions.put( hash, value );
            count++;
        }
//        BiStream.of( collisions.asMap() )
//            .filter( ( h, v ) -> v.size() > 1 )
//            .forEach( ( h, v ) -> System.out.printf( "%s -> %s\n", h, v ) );
        System.out.format( "values: %s -> hashes: %s, collision rate: %2.0f%%\n", count, collisions.asMap().size(), 100d * collisions.asMap().size() / count );
    }
}