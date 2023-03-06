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

package oap.util.function;

import oap.util.Lists;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionsTest {
    @Test
    public void ifInstance() {
        List<String> ll = new LinkedList<>( Lists.of( "a", "b" ) );
        List<String> al = new ArrayList<>( Lists.of( "a", "b" ) );
        assertThat( Functions.ifInstance( ll, LinkedList.class, LinkedList::getFirst ) ).contains( "a" );
        assertThat( Functions.ifInstance( al, LinkedList.class, LinkedList::getFirst ) ).isEmpty();
    }

    @Test
    public void memoize() {
        AtomicInteger count = new AtomicInteger( 0 );
        Supplier<Integer> memoize = Functions.memoize( count::incrementAndGet );
        memoize.get();
        memoize.get();
        assertThat( count.get() ).isEqualTo( 1 );
    }

    @Test
    public void applyIf() {
        AtomicInteger actionValue = new AtomicInteger( 1 );
        Functions.applyIf( true, v -> v.set( 2 * v.get() ), actionValue);

        assertThat( actionValue.get() ).isEqualTo( 2 );
    }

    @Test
    public void applyIfElse() {
        AtomicInteger actionValue = new AtomicInteger( 3 );
        AtomicInteger elseValue = new AtomicInteger( 5 );

        Functions.applyIfElse( false, null, v -> v.set( 3 * v.get() ), elseValue );
        Functions.applyIfElse( true, v -> v.set( 11 * v.get() ), null, actionValue);

        assertThat( elseValue.get() ).isEqualTo( 15 );
        assertThat( actionValue.get() ).isEqualTo( 33 );
    }
}
