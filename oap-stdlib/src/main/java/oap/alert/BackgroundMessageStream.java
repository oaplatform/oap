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

package oap.alert;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class BackgroundMessageStream<Message> implements MessageStream<Message>, Runnable {

    private final MessageTransport<Message> transport;
    private final GuaranteedDeliveryTransport guaranteedDeliveryTransport;
    private final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();

    public BackgroundMessageStream( MessageTransport<Message> transport,
                                    GuaranteedDeliveryTransport guaranteedDeliveryTransport ) {
        this.transport = transport;
        this.guaranteedDeliveryTransport = guaranteedDeliveryTransport;
    }

    @Override
    public void send( Message p ) {
        messages.add( p );
    }

    @Override
    public void run() {
        while( true ) {
            Message message = null;
            try {
                message = messages.take();
                guaranteedDeliveryTransport.send( message, transport );
            } catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
                log.error( "Interrupted background message stream - exiting" );
                return;
            } catch( Exception e ) {
                log.error( "Unexpected exception while sending: " + message, e );
            }
        }
    }

}
