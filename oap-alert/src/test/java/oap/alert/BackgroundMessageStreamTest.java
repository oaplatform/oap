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

import oap.concurrent.SynchronizedThread;
import oap.concurrent.Threads;
import oap.testng.AbstractTest;
import org.mockito.Mock;
import org.testng.annotations.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Created by Admin on 03.06.2016.
 */
public class BackgroundMessageStreamTest extends AbstractTest {

    @Mock
    MessageTransport<String> transport;

    @Mock
    GuaranteedDeliveryTransport guaranteedDeliveryTransport;

    @Test
    public void testSendDoesntBlock() {

        BackgroundMessageStream<String> backgroundStream = new BackgroundMessageStream<>( transport,
            guaranteedDeliveryTransport );

        backgroundStream.send( "Msg1" );
        verifyZeroInteractions( guaranteedDeliveryTransport );
        verifyZeroInteractions( transport );
    }

    @Test
    public void testSendIsExecutedInSeparateThread() throws InterruptedException {

        BackgroundMessageStream<String> backgroundStream = new BackgroundMessageStream<>( transport,
            guaranteedDeliveryTransport );

        backgroundStream.send( "Msg1" );
        verifyZeroInteractions( guaranteedDeliveryTransport );
        verifyZeroInteractions( transport );

        SynchronizedThread thread = new SynchronizedThread( backgroundStream );
        thread.start();
        Threads.sleepSafely( 100 );
        thread.stop();
        verify( guaranteedDeliveryTransport, times( 1 ) ).send( "Msg1", transport );
    }


}
