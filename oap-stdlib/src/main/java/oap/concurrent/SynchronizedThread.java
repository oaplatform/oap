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
package oap.concurrent;

import java.util.concurrent.Semaphore;

public class SynchronizedThread implements Runnable, SynchronizedRunnableReadyListener {
    public long stopTimeout = 60000;
    private Thread thread = new Thread( this );
    private Runnable child;
    private Semaphore semaphore = new Semaphore( 0 );
    private boolean stopped = true;

    public SynchronizedThread( SynchronizedRunnable child ) {
        this.child = child;
        child.listener = this;
    }

    public SynchronizedThread( Runnable child ) {
        this.child = child;
    }

    public SynchronizedThread( Runnable child, long stopTimeout ) {
        this( child );
        this.stopTimeout = stopTimeout;
    }

    public SynchronizedThread( String name, Runnable child ) {
        this( child );
        this.thread.setName( name );
    }

    @Override
    public void run() {
        semaphore.release();
        child.run();
    }

    /**
     * {@link SynchronizedRunnable} has its own {@link SynchronizedRunnableReadyListener} notification scheme,
     * hence 2 permits are needed to be released: the 1st is {@link SynchronizedThread}'s internal and
     * the 2nd is released by {@link #notifyReady()} from {@link SynchronizedRunnable}
     */
    public synchronized void start() {
        stopped = false;
        thread.start();
        try {
            semaphore.acquire( child instanceof SynchronizedRunnable ? 2 : 1 );
        } catch( InterruptedException e ) {
            throw new ThreadException( e );
        }
    }

    @SuppressWarnings( "deprecated" )
    public synchronized void stop() {
        stopped = true;
        thread.interrupt();
        try {
            thread.join( stopTimeout );
        } catch( InterruptedException e ) {
            thread.stop();
        }
    }

    public String getName() {
        return this.thread.getName();
    }

    public void setName( String name ) {
        this.thread.setName( name );
    }

    public boolean isRunning() {
        return !stopped;
    }

    @Override
    public void notifyReady() {
        this.semaphore.release();
    }

    public synchronized void synchronous( Runnable code ) {
        code.run();
    }

}
