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

package oap.prometheus;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;

@Slf4j
public class PrometheusJvmExporter implements Closeable, AutoCloseable {
    public boolean enableClassLoaderMetrics = true;
    public boolean enableJvmMemoryMetrics = true;
    public boolean enableJvmGcMetrics = true;
    public boolean enableLogbackMetrics = true;
    public boolean enableJvmThreadMetrics = true;

    private JvmGcMetrics jvmGcMetrics;
    private LogbackMetrics logbackMetrics;

    public void start() {
        log.info( "enableClassLoaderMetrics = {}, enableJvmMemoryMetrics = {}, enableJvmGcMetrics = {}, enableLogbackMetrics = {}",
            enableClassLoaderMetrics, enableJvmMemoryMetrics, enableJvmGcMetrics, enableLogbackMetrics );

        if( enableClassLoaderMetrics ) new ClassLoaderMetrics().bindTo( Metrics.globalRegistry );
        if( enableJvmMemoryMetrics ) new JvmMemoryMetrics().bindTo( Metrics.globalRegistry );
        if( enableJvmGcMetrics ) {
            jvmGcMetrics = new JvmGcMetrics();
            jvmGcMetrics.bindTo( Metrics.globalRegistry );
        }

        if( enableLogbackMetrics ) {
            logbackMetrics = new LogbackMetrics();
            logbackMetrics.bindTo( Metrics.globalRegistry );
        }

        if( enableJvmThreadMetrics ) new JvmThreadMetrics().bindTo( Metrics.globalRegistry );
    }

    @Override
    public void close() {
        if( logbackMetrics != null ) logbackMetrics.close();
        if( jvmGcMetrics != null ) jvmGcMetrics.close();
    }
}
