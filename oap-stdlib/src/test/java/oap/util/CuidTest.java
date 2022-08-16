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

import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CuidTest {

    @Test
    public void next() {
        var arr = new ArrayList<String>();
        for( int i = 0; i < 20; i++ ) {
            if( i % 3 == 0 ) Threads.sleepSafely( 1 );
            var cuid = Cuid.UNIQUE.next();
            arr.add( cuid );
            System.out.println( cuid );
        }

        for( var cuid : arr ) {
            System.out.println( cuid );
        }
    }

    @Test
    public void idLength() {
        assertThat( Cuid.UNIQUE.next().length() ).isEqualTo( 23 );
    }

    @Test
    public void hiResBug() {
        Set<String> ids = Sets.empty();

        int count = 2000000;
        for( int i = 0; i < count; i++ ) ids.add( Cuid.UNIQUE.next() );

        assertThat( ids.size() ).isEqualTo( count );
    }

    @Test
    public void last() {
        assertThat( Cuid.UNIQUE.last() )
            .isEqualTo( Cuid.UNIQUE.last() );
        assertThat( Cuid.UNIQUE.next() )
            .isEqualTo( Cuid.UNIQUE.last() )
            .isEqualTo( Cuid.UNIQUE.last() );
    }


    @Test
    public void toStringUniqueCuid() {
        System.out.println( Cuid.UniqueCuid.parse( "17621A6D3E200230AF4D60C" ) );

        System.out.println( "--" );
        System.out.println( Cuid.UniqueCuid.parse( "17621A46AC200320AF4D60C" ) );
        System.out.println( Cuid.UniqueCuid.parse( "17621A7B10700820AF4E002" ) );

        System.out.println( "--" );
        System.out.println( Cuid.UniqueCuid.parse( "17621CDBBFB001A0AF4E303" ) );
        System.out.println( Cuid.UniqueCuid.parse( "17621D24FDC00340AF4D20C" ) );

        System.out.println( "--" );
        System.out.println( Cuid.UniqueCuid.parse( "182864BB01800220AF4FE03" ) );
    }
}
