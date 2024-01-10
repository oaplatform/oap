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
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BackgroundMessageStreamTest {

    @Test
    public void sendDoesntBlock() {
        TestTransport transport = new TestTransport();
        BackgroundMessageStream<String> stream = new BackgroundMessageStream<>( transport,
            new GuaranteedDeliveryTransport( 1 ) );

        stream.send( "Msg1" );
        assertThat( transport.messages ).isEmpty();
    }

    @Test
    public void sendIsExecutedInSeparateThread() {
        TestTransport transport = new TestTransport();
        BackgroundMessageStream<String> stream = new BackgroundMessageStream<>( transport,
            new GuaranteedDeliveryTransport( 1 ) );

        stream.send( "Msg1" );
        assertThat( transport.messages ).isEmpty();

        SynchronizedThread thread = new SynchronizedThread( stream );
        thread.start();
        Threads.sleepSafely( 100 );
        thread.stop();
        assertThat( transport.messages ).containsExactly( "Msg1" );
    }

}
