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

package oap.application.remote;

import oap.application.Kernel;
import oap.application.Module;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;

import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class RemotingTest {
    @Test
    public void invoke() {
        List<URL> modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( RemotingTest.class, "module.conf" ) );

        Kernel kernel = new Kernel( modules );
        try {
            kernel.start( pathOfTestResource( getClass(), "application.conf" ) );

            assertThat( kernel.<RemoteClient>service( "remote-client1" ) ).isPresent().get()
                .satisfies( remote1 -> {
                    assertThat( remote1.accessible() ).isTrue();
                    assertString( remote1.toString() ).isEqualTo( "remote:remote-service-impl1@https://localhost:8980/remote/" );
                } );

            assertThat( kernel.<RemoteClient>service( "remote-client2" ) ).isPresent().get()
                .satisfies( s -> assertThat( s.accessible() ).isTrue() );

            assertThat( kernel.<RemoteClient>service( "remote-client3" ) ).isPresent().get()
                .satisfies( s -> assertThatThrownBy( s::accessible ).isInstanceOf( IllegalStateException.class ) );

        } finally {
            kernel.stop();
        }
    }


}
