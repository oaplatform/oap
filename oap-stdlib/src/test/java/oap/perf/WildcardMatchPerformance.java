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

package oap.perf;

import oap.io.Files;
import oap.testng.AbstractPerformance;
import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.Test;

@Test( enabled = false )
public class WildcardMatchPerformance extends AbstractPerformance {
    @Test( enabled = false )
    public void perf() {
        benchmark( "FilenameUtils.wildcardMatch", 10000000, 5, ( i ) -> {
            FilenameUtils.wildcardMatch( "bid_v15-2016-07-13-08-02.tsv.lz4", "bid_v*-2016-07-13-08-02.tsv.*" );
        } );

        benchmark( "wildcardMatch", 10000000, 5, ( i ) -> {
            Files.wildcardMatch( "bid_v15-2016-07-13-08-02.tsv.lz4", "bid_v*-2016-07-13-08-02.tsv.*" );
        } );
    }
}
