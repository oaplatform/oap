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

package oap.tsv;

import org.testng.annotations.Test;

import static oap.tsv.Delimiters.COMMA;
import static oap.tsv.Delimiters.TAB;
import static oap.tsv.Tokenizer.parse;
import static org.assertj.core.api.Assertions.assertThat;

public class TokenizerTest {

    @Test
    public void parseSimple() {
        assertThat( parse( "1,22,33,44", COMMA ) )
            .containsExactly( "1", "22", "33", "44" );
        assertThat( parse( "1,22,33,", COMMA ) )
            .containsExactly( "1", "22", "33", "" );
        assertThat( parse( "1\t22\t33\t", TAB ) )
            .containsExactly( "1", "22", "33", "" );
    }

    @Test
    public void parseLimited() {
        assertThat( parse( "1,22,33,44", COMMA, 3, false ) )
            .containsExactly( "1", "22", "33" );
    }

    @Test
    public void parseQuoted() {
        assertThat( parse( "1,\"22\",33,44", COMMA, true ) )
            .containsExactly( "1", "22", "33", "44" );
        assertThat( parse( "1,\"2,2\",33,44", COMMA, true ) )
            .containsExactly( "1", "2,2", "33", "44" );
        assertThat( parse( "1,\"2\"\"2\",33,44", COMMA, true ) )
            .containsExactly( "1", "2\"2", "33", "44" );
    }
}