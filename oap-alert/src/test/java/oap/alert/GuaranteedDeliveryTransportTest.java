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

import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by Admin on 03.06.2016.
 */
@SuppressWarnings( "unchecked" )
public class GuaranteedDeliveryTransportTest {

    @Test
    public void testRetryOnFailure() throws InterruptedException {
        GuaranteedDeliveryTransport tr = new GuaranteedDeliveryTransport( 10 );
        MessageTransport<String> backend = mock( MessageTransport.class );
        doThrow( IOException.class ).doNothing().when( backend ).send( anyString() );
        tr.send( "Aha!", backend );

        verify( backend, times( 2 ) ).send( "Aha!" );
    }

    @Test
    public void testStopsAfterMaxAttempts() throws InterruptedException {
        final int maxAttempts = 3;
        GuaranteedDeliveryTransport tr = new GuaranteedDeliveryTransport( 10, maxAttempts );
        MessageTransport<String> backend = mock( MessageTransport.class );
        doThrow( IOException.class ).when( backend ).send( anyString() );

        tr.send( "Aha!", backend );
        verify( backend, times( maxAttempts ) ).send( "Aha!" );
    }

    @Test( expectedExceptions = InterruptedException.class )
    public void testStopWhenInterrupted() throws InterruptedException {
        GuaranteedDeliveryTransport tr = new GuaranteedDeliveryTransport( 10 );
        MessageTransport<String> backend = mock( MessageTransport.class );
        doThrow( InterruptedException.class ).doNothing().when( backend ).send( anyString() );

        tr.send( "Aha!", backend );
    }

}
