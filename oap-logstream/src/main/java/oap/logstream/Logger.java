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
import oap.util.Dates;
import org.joda.time.DateTimeUtils;

public class Logger {
    private LoggerBackend backend;

    public Logger( LoggerBackend backend ) {
        this.backend = backend;
    }

    public void log( String logFileName, String logType, int shard, int version, String line ) {
        logWithoutTime( logFileName, logType, shard, version, Dates.formatDateWithMillis( DateTimeUtils.currentTimeMillis() ) + "\t" + line );
    }

    public void logWithoutTime( String logFileName, String logType, int shard, int version, String line ) {
        backend.log( Inet.HOSTNAME, logFileName, logType, shard, version, line );
    }

    public boolean isLoggingAvailable() {
        return backend.isLoggingAvailable();
    }

    public boolean isLoggingAvailable( String selector ) {
        return backend.isLoggingAvailable( Inet.HOSTNAME, selector );
    }

    public AvailabilityReport availabilityReport() {
        return backend.availabilityReport();
    }
}
