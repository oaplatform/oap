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
import lombok.val;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 27.03.2018.
 */
public class ObjectHistogramTest {
    private static final int PERIOD_HOUR = 1000 * 60 * 60;

    @AfterMethod
    public void afterMethod() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testHour() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val histogram = new TestObjectHistogram( 5, PERIOD_HOUR );
        histogram.update( PERIOD_HOUR, new TestObject( 1 ) );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { new TestObject( 1 ), null, null, null, null } );
        DateTimeUtils.setCurrentMillisFixed( 1000 );
        histogram.update( PERIOD_HOUR, new TestObject( 4 ) );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { new TestObject( 5 ), null, null, null, null } );
    }

    @Test
    public void testHourShiftOne() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val histogram = new TestObjectHistogram( 5, PERIOD_HOUR );
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
    public void testHistogramShiftAll() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val histogram = new TestObjectHistogram( 5, PERIOD_HOUR );
        histogram.update( PERIOD_HOUR, new TestObject( 3 ) );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR * 100 + 10 );
        assertThat( histogram.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { null, null, null, null, null } );
    }

    @Test
    public void testMerge() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val master = new TestObjectHistogram( new TestObject[] { new TestObject( 1 ), new TestObject( 4 ), null }, 0 );
        val update = new TestObjectHistogram( new TestObject[] { new TestObject( 2 ), null, null }, 0 );

        master.merge( PERIOD_HOUR, update );

        assertThat( master.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { new TestObject( 3 ), new TestObject( 4 ), null } );
    }

    @Test
    public void testMergeShift() {
        DateTimeUtils.setCurrentMillisFixed( 0 );
        val master = new TestObjectHistogram( new TestObject[] { new TestObject( 1 ), new TestObject( 4 ), null }, 0 );

        val update = new TestObjectHistogram( new TestObject[] { new TestObject( 2 ), null, null }, 1 );

        DateTimeUtils.setCurrentMillisFixed( PERIOD_HOUR * 2 + 1 );

        master.merge( PERIOD_HOUR, update );

        assertThat( master.get( PERIOD_HOUR ) ).isEqualTo( new TestObject[] { null, new TestObject( 2 ), new TestObject( 1 ) } );
    }

    @EqualsAndHashCode
    @ToString
    private static class TestObject implements Mergeable<TestObject> {
        private int value;

        public TestObject() {
        }

        public TestObject( int value ) {
            this.value = value;
        }

        @Override
        public TestObject merge( TestObject object ) {
            this.value += object.value;

            return this;
        }
    }

    private static class TestObjectHistogram extends ObjectHistogram<TestObject> {

        public TestObjectHistogram( TestObject[] values, long lastTick ) {
            super( values, lastTick );
        }

        protected TestObjectHistogram() {
            super( TestObject.class );
        }

        protected TestObjectHistogram( int count, long period ) {
            super( TestObject.class, count, period );
        }
    }
}