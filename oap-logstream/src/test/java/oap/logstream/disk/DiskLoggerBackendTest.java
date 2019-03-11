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

import lombok.val;
import oap.dictionary.LogConfiguration;
import oap.logstream.Timestamp;
import oap.template.Engine;
import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.Test;

import java.nio.file.Paths;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DiskLoggerBackendTest extends AbstractTest {
    @Test
    public void spaceAvailable() {
        val engine = new Engine( Paths.get( "/tmp/file-cache" ), 1000 * 60 * 60 * 24 );
        val logConfiguration = new LogConfiguration( engine, null, "test-logconfig" );
        try( DiskLoggerBackend backend = new DiskLoggerBackend( Env.tmpPath( "logs" ), Timestamp.BPH_12, 4000, logConfiguration ) ) {
            assertTrue( backend.isLoggingAvailable() );
            backend.requiredFreeSpace *= 1000;
            assertFalse( backend.isLoggingAvailable() );
            backend.requiredFreeSpace /= 1000;
            assertTrue( backend.isLoggingAvailable() );
        }
    }
}
