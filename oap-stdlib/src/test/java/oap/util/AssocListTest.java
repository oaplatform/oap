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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssocListTest {
    @Test
    public void add() {
        AssocList<String, Bean> beans = AssocList.forKey( b -> b.id );
        Bean b1 = new Bean( "a", "v1" );
        beans.add( b1 );
        assertThat( beans ).hasSize( 1 );
        assertThat( beans.get( "a" ) ).get().isEqualTo( b1 );
        Bean b2 = new Bean( "a", "v2" );
        beans.add( b2 );
        assertThat( beans ).hasSize( 1 );
        assertThat( beans.get( "a" ) ).get().isEqualTo( b2 );
    }

    @ToString
    @EqualsAndHashCode
    static class Bean {
        String id;
        String value;

        Bean( String id, String value ) {
            this.id = id;
            this.value = value;
        }
    }
}
