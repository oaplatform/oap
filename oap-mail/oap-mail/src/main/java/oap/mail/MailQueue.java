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
import oap.util.Dates;
import org.joda.time.DateTime;

import java.util.Iterator;
import java.util.function.Predicate;

@Slf4j
public class MailQueue {
    private final MailQueuePersistence mailQueuePersistence;
    public long brokenMessageTTL = Dates.w( 2 );

    public MailQueue( MailQueuePersistence mailQueuePersistence ) {
        this.mailQueuePersistence = mailQueuePersistence;
    }

    public MailQueue() {
        this( null );
    }

    public void add( Message message ) {
        log.trace( "adding {}", message );
        mailQueuePersistence.add( message );
    }

    public void processing( Predicate<Message> processor ) {
        DateTime ttl = DateTime.now().minus( brokenMessageTTL );
        Iterator<Message> iterator = mailQueuePersistence.iterator();
        while( iterator.hasNext() ) {
            Message message = iterator.next();

            if( processor.test( message ) ) {
                iterator.remove();
            } else if( message.created.isBefore( ttl ) ) {
                log.debug( "removing expired message: {}", message );
                iterator.remove();
            }
        }
    }

    public int size() {
        return mailQueuePersistence.size();
    }

    public Iterable<? extends Message> messages() {
        return mailQueuePersistence;
    }
}
