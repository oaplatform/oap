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
import oap.concurrent.SynchronizedRunnable;
import oap.concurrent.SynchronizedRunnableReadyListener;
import oap.concurrent.SynchronizedThread;

@Slf4j
public class ThreadService extends SynchronizedRunnable implements WrapperService<Runnable>, SynchronizedRunnableReadyListener {

    private final Supervisor supervisor;
    private final SynchronizedThread thread = new SynchronizedThread( this );
    private final Runnable supervised;
    protected SynchronizedRunnableReadyListener listener;
    private int maxFailures = 100;

    public ThreadService( final String name, Runnable supervisee, final Supervisor supervisor ) {
        this.supervised = supervisee;

        this.supervisor = supervisor;
        this.thread.setName( name );
        if( supervisee instanceof SynchronizedRunnable )
            ( ( SynchronizedRunnable ) supervisee ).readyListener( this );
        else notifyReady();
    }

    @Override
    public String type() {
        return "thread";
    }

    @Override
    public Runnable service() {
        return supervised;
    }

    @Override
    public void run() {
        while( thread.isRunning() && maxFailures > 0 ) try {
            supervised.run();
        } catch( Exception e ) {
            maxFailures--;
            log.error( "Crushed unexpectedly with message: " + e.getMessage() + ". Restarting...", e );
        }
        if( maxFailures <= 0 ) {
            log.error( supervised + " constantly crushing. Requesting shutdown..." );
            new Thread( () -> {
                supervisor.preStop();
                supervisor.stop();
            } ).run();
        }
    }


    public synchronized void start() {
        log.debug( "starting thread " + thread.getName() );
        thread.start();
    }

    @Override
    public void preStop() {
        log.debug( "stopping thread " + thread.getName() );

        thread.stop();
    }

    public synchronized void stop() {
    }
}
