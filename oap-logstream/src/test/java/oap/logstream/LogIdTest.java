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

package oap.logstream;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 20.03.2019.
 */
public class LogIdTest {
    @Test
    public void hashAndEquals() {
        var lid1 = new LogId( "ln", "lt", "chn", 1, 1 );
        var lid2 = new LogId( "ln", "lt", "chn", 1, 1 );
        var lid3 = new LogId( "ln1", "lt", "chn", 1, 1 );
        var lid4 = new LogId( "ln", "lt1", "chn", 1, 1 );
        var lid5 = new LogId( "ln", "lt", "chn", 2, 1 );
        var lid6 = new LogId( "ln", "lt", "chn", 1, 2 );
        var lid7 = new LogId( "ln", "lt", "chn----!", 1, 1 );

        assertThat( lid1.hashCode() ).isEqualTo( lid2.hashCode() );
        assertThat( lid1.hashCode() ).isNotEqualTo( lid3.hashCode() );
        assertThat( lid1.hashCode() ).isNotEqualTo( lid4.hashCode() );
        assertThat( lid1.hashCode() ).isNotEqualTo( lid5.hashCode() );
        assertThat( lid1.hashCode() ).isNotEqualTo( lid6.hashCode() );
        assertThat( lid1.hashCode() ).isEqualTo( lid7.hashCode() );

        assertThat( lid1 ).isEqualTo( lid2 );
        assertThat( lid1 ).isNotEqualTo( lid3 );
        assertThat( lid1 ).isNotEqualTo( lid4 );
        assertThat( lid1 ).isNotEqualTo( lid5 );
        assertThat( lid1 ).isNotEqualTo( lid6 );
        assertThat( lid1 ).isEqualTo( lid7 );
    }
}
