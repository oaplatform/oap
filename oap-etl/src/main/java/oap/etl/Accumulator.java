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
package oap.etl;

import java.util.List;
import java.util.function.Predicate;

public interface Accumulator<T extends Accumulator<T>> {

    static Accumulator count() {
        return new CountAccumulator();
    }

    static Accumulator intSum( int field ) {
        return new IntegerSumAccumulator( field );
    }

    static Accumulator longSum( int field ) {
        return new LongSumAccumulator( field );
    }

    static Accumulator avg( int field ) {
        return new AvgAccumulator( field );
    }

    static Accumulator cost( int moneyField, int eventField ) {
        return new CostAccumulator( moneyField, eventField );
    }

    static <T> Accumulator filter( Accumulator accumulator, int fileld, Predicate<T> filter ) {
        return new Filter<>( accumulator, fileld, filter );
    }

    void accumulate( List<Object> values );

    void reset();

    Object result();

    T clone();

    class CountAccumulator implements Accumulator<CountAccumulator> {
        private long count;

        @Override
        public void accumulate( List<Object> values ) {
            this.count++;
        }

        @Override
        public void reset() {
            this.count = 0;
        }

        @Override
        public Long result() {
            return this.count;
        }

        @Override
        public CountAccumulator clone() {
            return new CountAccumulator();
        }
    }

    class IntegerSumAccumulator implements Accumulator<IntegerSumAccumulator> {
        private int field;
        private int sum;

        public IntegerSumAccumulator( int field ) {
            this.field = field;
        }

        @Override
        public void accumulate( List<Object> values ) {
            this.sum += ( ( Number ) values.get( this.field ) ).intValue();
        }

        @Override
        public void reset() {
            this.sum = 0;
        }

        @Override
        public Integer result() {
            return this.sum;
        }

        @Override
        public IntegerSumAccumulator clone() {
            return new IntegerSumAccumulator( field );
        }
    }

    class LongSumAccumulator implements Accumulator<LongSumAccumulator> {
        private int field;
        private long sum;

        public LongSumAccumulator( int field ) {
            this.field = field;
        }

        @Override
        public void accumulate( List<Object> values ) {
            this.sum += ( ( Number ) values.get( this.field ) ).longValue();
        }

        @Override
        public void reset() {
            this.sum = 0;
        }

        @Override
        public Long result() {
            return this.sum;
        }

        @Override
        public LongSumAccumulator clone() {
            return new LongSumAccumulator( field );
        }
    }

    class AvgAccumulator implements Accumulator<AvgAccumulator> {
        private int field;
        private double sum;
        private int count;

        public AvgAccumulator( int field ) {
            this.field = field;
        }

        @Override
        public void accumulate( List<Object> values ) {
            this.sum += ( ( Number ) values.get( this.field ) ).doubleValue();
            this.count++;
        }

        @Override
        public void reset() {
            this.sum = 0;
            this.count = 0;
        }

        @Override
        public Double result() {
            return this.count > 0 ? this.sum / this.count : 0.0;
        }

        @Override
        public AvgAccumulator clone() {
            return new AvgAccumulator( field );
        }
    }

    class CostAccumulator implements Accumulator<CostAccumulator> {
        private int moneyField;
        private int eventField;
        private long money;
        private int events;

        public CostAccumulator( int moneyField, int eventField ) {
            this.moneyField = moneyField;
            this.eventField = eventField;
        }

        @Override
        public void accumulate( List<Object> values ) {
            this.money += ( ( Number ) values.get( this.moneyField ) ).longValue();
            this.events += ( ( Number ) values.get( this.eventField ) ).longValue();
        }

        @Override
        public void reset() {
            this.money = 0;
            this.events = 0;
        }

        @Override
        public Double result() {
            return this.events > 0 ? this.money / this.events : 0.0;
        }

        @Override
        public CostAccumulator clone() {
            return new CostAccumulator( moneyField, eventField );
        }
    }

    class Filter<T> implements Accumulator<Filter<T>> {
        private final Accumulator accumulator;
        private final Predicate<T> filter;
        private int field;

        public Filter( Accumulator accumulator, int field, Predicate<T> filter ) {
            this.accumulator = accumulator;
            this.field = field;
            this.filter = filter;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public void accumulate( List<Object> values ) {
            if( filter.test( ( T ) values.get( field ) ) ) accumulator.accumulate( values );
        }

        @Override
        public void reset() {
            accumulator.reset();
        }

        @Override
        public Object result() {
            return accumulator.result();
        }

        @Override
        public Filter<T> clone() {
            return new Filter<>( accumulator, field, filter );
        }
    }
}
