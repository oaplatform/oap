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

import oap.concurrent.Threads;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;

public class CuidTest {

    @Test
    public void next() {
        for( int i = 0; i < 20; i++ ) {
            if( i % 3 == 0 ) Threads.sleepSafely( 1 );
            System.out.println( Cuid.next() );
        }
    }

    @Test
    public void idLength() {
        assertThat( Cuid.next().length() ).isEqualTo( 23 );
    }

    @Test
    public void hiResBug() {
        Set<String> ids = Sets.empty();
        List<String> idl = Lists.empty();

        int count = 1000000;
        for( int i = 0; i < count; i++ ) {
            String next = Cuid.next();
            ids.add( next );
            idl.add( next );
        }
        Cuid.resetToDefaults();
        for( int i = 0; i < count; i++ ) {
            String next = Cuid.next();
            ids.add( next );
            idl.add( next );
        }
        assertString( String.join( "\n", ids ) ).isEqualTo( Strings.join( "\n", idl ) );
        assertThat( ids.size() ).isEqualTo( count * 2 );
    }

}

