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

package oap.concurrent.stringbuilder;

import cn.danielw.fop.Poolable;
import oap.concurrent.pool.StringBuilderPool;

public class ThreadLocalStringBuilderPool {
    private final ThreadLocal<StringBuilderPool> pool;
    private String prefix = null;

    public ThreadLocalStringBuilderPool( int partitionSize, int poolMinSize, int poolMaxSize, long maxIdle, long maxWait ) {
        pool = ThreadLocal.withInitial( () -> new StringBuilderPool( partitionSize, poolMinSize, poolMaxSize, maxIdle, maxWait ).withMethrics( prefix + ":" + Thread.currentThread().getName() ) );
    }


    public Poolable<StringBuilder> borrowObject( boolean blocking ) {
        return pool.get().borrowObject( blocking );
    }

    public Poolable<StringBuilder> borrowObject() {
        return pool.get().borrowObject();
    }

    public int getSize() {
        return pool.get().getSize();
    }

    public ThreadLocalStringBuilderPool withMethrics( String prefix ) {
        this.prefix = prefix;
        return this;
    }

}
