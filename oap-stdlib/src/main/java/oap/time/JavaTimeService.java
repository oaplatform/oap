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

package oap.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class JavaTimeService implements TimeService {
    public static final JavaTimeService INSTANCE = new JavaTimeService();

    private Clock clock = Clock.system( ZoneOffset.UTC );

    @Override
    public Instant now() {
        return Instant.now( clock );
    }

    @Override
    public long currentTimeMillis() {
        return clock.millis();
    }

    public void useFixedClockAt( Instant date ) {
        clock = Clock.fixed( date, ZoneOffset.UTC );
    }

    public void setCurrentMillisFixed( long fixedMillis ) {
        clock = Clock.fixed( Instant.ofEpochMilli( fixedMillis ), ZoneOffset.UTC );
    }

    public void setCurrentMillisSystem() {
        clock = Clock.system( ZoneOffset.UTC );
    }
}
