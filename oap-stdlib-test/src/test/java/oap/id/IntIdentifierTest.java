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

package oap.id;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Lists;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntIdentifierTest {
    @Test
    public void forId() {
        var identifier = IntIdentifier.<Bean>forId( b -> b.id ).build();
        assertThat( identifier.get( new Bean( 1, "aaaa" ) ) ).isEqualTo( 1 );
        assertThat( identifier.get( new Bean( 2, "bbbb" ) ) ).isEqualTo( 2 );
    }

    @Test
    public void forIdWithSetter() {
        var identifier = IntIdentifier.<Bean>forId( b -> b.id, ( o, id ) -> o.id = id )
            .build();
        var a = new Bean( "aaaa" );
        var b = new Bean( "aaaa" );
        var conflicts = Lists.of( 1, 3 );
        assertThat( identifier.getOrInit( a, conflicts::contains ) ).isEqualTo( 2 );
        assertThat( a.id ).isEqualTo( 2 );
        conflicts.add( 2 );
        assertThat( identifier.getOrInit( b, conflicts::contains ) ).isEqualTo( 4 );
        assertThat( b.id ).isEqualTo( 4 );
    }

    @ToString
    @EqualsAndHashCode
    public static class Bean {

        public Integer id;
        public String s;

        public Bean( Integer id, String s ) {
            this.id = id;
            this.s = s;
        }

        public Bean( String s ) {
            this( null, s );
        }

    }
}
