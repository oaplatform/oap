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

package oap.logstream.net;

import lombok.extern.slf4j.Slf4j;
import oap.concurrent.scheduler.Scheduled;
import oap.concurrent.scheduler.Scheduler;
import oap.io.Closeables;
import oap.logstream.LoggingBackend;
import oap.metrics.Metrics;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SocketLoggingBackend implements LoggingBackend {

    public static final String METRICS_LOGGING_SOCKET = "logging_socket";
    private final String host;
    private final int port;
    private final Scheduled scheduled;
    protected int soTimeout = 5000;
    protected int maxBuffers = 5000;
    private DataSocket socket;
    private Buffers buffers;
    private long flushInterval = 10000;
    private boolean loggingAvailable = true;
    private boolean closed = false;

    public SocketLoggingBackend(String host, int port, Path location, int bufferSize) {
        this.host = host;
        this.port = port;
        this.buffers = new Buffers(location, bufferSize);
        this.scheduled = Scheduler.scheduleWithFixedDelay(flushInterval, TimeUnit.MILLISECONDS, this::send);
        Metrics.measureGauge(Metrics.name("logging_buffers_cache"), () -> buffers.cache.size());

    }

    public synchronized void send() {
        if (!closed) try {
            if (buffers.isEmpty()) loggingAvailable = true;

            log.debug("sending data to server...");
            if (this.socket == null || !socket.isConnected()) {
                Closeables.close(socket);
                this.socket = new DataSocket(host, port, soTimeout);
            }

            buffers.forEachReadyData(bucket -> {
                try {
                    log.trace("sending {}", bucket);
                    DataOutputStream out = socket.getOutputStream();
                    out.writeLong(bucket.id);
                    out.writeUTF(bucket.selector);
                    out.writeInt(bucket.buffer.length());
                    out.write(bucket.buffer.data(), 0, bucket.buffer.length());

                    Metrics.measureCounterIncrement(Metrics.name(METRICS_LOGGING_SOCKET), bucket.buffer.length());

                    loggingAvailable = true;
                    return true;
                } catch (IOException e) {
                    loggingAvailable = false;
                    log.warn(e.getMessage());
                    Closeables.close(socket);
                    return false;
                }
            });
        } catch (Exception e) {
            loggingAvailable = false;
            log.warn(e.getMessage());
            Closeables.close(socket);
        }
        if (!loggingAvailable) log.debug("logging unavailable");

    }

    @Override
    public void log(String hostName, String fileName, byte[] buffer, int offset, int length) {
        buffers.put(fileName, buffer, offset, length);
    }

    @Override
    public synchronized void close() {
        closed = true;
        Scheduled.cancel(scheduled);
        Closeables.close(socket);
        Closeables.close(buffers);
    }

    @Override
    public boolean isLoggingAvailable() {
        return loggingAvailable && !closed && buffers.allocatedBuffers() < maxBuffers;
    }
}
