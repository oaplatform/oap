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
import org.joda.time.DateTimeUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectHistogramTest {
    private static final int PERIOD_HOUR = 1000 * 60 * 60;

    @AfterMethod
    public void afterMethod() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void hour() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        var histogram = new TestObjectHistogram( 5, PERIOD_HOUR );
        histogram.update( PERIOD_HOUR, new TestObject( 1 ) );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { new TestObject( 1 ), null, null, null, null } );
        DateTimeUtils.setCurrentMillisFixed( 1000 );
        histogram.update( PERIOD_HOUR, new TestObject( 4 ) );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { new TestObject( 5 ), null, null, null, null } );
    }

    @Test
    public void updateForTime() {
        DateTimeUtils.setCurrentMillisFixed( 10000000 );
        var histogram = new TestObjectHistogram( 5, PERIOD_HOUR );
        histogram.update( PERIOD_HOUR, new TestObject( 1 ) );

        histogram.update( PERIOD_HOUR, 10000000 - PERIOD_HOUR, new TestObject( 2 ) );
        histogram.update( PERIOD_HOUR, 10000000 - PERIOD_HOUR * 10, new TestObject( 2 ) );

        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { new TestObject( 1 ), new TestObject( 2 ), null, null, null } );
    }

    @Test
    public void hourShiftOne() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        var histogram = new TestObjectHistogram( 5, PERIOD_HOUR );
        histogram.update( PERIOD_HOUR, new TestObject( 3 ) );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR + 10 );
        histogram.update( PERIOD_HOUR, new TestObject( 4 ) );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { new TestObject( 4 ), new TestObject( 3 ), null, null, null } );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR * 2 + 10 );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] {
            null, new TestObject( 4 ), new TestObject( 3 ), null, null
        } );
    }

    @Test
    public void histogramShiftAll() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        var histogram = new TestObjectHistogram( 5, PERIOD_HOUR );
        histogram.update( PERIOD_HOUR, new TestObject( 3 ) );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR * 100 + 10 );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { null, null, null, null, null } );
    }

    @Test
    public void merge() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        var master = new TestObjectHistogram( new TestObject[] { new TestObject( 1 ), new TestObject( 4 ), null }, 0 );
        var update = new TestObjectHistogram( new TestObject[] { new TestObject( 2 ), null, null }, 0 );

        master.merge( PERIOD_HOUR, update );

        assertThat( master.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { new TestObject( 3 ), new TestObject( 4 ), null } );
    }

    @Test
    public void mergeShift() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        var master = new TestObjectHistogram( new TestObject[] { new TestObject( 1 ), new TestObject( 4 ), null }, 0 );

        var update = new TestObjectHistogram( new TestObject[] { new TestObject( 2 ), null, null }, 1 );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR * 2 + 1 );

        master.merge( PERIOD_HOUR, update );

        assertThat( master.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { null, new TestObject( 2 ), new TestObject( 1 ) } );
    }

    @EqualsAndHashCode
    @ToString
    private static class TestObject implements Mergeable<TestObject> {
        private int value;

        private TestObject() {
        }

        private TestObject( int value ) {
            this.value = value;
        }

        @Override
        public TestObject merge( TestObject object ) {
            this.value += object.value;

            return this;
        }
    }

    private static class TestObjectHistogram extends ObjectHistogram<TestObject> {

        private TestObjectHistogram( TestObject[] values, long lastTick ) {
            super( values, lastTick );
        }

        private TestObjectHistogram() {
            super();
        }

        protected TestObjectHistogram( int count, long period ) {
            super( TestObject.class, count, period );
        }
    }
}
