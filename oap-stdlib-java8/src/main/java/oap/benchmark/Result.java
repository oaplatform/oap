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

package oap.benchmark;

import java.util.List;

class Result {
    public long rate;
    public long time;

    public Result( long time, long rate ) {
        this.time = time;
        this.rate = rate;
    }

    public static Result average( List<Result> results, int experiments ) {
        return new Result(
            results.stream()
                .skip( experiments > 1 ? 1 : 0 )
                .mapToLong( r -> r.time )
                .sum() / ( experiments > 1 ? experiments - 1 : experiments ),
            results.stream()
                .skip( experiments > 1 ? 1 : 0 )
                .mapToLong( r1 -> r1.rate )
                .sum() / ( experiments > 1 ? experiments - 1 : experiments )
        );
    }
}
