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
    };
    private static JavaAgentStatus javaagent = JavaAgentStatus.UNKNOWN;

    public static MemoryMeter get() {
        if( javaagent == JavaAgentStatus.OFF ) return NULL_MEMORY_METER;

        var memoryMeter = new org.github.jamm.MemoryMeter().enableDebug();

        try {
            if( javaagent == JavaAgentStatus.UNKNOWN ) {
                memoryMeter.measure( 1 );
                javaagent = JavaAgentStatus.ON;
            }

            return new MemoryMeter() {
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
            };
        } catch( IllegalStateException e ) {
            javaagent = JavaAgentStatus.OFF;
            log.error( e.getMessage() );

            return NULL_MEMORY_METER;
        }
    }

    public abstract long measureDeep( Object object );

    public abstract long countChildren( Object object );

    public abstract long measure( Object object );

    private enum JavaAgentStatus {
        UNKNOWN, ON, OFF
    }
}
