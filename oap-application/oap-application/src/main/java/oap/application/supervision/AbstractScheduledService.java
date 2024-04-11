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

import lombok.extern.slf4j.Slf4j;
import oap.remote.RemoteInvocationException;
import oap.concurrent.scheduler.Scheduled;

@Slf4j
public abstract class AbstractScheduledService implements WrapperService<Runnable>, Runnable {
    protected final Runnable runnable;
    private Scheduled scheduled;
    private final String type;

    public AbstractScheduledService( String type, Runnable runnable ) {
        this.type = type;
        this.runnable = runnable;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public Runnable service() {
        return runnable;
    }

    public void start() {
        this.scheduled = schedule();
    }

    protected abstract Scheduled schedule();

    @Override
    public void preStop() {
        Scheduled.cancel( scheduled );
    }

    @Override
    public void stop() {
    }

    @Override
    public void run() {
        try {
            this.runnable.run();
        } catch( Exception e ) {
            if( e instanceof RemoteInvocationException && e.getCause() instanceof java.net.http.HttpTimeoutException ) {
                log.error( e.getMessage() );
            } else {
                log.error( e.getMessage(), e );
            }
        }
    }
}
