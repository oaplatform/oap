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
 * When the data written to a log file is flushed to the device (fdatasync).
 * A process crash loses nothing in any mode (the data is in the page cache, the recovery truncates a torn tail);
 * the modes differ in what an OS crash / power loss can lose.
 */
public enum SyncMode {
    /**
     * every buffer is synced before the write returns (before the client gets the ACK). Nothing acknowledged is lost.
     */
    ALWAYS,
    /**
     * the data is synced every {@code syncInterval} and when the file is closed.
     * An OS crash loses up to {@code syncInterval} of acknowledged data.
     */
    PERIODIC,
    /**
     * the data is synced only when the file is closed (rotation, shutdown).
     */
    ON_CLOSE
}
