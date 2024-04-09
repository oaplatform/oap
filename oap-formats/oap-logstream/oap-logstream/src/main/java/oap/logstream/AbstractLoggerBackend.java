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
package oap.logstream;


import oap.logstream.LogStreamProtocol.ProtocolVersion;

import java.io.Closeable;
import java.util.Map;

public abstract class AbstractLoggerBackend implements Closeable {
    public final LoggerListeners listeners = new LoggerListeners();

    public void log( ProtocolVersion version, String hostName, String filePreffix, Map<String, String> properties, String logType,
                     String[] headers, byte[][] types, byte[] buffer ) {
        log( version, hostName, filePreffix, properties, logType, headers, types, buffer, 0, buffer.length );
    }

    public abstract void log( ProtocolVersion version, String hostName, String filePreffix, Map<String, String> properties, String logType,
                              String[] headers, byte[][] types, byte[] buffer, int offset, int length );

    public abstract void close();

    public abstract AvailabilityReport availabilityReport();

    public boolean isLoggingAvailable() {
        return availabilityReport().state == AvailabilityReport.State.OPERATIONAL;
    }

    public void addListener( LoggerListener listener ) {
        listeners.addListener( listener );
    }

    public void removeListener( LoggerListener listener ) {
        listeners.removeListener( listener );
    }
}
