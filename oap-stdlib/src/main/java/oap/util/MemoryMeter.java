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

package oap.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MemoryMeter {
    private static final MemoryMeter NULL_MEMORY_METER = new MemoryMeter() {
        @Override
        public long measureDeep( Object object ) {
            return 0;
        }

        @Override
        public long countChildren( Object object ) {
            return 0;
        }

        @Override
        public long measure( Object object ) {
            return 0;
        }

        @Override
        public MemoryMeter withGuessing( org.github.jamm.MemoryMeter.Guess guess ) {
            return this;
        }

        @Override
        public MemoryMeter ignoreOuterClassReference() {
            return this;
        }

        @Override
        public MemoryMeter enableDebug() {
            return this;
        }

        @Override
        public MemoryMeter enableDebug( int depth ) {
            return this;
        }
    };
    private static JavaAgentStatus javaagent = JavaAgentStatus.UNKNOWN;

    public static MemoryMeter get() {
        if( javaagent == JavaAgentStatus.OFF ) return NULL_MEMORY_METER;

        var memoryMeter = new org.github.jamm.MemoryMeter().withGuessing( org.github.jamm.MemoryMeter.Guess.ALWAYS_UNSAFE );

        try {
            if( javaagent == JavaAgentStatus.UNKNOWN ) {
                memoryMeter.measure( 1 );
                javaagent = JavaAgentStatus.ON;
            }

            return new OapMemoryMeter( memoryMeter );
        } catch( IllegalStateException e ) {
            javaagent = JavaAgentStatus.OFF;
            log.error( e.getMessage() );

            return NULL_MEMORY_METER;
        }
    }

    /**
     * @return the memory usage of @param object including referenced objects
     * @throws NullPointerException if object is null
     */
    public abstract long measureDeep( Object object );

    /**
     * @return the number of child objects referenced by @param object
     * @throws NullPointerException if object is null
     */
    public abstract long countChildren( Object object );

    /**
     * @return the shallow memory usage of @param object
     * @throws NullPointerException if object is null
     */
    public abstract long measure( Object object );

    /**
     * @return a MemoryMeter that permits guessing the size of objects if instrumentation isn't enabled
     */
    public abstract MemoryMeter withGuessing( org.github.jamm.MemoryMeter.Guess guess );

    /**
     * @return a MemoryMeter that ignores the size of an outer class reference
     */
    public abstract MemoryMeter ignoreOuterClassReference();

    /**
     * Makes this <code>MemoryMeter</code> prints the classes tree to <code>System.out</code> when measuring
     */
    public abstract MemoryMeter enableDebug();

    /**
     * Makes this <code>MemoryMeter</code> prints the classes tree to <code>System.out</code> up to the specified depth
     * when measuring
     *
     * @param depth the maximum depth for which the class tree must be printed
     */
    public abstract MemoryMeter enableDebug( int depth );

    private enum JavaAgentStatus {
        UNKNOWN, ON, OFF
    }

    private static class OapMemoryMeter extends MemoryMeter {
        private final org.github.jamm.MemoryMeter memoryMeter;

        public OapMemoryMeter( org.github.jamm.MemoryMeter memoryMeter ) {
            this.memoryMeter = memoryMeter;
        }

        @Override
        public long measureDeep( Object object ) {
            return memoryMeter.measureDeep( object );
        }

        @Override
        public long countChildren( Object object ) {
            return memoryMeter.countChildren( object );
        }

        @Override
        public long measure( Object object ) {
            return memoryMeter.measure( object );
        }

        @Override
        public MemoryMeter withGuessing( org.github.jamm.MemoryMeter.Guess guess ) {
            return new OapMemoryMeter( memoryMeter.withGuessing( guess ) );
        }

        @Override
        public MemoryMeter ignoreOuterClassReference() {
            return new OapMemoryMeter( memoryMeter.ignoreOuterClassReference() );
        }

        @Override
        public MemoryMeter enableDebug() {
            return new OapMemoryMeter( memoryMeter.enableDebug() );
        }

        @Override
        public MemoryMeter enableDebug( int depth ) {
            return new OapMemoryMeter( memoryMeter.enableDebug( depth ) );
        }
    }
}
