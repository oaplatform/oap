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
package oap.mail;


import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;

import java.io.Closeable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Mailman implements Runnable, Closeable {
    private final Transport transport;
    private final MailQueue queue;
    private final Semaphore semaphore = new Semaphore( 0 );
    private final Scheduled scheduled;

    public Mailman( Transport transport, MailQueue queue, long retryPeriod ) {
        super();
        this.transport = transport;
        this.queue = queue;

        scheduled = Scheduler.scheduleWithFixedDelay( retryPeriod, TimeUnit.MILLISECONDS, semaphore::release );
    }

    public void run() {
        var done = false;
        while( !done ) {
            try {
                semaphore.acquire();

                log.debug( "sending {} messages from queue ...", queue.size() );
                queue.processing( this::sendMessage );
            } catch( InterruptedException e ) {
                done = true;
            } catch( Exception e ) {
                log.error( e.getMessage(), e );
            }
        }
    }

    private boolean sendMessage( Message message ) {
        try {
            transport.send( message );
            return true;
        } catch( Exception e ) {
            log.error( "Cannot send a message: {}", message, e );
            return false;
        }
    }

    public void send( Message message ) {
        log.debug( "enqueue message {}", message );

        this.queue.add( message );

        semaphore.release();
    }

    @Override
    public void close() {
        Closeables.close( scheduled );
    }
}
