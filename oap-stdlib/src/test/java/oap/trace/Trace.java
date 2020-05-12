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

package oap.trace;

import oap.json.Binder;

import java.util.function.Supplier;

public interface Trace {
    int HTTP_STATUS_TRACE_OK = 299;
    Trace NULL = new NullTrace();

    static Trace create() {
        return new CollectingTrace();
    }

    void message( String label, Supplier<Object> value );

    void message( String label, Object value );

    String getTrace();

    boolean isTracing();

    class NullTrace implements Trace {
        @Override
        public void message( String label, Supplier<Object> value ) {

        }

        @Override
        public void message( String label, Object value ) {

        }

        @Override
        public String getTrace() {
            return null;
        }

        @Override
        public boolean isTracing() {
            return false;
        }
    }

    class CollectingTrace implements Trace {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public void message( String label, Supplier<Object> value ) {
            message( label, value.get() );
        }

        @Override
        public void message( String label, Object value ) {
            sb.append( label ).append( ": " )
                .append( value instanceof String ? ( String ) value : Binder.json.marshal( value ) )
                .append( '\n' );
        }

        @Override
        public String getTrace() {
            return sb.toString();
        }

        @Override
        public boolean isTracing() {
            return true;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
