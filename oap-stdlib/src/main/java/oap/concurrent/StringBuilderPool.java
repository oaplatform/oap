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

import lombok.SneakyThrows;
import stormpot.Allocator;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.Pool;
import stormpot.Poolable;
import stormpot.Slot;
import stormpot.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * Created by igor.petrenko on 01.05.2019.
 */
public final class StringBuilderPool {
    private static Pool<StringBuilderPoolable> pool;
    private static Timeout timeout;

    static {
        var allocator = new StringBuilderAllocator();
        var config = new Config<StringBuilderPoolable>()
            .setAllocator( allocator );
        pool = new BlazePool<>( config );
        timeout = new Timeout( 10, TimeUnit.SECONDS );
    }

    private StringBuilderPool() {
    }

    @SneakyThrows
    public static StringBuilderPoolable claim() {
        return pool.claim( timeout );
    }

    private static class StringBuilderAllocator implements Allocator<StringBuilderPoolable> {

        @Override
        public StringBuilderPoolable allocate( Slot slot ) {
            return new StringBuilderPoolable( slot );
        }

        @Override
        public void deallocate( StringBuilderPoolable stringBuilder ) {
            stringBuilder.release();
        }
    }

    public static class StringBuilderPoolable implements Poolable {
        public final StringBuilder sb = new StringBuilder();
        private final Slot slot;

        public StringBuilderPoolable( Slot slot ) {
            this.slot = slot;
        }

        @Override
        public void release() {
            slot.release( this );
        }
    }
}
