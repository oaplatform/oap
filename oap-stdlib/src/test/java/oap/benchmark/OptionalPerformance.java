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

package oap.benchmark;

import org.testng.annotations.Test;

import java.util.Optional;

import static oap.benchmark.Benchmark.benchmark;

public class OptionalPerformance {
    public static final int SAMPLES = 10000000;
    public static final int EXPERIMENTS = 5;

    @Test
    public void testOptionalVsNull() {
        benchmark( "OptionalChain", SAMPLES, () -> {
            optionalChainTest();
        } ).experiments( EXPERIMENTS ).run();

        benchmark( "Null", SAMPLES, () -> {
            nullTest();
        } ).experiments( EXPERIMENTS ).run();
    }

    public Optional<TestOuterOptional> getOuterOf() {
        return Optional.ofNullable( new TestOuterOptional( 6 ) );
    }

    public Integer optionalChainTest() {
        var outer = getOuter();
        return Optional.of( outer )
            .flatMap( o -> Optional.ofNullable( o.nested ) )
            .flatMap( n -> Optional.ofNullable( n.inner ) )
            .flatMap( i -> Optional.ofNullable( i.value ) )
            .get();
    }

    public Integer nullTest() {
        var outer = getOuter();

        return ( outer != null && outer.nested != null && outer.nested.inner != null ) ? outer.nested.inner.value
            : null;
    }

    public TestOuter getOuter() {
        return new TestOuter( 77 );
    }

    public class TestOuter {

        public TestNested nested;

        public TestOuter( Integer value ) {
            this.nested = new TestNested( value );
        }
    }

    public class TestNested {

        public TestInner inner;

        public TestNested( Integer value ) {
            this.inner = new TestInner( value );
        }
    }

    public class TestInner {

        public Integer value;

        public TestInner( Integer foo ) {
            this.value = foo;
        }
    }

    public class TestOuterOptional {

        public Optional<TestNestedOptional> nested;

        public TestOuterOptional( Integer value ) {
            this.nested = Optional.ofNullable( new TestNestedOptional( value ) );
        }
    }

    public class TestNestedOptional {

        public Optional<TestInnerOptional> inner;

        public TestNestedOptional( Integer value ) {
            this.inner = Optional.ofNullable( new TestInnerOptional( value ) );
        }
    }

    public class TestInnerOptional {

        public Optional<Integer> value;

        public TestInnerOptional( Integer foo ) {
            this.value = Optional.ofNullable( foo );
        }
    }
}
