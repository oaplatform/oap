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

package oap.http.pnio;

import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestWorkflowTest {
    @Test
    public void testSkip() {
        RequestWorkflow<Object> workflow = RequestWorkflow
            .init( new TestPnioRequestHandler( "1" ) )
            .next( new TestPnioRequestHandler( "2" ) )
            .next( new TestPnioRequestHandler( "3" ) )
            .next( new TestPnioRequestHandler( "4" ) )
            .build();

        assertThat( workflow.map( AbstractPnioRequestHandler::toString ) ).isEqualTo( List.of( "1", "2", "3", "4" ) );
        assertThat( workflow
            .skipBefore( h -> ( ( TestPnioRequestHandler ) h ).id.equals( "2" ) )
            .map( AbstractPnioRequestHandler::toString ) ).isEqualTo( List.of( "2", "3", "4" ) );
    }

    @Test
    private static class TestPnioRequestHandler extends ComputePnioRequestHandler<Object> {
        public final String id;

        private TestPnioRequestHandler( String id ) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }

        @Override
        public void handle( PnioExchange<Object> pnioExchange, Object o ) {
        }
    }
}
