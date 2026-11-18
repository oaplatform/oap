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

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ShardControllerTest {
    private static final long WINDOW = TimeUnit.SECONDS.toNanos( 10 );
    private static final long GROW_WAIT = TimeUnit.MILLISECONDS.toNanos( 5 );

    /**
     * 100 appends of a window that waited {@code meanMillis} each on average with {@code shards} shards held at once
     */
    private static ShardController.Window waiting( long meanMillis, int shards ) {
        return new ShardController.Window( 100, 100 * TimeUnit.MILLISECONDS.toNanos( meanMillis ), shards, WINDOW );
    }

    /**
     * 100 appends of a window without waits that held at most {@code shards} shards at once
     */
    private static ShardController.Window peak( int shards ) {
        return new ShardController.Window( 100, 0, shards, WINDOW );
    }

    @Test
    public void theShardsDoubleUpToTheirLimitWhileAppendsWaitTooLong() {
        var controller = new ShardController( 8, GROW_WAIT, 3 );

        assertThat( controller.next( 1, waiting( 6, 1 ) ) ).isEqualTo( 2 );
        assertThat( controller.next( 2, waiting( 6, 2 ) ) ).isEqualTo( 4 );
        assertThat( controller.next( 4, waiting( 6, 4 ) ) ).isEqualTo( 8 );
        assertThat( controller.next( 8, waiting( 6, 8 ) ) ).isEqualTo( 8 );
        // a mean wait at the threshold is not too long
        assertThat( controller.next( 4, waiting( 5, 4 ) ) ).isEqualTo( 4 );
    }

    @Test
    public void theShardsDropByOneAfterEnoughQuietWindows() {
        var controller = new ShardController( 8, GROW_WAIT, 3 );

        // 4 shards, never more than 3 held at once: one fewer would have served every append
        assertThat( controller.next( 4, peak( 3 ) ) ).isEqualTo( 4 );
        assertThat( controller.next( 4, peak( 3 ) ) ).isEqualTo( 4 );
        assertThat( controller.next( 4, peak( 3 ) ) ).isEqualTo( 3 );
        // the count of quiet windows starts afresh for 3 shards
        assertThat( controller.next( 3, peak( 1 ) ) ).isEqualTo( 3 );
        assertThat( controller.next( 3, peak( 1 ) ) ).isEqualTo( 3 );
        assertThat( controller.next( 3, peak( 1 ) ) ).isEqualTo( 2 );
    }

    @Test
    public void allShardsHeldAtOnceKeepThemEvenWhenTheyWereHeldBriefly() {
        var controller = new ShardController( 8, GROW_WAIT, 3 );

        // the shards are held only for a moment and the mean wait stays under the threshold, but a burst needed all
        // four: dropping one would make the next burst wait
        for( int window = 0; window < 10; window++ ) assertThat( controller.next( 4, waiting( 1, 4 ) ) ).isEqualTo( 4 );
    }

    @Test
    public void aWindowWithAllShardsHeldOrAGrowthStartsTheQuietWindowsAfresh() {
        var controller = new ShardController( 8, GROW_WAIT, 3 );

        controller.next( 4, peak( 1 ) );
        controller.next( 4, peak( 1 ) );
        assertThat( controller.next( 4, peak( 4 ) ) ).isEqualTo( 4 );
        controller.next( 4, peak( 1 ) );
        assertThat( controller.next( 4, peak( 1 ) ) ).isEqualTo( 4 );
        assertThat( controller.next( 4, waiting( 10, 4 ) ) ).isEqualTo( 8 );
        controller.next( 8, peak( 1 ) );
        assertThat( controller.next( 8, peak( 1 ) ) ).isEqualTo( 8 );
        assertThat( controller.next( 8, peak( 1 ) ) ).isEqualTo( 7 );
    }

    @Test
    public void aWindowWithoutAppendsChangesNothing() {
        var controller = new ShardController( 8, GROW_WAIT, 1 );

        assertThat( controller.next( 4, new ShardController.Window( 0, 0, 0, WINDOW ) ) ).isEqualTo( 4 );
        assertThat( controller.next( 4, new ShardController.Window( 0, 0, 0, 0 ) ) ).isEqualTo( 4 );
    }

    @Test
    public void oneShardIsTheLeast() {
        var controller = new ShardController( 8, GROW_WAIT, 1 );

        assertThat( controller.next( 1, peak( 0 ) ) ).isEqualTo( 1 );
        assertThat( controller.next( 2, peak( 1 ) ) ).isEqualTo( 1 );
    }

    @Test
    public void oneShardAtMostNeverGrows() {
        var controller = new ShardController( 1, GROW_WAIT, 1 );

        assertThat( controller.next( 1, waiting( 1_000, 1 ) ) ).isEqualTo( 1 );
    }

    @Test
    public void invalidSettingsAreRejected() {
        assertThatThrownBy( () -> new ShardController( 0, GROW_WAIT, 6 ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> new ShardController( 8, -1, 6 ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> new ShardController( 8, GROW_WAIT, 0 ) ).isInstanceOf( IllegalArgumentException.class );
    }
}
