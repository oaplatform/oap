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

package oap.metrics;

import lombok.ToString;

import java.io.Serializable;

/**
 * Created by igor.petrenko on 09/11/2019.
 */
@ToString
public class InfluxMetricsConfiguration implements Serializable {
    private static final long serialVersionUID = 207291139773919863L;

    public final TimerConfiguration timer = new TimerConfiguration();
    public final HistogramConfiguration histogram = new HistogramConfiguration();
    public final MeterConfiguration meter = new MeterConfiguration();

    @ToString
    public static class MeterConfiguration extends HistogramConfiguration {
        private static final long serialVersionUID = 9156993654276695856L;

        public boolean oneMinuteRate = true;
        public boolean fiveMinuteRate = false;
        public boolean fifteenMinuteRate = false;
        public boolean count = true;
        public boolean meanRate = true;
    }

    @ToString
    public static class TimerConfiguration extends HistogramConfiguration {
        private static final long serialVersionUID = -7271222012930755231L;

        public boolean oneMinuteRate = false;
        public boolean fiveMinuteRate = false;
        public boolean fifteenMinuteRate = false;
        public boolean meanRate = false;
    }

    @ToString
    public static class HistogramConfiguration implements Serializable {
        private static final long serialVersionUID = 9172465537038550035L;

        public boolean mean = true;
        public boolean p75th = false;
        public boolean p95th = false;
        public boolean p98th = false;
        public boolean p99th = false;
        public boolean p999th = false;
        public boolean max = false;
        public boolean min = false;
        public boolean median = false;
        public boolean stddev = false;
        public boolean count = false;
    }
}
