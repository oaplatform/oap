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
package oap.testng;

import oap.io.Files;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;

public abstract class AbstractTest {
    static {
        DateTimeZone.setDefault( DateTimeZone.UTC );
    }

    protected boolean CLEANUP_TEMP = true;

    @AfterSuite
    public void afterSuite() {
        if( CLEANUP_TEMP ) Files.delete( Env.tmp );
    }

    @AfterClass
    public void afterClass() {
        if( CLEANUP_TEMP ) Files.delete( Env.tmpRoot );
    }

    @BeforeMethod
    public void beforeMethod() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @AfterMethod
    public void afterMethod() throws IOException {
        afterMethod( true );
    }

    protected void afterMethod( boolean cleanup ) throws IOException {
        if( CLEANUP_TEMP && cleanup ) Files.delete( Env.tmpRoot );
        DateTimeUtils.setCurrentMillisSystem();
    }
}
