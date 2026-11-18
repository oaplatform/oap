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

/**
 * Chooses how many shards {@link WriterShards} appends to, from what its appends did during one window
 * ({@link DiskLoggerBackend#refreshPeriod}). The count doubles, up to {@code maxShards}, once a mean append has waited
 * longer than {@code growWaitNanos} for its shard. It drops by one after {@code shrinkWindows} consecutive windows in
 * which fewer shards than in use were ever held at once: one shard fewer would have served every append without a wait.
 * A window without appends changes nothing. Not thread-safe: one caller at a time.
 * <p>
 * The shrink signal is the peak of shards held at once, not the time they were held: an append holds its shard only
 * to write into the page cache and is made durable outside the shard lock, so the mean hold of a window says nothing
 * about the bursts that make appends wait.
 */
final class ShardController {
    private final int maxShards;
    private final long growWaitNanos;
    private final int shrinkWindows;
    /**
     * consecutive quiet windows so far
     */
    private int quietWindows;

    ShardController( int maxShards, long growWaitNanos, int shrinkWindows ) {
        if( maxShards < 1 ) throw new IllegalArgumentException( "maxShards must be positive: " + maxShards );
        if( growWaitNanos < 0 ) throw new IllegalArgumentException( "shardGrowWait must not be negative: " + growWaitNanos );
        if( shrinkWindows < 1 ) throw new IllegalArgumentException( "shardShrinkWindows must be positive: " + shrinkWindows );
        this.maxShards = maxShards;
        this.growWaitNanos = growWaitNanos;
        this.shrinkWindows = shrinkWindows;
    }

    /**
     * the shard count for the next window
     */
    int next( int current, Window window ) {
        assert current >= 1 && current <= maxShards : "shard count out of range: " + current;
        if( window.appends() == 0 ) return current;
        if( window.waitNanos() / window.appends() > growWaitNanos ) {
            quietWindows = 0;
            return Math.min( maxShards, current * 2 );
        }
        if( current > 1 && window.peakShards() < current ) {
            if( ++quietWindows < shrinkWindows ) return current;
            quietWindows = 0;
            return current - 1;
        }
        quietWindows = 0;
        return current;
    }

    /**
     * What the appends of one window did.
     *
     * @param appends     appends
     * @param waitNanos   time the appends waited for a busy shard, summed
     * @param peakShards  the most shards held at once
     * @param windowNanos length of the window
     */
    record Window( long appends, long waitNanos, int peakShards, long windowNanos ) {
        Window {
            assert appends >= 0 && waitNanos >= 0 && peakShards >= 0 && windowNanos >= 0 : "negative window statistics";
        }
    }
}
