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

package oap.metrics;

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Created by Igor Petrenko on 01.12.2015.
 */
public class ReporterFilterTest extends AbstractTest {

    @Test
    public void testMatchesExclude() throws Exception {
        final ReporterFilter reporterFilter = new ReporterFilter();
        reporterFilter.exclude.add( Pattern.compile( "^test.*" ) );

        assertFalse( reporterFilter.matches( "test.test", null ) );
        assertTrue( reporterFilter.matches( "tes.test", null ) );
    }

    @Test
    public void testMatchesInclude() throws Exception {
        final ReporterFilter reporterFilter = new ReporterFilter();
        reporterFilter.include.add( Pattern.compile( "^test.*" ) );

        assertTrue( reporterFilter.matches( "test.test", null ) );
        assertFalse( reporterFilter.matches( "tes.test", null ) );
    }

    @Test
    public void testMatchesExcludeVsInclude() throws Exception {
        final ReporterFilter reporterFilter = new ReporterFilter();
        reporterFilter.include.add( Pattern.compile( "^test.*" ) );
        reporterFilter.exclude.add( Pattern.compile( "^testt.*" ) );

        assertTrue( reporterFilter.matches( "test.test", null ) );
        assertFalse( reporterFilter.matches( "tes.test", null ) );
        assertFalse( reporterFilter.matches( "testt", null ) );
    }
}