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

package oap.concurrent;

import cn.danielw.fop.DisruptorObjectPool;
import cn.danielw.fop.ObjectFactory;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.EnvironmentConfiguration;

/**
 * Created by igor.petrenko on 01.05.2019.
 * <p>
 * env:
 * - POOL_PARTITION_SIZE: 20
 * - POOL_MAX_SIZE: 150
 * - POOL_MIN_SIZE: 5
 * - POOL_MAX_IDLE_MILLISECONDS: 300000
 */
public final class StringBuilderPool {
    private static final DisruptorObjectPool<StringBuilder> pool;

    static {
        var envConfig = new EnvironmentConfiguration();
        PoolConfig config = new PoolConfig();
        config.setPartitionSize( envConfig.getInt( "POOL_PARTITION_SIZE", 20 ) );
        config.setMaxSize( envConfig.getInt( "POOL_MAX_SIZE", 3000 / 20 ) );
        config.setMinSize( envConfig.getInt( "POOL_MIN_SIZE", 5 ) );
        config.setMaxIdleMilliseconds( envConfig.getInt( "POOL_MAX_IDLE_MILLISECONDS", 60 * 1000 * 5 ) );

        ObjectFactory<StringBuilder> factory = new ObjectFactory<>() {
            @Override
            public StringBuilder create() {
                return new StringBuilder();
            }

            @Override
            public void destroy( StringBuilder o ) {
            }

            @Override
            public boolean validate( StringBuilder o ) {
                o.delete( 0, o.length() );
                return true;
            }
        };

        pool = new DisruptorObjectPool<>( config, factory );

        if( envConfig.getBoolean( "METRICS_StringBuilderPool", false ) ) {
            Metrics.gauge( "StringBuilderPool.partition_size", config, p -> config.getPartitionSize() );
            Metrics.gauge( "StringBuilderPool.min_size", config, p -> config.getMinSize() );
            Metrics.gauge( "StringBuilderPool.max_size", config, p -> config.getMaxSize() );

            Metrics.gauge( "StringBuilderPool.size", pool, p -> ( double ) p.getSize() );
        }
    }

    private StringBuilderPool() {
    }

    @SneakyThrows
    public static Poolable<StringBuilder> borrowObject() {
        return pool.borrowObject();
    }

    public static int getSize() {
        return pool.getSize();
    }
}
