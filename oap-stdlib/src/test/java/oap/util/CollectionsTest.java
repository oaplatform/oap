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

import lombok.val;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class CollectionsTest {
    @Test
    public void testCount() {
        assertThat( Collections.count( asList( "a", "b", "ab" ), ( str ) -> str.startsWith( "a" ) ) ).isEqualTo( 2 );
    }

    @Test
    public void testFind() {
        val list = asList( "a", "b", "ab" );
        assertThat( Collections.find( list, ( str ) -> str.startsWith( "a" ) ) ).contains( "a" );
        assertThat( Collections.find( list, ( str ) -> str.startsWith( "z" ) ) ).isEmpty();
    }

    @Test
    public void testFind2() {
        val list = asList( "a", "b", "ab" );
        assertThat( Collections.find2( list, ( str ) -> str.startsWith( "a" ) ) ).isEqualTo( "a" );
        assertThat( Collections.find2( list, ( str ) -> str.startsWith( "z" ) ) ).isNull();
    }

    @Test
    public void testAllMatch() {
        val list = asList( 1, 2, 4, 5 );

        assertThat( Collections.allMatch( list, i -> i <= 5 ) ).isTrue();
        assertThat( Collections.allMatch( list, i -> i >= 5 ) ).isFalse();
    }

    @Test
    public void testAnyMatch() {
        val list = asList( 1, 2, 4, 5 );

        assertThat( Collections.anyMatch( list, i -> i == 1 ) ).isTrue();
        assertThat( Collections.anyMatch( list, i -> i == 6 ) ).isFalse();
    }

    @Test
    public void testGroupBy() {
        val list = asList( 1, 2, 1, 4 );

        assertThat( Collections.groupBy( list, ( i ) -> i + 1 ) )
            .containsOnly( entry( 2, asList( 1, 1 ) ), entry( 3, asList( 2 ) ), entry( 5, asList( 4 ) ) );

    }

    @Test
    public void testMax() {
        val list = asList( 1, 2, 1, 4 );

        assertThat( Collections.max( list ) ).isEqualTo( 4 );
    }

    @Test
    public void testHead2() {
        assertThat( Collections.head2( new LinkedHashSet<>( asList( 3, 6, 8 ) ) ) ).isEqualTo( 3 );
        assertThat( Collections.head2( new LinkedHashSet<Integer>() ) ).isNull();
    }
}
