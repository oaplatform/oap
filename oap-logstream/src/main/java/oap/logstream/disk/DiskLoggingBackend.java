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

package oap.logstream.disk;

import oap.io.Closeables;
import oap.logstream.LoggingBackend;
import oap.metrics.Metrics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class DiskLoggingBackend implements LoggingBackend {
    public static final int DEFAULT_BUFFER = 1024 * 100;
    public static final String METRICS_LOGGING_DISK = "logging_disk";
    final Path logDirectory;
    final String ext;
    final int bufferSize;
    private ConcurrentHashMap<String, LogWriter> writers = new ConcurrentHashMap<>();
    private int bucketsPerHour;
    private boolean closed;

    public DiskLoggingBackend(Path logDirectory, String ext, int bufferSize, int bucketsPerHour) {
        this.logDirectory = logDirectory;
        this.ext = ext;
        this.bufferSize = bufferSize;
        this.bucketsPerHour = bucketsPerHour;
    }

    @Override
    public void log(String hostName, String fileName, byte[] buffer, int offset, int length) {
        if (closed) throw new UncheckedIOException(new IOException("already closed!"));

        Metrics.measureCounterIncrement(Metrics.name(METRICS_LOGGING_DISK).tag("from", hostName));

        writers.computeIfAbsent(hostName + fileName,
                k -> new LogWriter(logDirectory.resolve(hostName), fileName, ext, bufferSize, bucketsPerHour))
                .write(buffer, offset, length);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            flush();
            writers.forEach((selector, writer) -> Closeables.close(writer));
            writers.clear();
        }
    }

    public void flush() {
        writers.forEach((selector, writer) -> writer.flush());
    }
}
