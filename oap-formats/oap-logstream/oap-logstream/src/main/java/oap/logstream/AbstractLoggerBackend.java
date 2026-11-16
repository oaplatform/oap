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
import oap.logstream.disk.LogstreamException;
import oap.util.Result;

import java.io.Closeable;
import java.util.Map;

public abstract class AbstractLoggerBackend implements Closeable {
    /**
     * shared: Result is immutable, no allocation per log() call
     */
    protected static final Result<Void, LogstreamException> SUCCESS = Result.success( null );

    public Result<Void, LogstreamException> log( ProtocolVersion version, Map<String, String> properties, String logType,
                                                   String[] headers, byte[][] types, byte[] buffer ) {
        return log( version, properties, logType, headers, types, buffer, 0, buffer.length );
    }

    public abstract Result<Void, LogstreamException> log( ProtocolVersion version, Map<String, String> properties, String logType,
                                                            String[] headers, byte[][] types, byte[] buffer, int offset, int length );

    /**
     * @param logId created ( and may be cached ) by the caller
     */
    public Result<Void, LogstreamException> log( ProtocolVersion version, LogId logId, byte[] buffer, int offset, int length ) {
        return log( version, logId.properties, logId.logType, logId.headers, logId.types, buffer, offset, length );
    }

    /**
     * @param compression codec of the buffer (server side, {@link ProtocolVersion#ROW_BINARY_V4} header).
     *                    Backends that do not store the compressed payload as is ignore it.
     */
    public Result<Void, LogstreamException> log( ProtocolVersion version, LogStreamCompression compression,
                                                   Map<String, String> properties, String logType,
                                                   String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
        return log( version, properties, logType, headers, types, buffer, offset, length );
    }

    public abstract void close();

    public abstract AvailabilityReport availabilityReport();

    public boolean isLoggingAvailable() {
        return availabilityReport().state == AvailabilityReport.State.OPERATIONAL;
    }
}
