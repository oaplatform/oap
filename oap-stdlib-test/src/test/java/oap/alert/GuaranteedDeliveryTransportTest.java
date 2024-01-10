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

import lombok.SneakyThrows;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings( "unchecked" )
public class GuaranteedDeliveryTransportTest {

    @Test
    public void retryOnFailure() throws InterruptedException {
        GuaranteedDeliveryTransport gdt = new GuaranteedDeliveryTransport( 10 );
        TestTransport transport = new TestTransport( 3 );
        gdt.send( "Aha!", transport );

        assertThat( transport.messages ).containsOnly( "Aha!" );
        assertThat( transport.failures ).isZero();
    }

    @Test
    public void stopsAfterMaxAttempts() throws InterruptedException {
        GuaranteedDeliveryTransport gdt = new GuaranteedDeliveryTransport( 10, 3 );
        TestTransport transport = new TestTransport( 10 );

        gdt.send( "Aha!", transport );
        assertThat( transport.messages ).isEmpty();
        assertThat( transport.failures ).isEqualTo( 7 );
    }

    @Test( expectedExceptions = InterruptedException.class )
    public void stopWhenInterrupted() throws InterruptedException {
        GuaranteedDeliveryTransport gdt = new GuaranteedDeliveryTransport( 10 );
        MessageTransport<String> transport = new MessageTransport<String>() {
            @Override
            @SneakyThrows
            public void send( String message ) {
                throw new InterruptedException();
            }
        };
        gdt.send( "Aha!", transport );
    }

}
