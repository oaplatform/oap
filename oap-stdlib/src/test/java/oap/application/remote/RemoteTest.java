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

import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.util.List;

import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class RemoteTest extends Fixtures {
    protected KernelFixture kernelFixture;

    {
        fixture( kernelFixture = new KernelFixture(
            pathOfTestResource( RemoteTest.class, "application.conf" ),
            List.of( urlOfTestResource( RemoteTest.class, "module.conf" ) )
        ) );
    }

    @Test
    public void invoke() {
        assertThat( kernelFixture.<RemoteClient>service( "remote-client" ) )
            .satisfies( remote -> {
                assertThat( remote.accessible() ).isTrue();
                //this tests local methods of Object.class
                assertString( remote.toString() ).isEqualTo( "remote:remote-service(retry=5)@http://localhost:8980/remote/" );
            } );

        assertThat( kernelFixture.<RemoteClient>service( "remote-client" ) )
            .satisfies( remote -> assertThatThrownBy( remote::erroneous ).isInstanceOf( IllegalStateException.class ) );

        assertThat( kernelFixture.<RemoteClient>service( "remote-client" ) )
            .satisfies( RemoteClient::testRetry );

        assertThat( kernelFixture.<RemoteClient>service( "remote-client-unreachable" ) )
            .satisfies( remote -> assertThatThrownBy( remote::accessible ).isInstanceOf( RemoteInvocationException.class ) );
    }

    @Test
    public void testStream() {
        assertThat( kernelFixture.<RemoteClient>service( "remote-client" ).testStream() )
            .contains( "1", "2", "3" );
        assertThat( kernelFixture.<RemoteClient>service( "remote-client" ).testStream() )
            .contains( "1", "2", "3" );
    }
}
