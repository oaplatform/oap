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

package oap.logstream;

import oap.concurrent.Threads;
import oap.logstream.disk.DiskLoggingBackend;
import oap.logstream.net.SocketLoggingBackend;
import oap.logstream.net.SocketLoggingServer;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.io.IOException;

import static oap.io.IoAsserts.assertFileContent;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.logstream.disk.DiskLoggingBackend.DEFAULT_BUFFER;
import static oap.net.Inet.HOSTNAME;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LoggerTest extends AbstractTest {
    @Test
    public void disk() {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        String content = "12345678";
        try( LoggingBackend backend = new DiskLoggingBackend( Env.tmpPath( "logs" ), "log", DEFAULT_BUFFER, 12, false ) ) {
            Logger logger = new Logger( backend );
            logger.log( "a", content );
            logger.log( "b", content );
            logger.log( "a", content );
            logger.log( "d", content );
        }

        assertFileContent( Env.tmpPath( "logs/" + HOSTNAME + "/2015-10/10/a-2015-10-10-01-00.log" ),
            content + "\n" + content + "\n" );
        assertFileContent( Env.tmpPath( "logs/" + HOSTNAME + "/2015-10/10/b-2015-10-10-01-00.log" ),
            content + "\n" );
        assertFileContent( Env.tmpPath( "logs/" + HOSTNAME + "/2015-10/10/d-2015-10-10-01-00.log" ),
            content + "\n" );
    }

    @Test
    public void diskCompression() {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        String content = "12345678";
        try( LoggingBackend backend = new DiskLoggingBackend( Env.tmpPath( "logs" ), "gz", DEFAULT_BUFFER, 12, true ) ) {
            Logger logger = new Logger( backend );
            logger.log( "a", content );
            logger.log( "b", content );
            logger.log( "a", content );
            logger.log( "d", content );
        }

        assertFileContent( Env.tmpPath( "logs/" + HOSTNAME + "/2015-10/10/a-2015-10-10-01-00.gz" ), GZIP,
            content + "\n" + content + "\n" );
        assertFileContent( Env.tmpPath( "logs/" + HOSTNAME + "/2015-10/10/b-2015-10-10-01-00.gz" ), GZIP,
            content + "\n" );
        assertFileContent( Env.tmpPath( "logs/" + HOSTNAME + "/2015-10/10/d-2015-10-10-01-00.gz" ), GZIP,
            content + "\n" );
    }

    @Test
    public void net() throws IOException {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        String content = "12345678";

        try( LoggingBackend serverBackend =
                 new DiskLoggingBackend( Env.tmpPath( "logs" ), "log", DEFAULT_BUFFER, 12, false ) ) {
            SocketLoggingServer server = new SocketLoggingServer( 7777, 1024, serverBackend, Env.tmpPath( "control" ) );
            try( SocketLoggingBackend clientBackend = new SocketLoggingBackend( "localhost", 7777,
                Env.tmpPath( "buffers" ), 25 ) ) {
                Logger logger = new Logger( clientBackend );
                logger.log( "a", content );
                clientBackend.send();
                assertFalse( logger.isLoggingAvailable() );
                server.start();
                Threads.sleepSafely( 100 );
                clientBackend.send();
                assertTrue( logger.isLoggingAvailable() );
                logger.log( "b", content );
                logger.log( "a", content );
                logger.log( "d", content );
                clientBackend.send();
            } finally {
                server.stop();
            }
        }
        assertFileContent( Env.tmpPath( "logs/localhost/2015-10/10/a-2015-10-10-01-00.log" ),
            content + "\n" + content + "\n" );
        assertFileContent( Env.tmpPath( "logs/localhost/2015-10/10/b-2015-10-10-01-00.log" ),
            content + "\n" );
        assertFileContent( Env.tmpPath( "logs/localhost/2015-10/10/d-2015-10-10-01-00.log" ),
            content + "\n" );
    }


}
