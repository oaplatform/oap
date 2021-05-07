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
package oap.application.supervision;

import lombok.ToString;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.util.Numbers;
import org.apache.commons.lang3.RandomUtils;

import java.util.regex.Pattern;

public class CronScheduledService extends AbstractScheduledService {
    public final String cron;
    public final long jitter;

    public CronScheduledService( Runnable runnable, String cron ) {
        super( "cron", runnable );
        var info = parse( cron );
        this.cron = info.cron;
        this.jitter = info.jitter;
    }

    private static CronInfo parse( String cron ) {
        var m = Pattern.compile( "(.+)\\s+jitter\s+(\\d+\\w*)$" ).matcher( cron );
        if( m.matches() ) {
            return new CronInfo( m.group( 1 ).trim(), Numbers.parseLongWithUnits( m.group( 2 ) ) );
        }

        return new CronInfo( cron.trim(), 0L );
    }

    @Override
    protected Scheduled schedule() {
        return Scheduler.scheduleCron( cron, this );
    }

    @Override
    public void run() {
        if( jitter > 0 ) {
            try {
                Thread.sleep( RandomUtils.nextLong( 0, jitter + 1 ) );
            } catch( InterruptedException e ) {
                return;
            }
        }

        super.run();
    }

    @ToString
    private static class CronInfo {
        public final String cron;
        public final long jitter;

        private CronInfo( String cron, long jitter ) {
            this.cron = cron;
            this.jitter = jitter;
        }
    }
}
