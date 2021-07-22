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

import cn.danielw.fop.ObjectFactory;
import oap.util.FastByteArrayOutputStream;

/**
 * Created by igor.petrenko on 02.05.2019.
 */
public final class FastByteArrayOutputStreamPool extends AbstractObjectPool<FastByteArrayOutputStream, FastByteArrayOutputStreamPool> {
    private static final ObjectFactory<FastByteArrayOutputStream> OBJECT_FACTORY = new ObjectFactory<>() {
        @Override
        public FastByteArrayOutputStream create() {
            return new FastByteArrayOutputStream();
        }

        @Override
        public void destroy( FastByteArrayOutputStream o ) {
        }

        @Override
        public boolean validate( FastByteArrayOutputStream o ) {
            o.reset();
            return true;
        }
    };

    public FastByteArrayOutputStreamPool( int partitionSize, int poolMinSize, int poolMaxSize, long maxIdle, long maxWait ) {
        super( OBJECT_FACTORY, partitionSize, poolMinSize, poolMaxSize, maxIdle, maxWait );
    }
}
