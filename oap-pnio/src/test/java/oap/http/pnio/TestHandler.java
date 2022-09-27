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

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class TestHandler extends PnioRequestHandler<TestState> {
    private final String name;
    private final Type type;
    private final BiConsumer<PnioExchange<TestState>, TestState> function;

    public RuntimeException runtimeException;
    public long sleepTime = -1;

    public TestHandler( String name, Type type ) {
        this.name = name;
        this.type = type;
        function = ( pnioExchange, testState ) -> {
            if( runtimeException != null ) throw new RuntimeException( runtimeException );
            if( sleepTime > 0 ) {
                try {
                    TimeUnit.MILLISECONDS.sleep( sleepTime );
                } catch( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException( e );
                }
            }
            if( testState.sb.length() > 0 ) testState.sb.append( "\n" );
            var data = "name '" + name + "' type " + type + " thread '"
                + Thread.currentThread().getName().substring( 0, 2 )
                + "' new thread " + !testState.oldThreadName.equals( Thread.currentThread().getName() );
            testState.sb.append( data );
            testState.oldThreadName = Thread.currentThread().getName();
        };
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void handle( PnioExchange<TestState> pnioExchange, TestState testState ) {
        function.accept( pnioExchange, testState );
    }

    @Override
    public String description() {
        return "name '" + name + "' type " + type + " thread '" + Thread.currentThread().getName() + "'";
    }

    public TestHandler withException( RuntimeException testException ) {
        this.runtimeException = testException;
        return this;
    }

    public TestHandler withSleepTime( long duration ) {
        this.sleepTime = duration;
        return this;
    }
}
