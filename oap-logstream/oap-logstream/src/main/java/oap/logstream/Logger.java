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

import oap.net.Inet;

import java.util.Map;

import static oap.logstream.LogStreamProtocol.CURRENT_PROTOCOL_VERSION;

public class Logger {
    protected final LogStreamProtocol.ProtocolVersion protocolVersion;
    protected final AbstractLoggerBackend backend;

    public Logger( AbstractLoggerBackend backend, LogStreamProtocol.ProtocolVersion protocolVersion ) {
        this.backend = backend;
        this.protocolVersion = protocolVersion;
    }

    public Logger( AbstractLoggerBackend backend ) {
        this( backend, CURRENT_PROTOCOL_VERSION );
    }

    public void log( String filePreffix, Map<String, String> properties, String logType,
                     String[] headers, byte[][] types, byte[] row ) {
        backend.log( protocolVersion, Inet.HOSTNAME, filePreffix, properties, logType, headers, types, row );
    }

    public boolean isLoggingAvailable() {
        return backend.isLoggingAvailable();
    }

    public AvailabilityReport availabilityReport() {
        return backend.availabilityReport();
    }
}
