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

package oap.concurrent.pool;

import cn.danielw.fop.DisruptorObjectPool;
import cn.danielw.fop.ObjectFactory;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;
import io.micrometer.core.instrument.Metrics;
import oap.concurrent.LongAdder;
import oap.util.Dates;

public class AbstractObjectPool<T, Self extends AbstractObjectPool<T, Self>> {
    protected final DisruptorObjectPool<T> pool;
    protected final LongAdder used = new LongAdder();
    protected final PoolConfig config;

    protected AbstractObjectPool( ObjectFactory<T> objectFactory ) {
        this(
            objectFactory,
            Runtime.getRuntime().availableProcessors(),
            3000 / Runtime.getRuntime().availableProcessors(),
            5, Dates.m( 5 ), Dates.s( 5 ) );
    }

    /**
     * @param partitionSize
     * @param poolMinSize
     * @param poolMaxSize
     * @param maxIdle       objects idle for maxIdle ms will be destroyed to shrink the pool size
     * @param maxWait       when pool is full, wait at most maxWait ms, then throw an exception
     */
    protected AbstractObjectPool( ObjectFactory<T> objectFactory, int partitionSize, int poolMinSize, int poolMaxSize, long maxIdle, long maxWait ) {
        config = new PoolConfig();
        config.setPartitionSize( partitionSize );
        config.setMaxSize( poolMaxSize );
        config.setMinSize( poolMinSize );
        config.setMaxIdleMilliseconds( ( int ) maxIdle );
        config.setMaxWaitMilliseconds( ( int ) maxWait );

        pool = new DisruptorObjectPool<T>( config, objectFactory );
    }

    public Poolable<T> borrowObject( boolean blocking ) {
        used.increment();
        return pool.borrowObject( blocking );
    }

    public Poolable<T> borrowObject() {
        used.increment();
        return pool.borrowObject();
    }

    public void returnObject( Poolable<T> obj ) {
        used.decrement();
        pool.returnObject( obj );
    }

    public int getSize() {
        return pool.getSize();
    }

    @SuppressWarnings( "unchecked" )
    public Self withMetrics( String prefix ) {
        Metrics.gauge( prefix + "_pool_partition_size", config, p -> config.getPartitionSize() );
        Metrics.gauge( prefix + "_pool_min_size", config, p -> config.getMinSize() );
        Metrics.gauge( prefix + "_pool_max_size", config, p -> config.getMaxSize() );
        Metrics.gauge( prefix + "_pool_in_use", config, p -> used.intValue() );

        Metrics.gauge( prefix + "_pool_size", pool, p -> ( double ) p.getSize() );

        return ( Self ) this;
    }
}
