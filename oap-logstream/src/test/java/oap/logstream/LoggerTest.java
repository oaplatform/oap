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

import lombok.val;
import oap.io.IoStreams.Encoding;
import oap.logstream.disk.DiskLoggerBackend;
import oap.logstream.net.SocketLoggerBackend;
import oap.logstream.net.SocketLoggerServer;
import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.nio.file.Files;

import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_BUFFER;
import static oap.logstream.disk.DiskLoggerBackend.DEFAULT_FREE_SPACE_REQUIRED;
import static oap.net.Inet.HOSTNAME;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Env.tmpPath;
import static oap.util.Dates.formatDateWithMillis;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LoggerTest extends AbstractTest {
    @Test
    public void disk() {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        String content = "12345678";
        try( DiskLoggerBackend backend = new DiskLoggerBackend( tmpPath( "logs" ), Timestamp.BPH_12, DEFAULT_BUFFER ) ) {
            Logger logger = new Logger( backend );
            logger.log( "lfn1", "lft", 1, content );
            logger.log( "lfn2", "lft", 1, content );
            logger.log( "lfn1", "lft", 1, content );
            logger.log( "lfn1", "lft1", 1, content );
        }

        assertFile( tmpPath( "logs/" + HOSTNAME + "2015-10/10/a-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n"
                + formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n", Encoding.GZIP );
        assertFile( tmpPath( "logs/" + HOSTNAME + "/2015-10/10/b-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n", Encoding.GZIP );
        assertFile( tmpPath( "logs/" + HOSTNAME + "/2015-10/10/d-2015-10-10-01-00.tsv.gz" ) )
            .hasContent( formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n", Encoding.GZIP );
    }

    @Test
    public void net() {
        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        String content = "12345678";

        try( val serverBackend = new DiskLoggerBackend( tmpPath( "logs" ), Timestamp.BPH_12, DEFAULT_BUFFER ) ) {
            SocketLoggerServer server = new SocketLoggerServer( Env.port( "net" ), 1024, serverBackend, tmpPath( "control" ) );
            try( val clientBackend = new SocketLoggerBackend( ( byte ) 1, "localhost", Env.port( "net" ),
                tmpPath( "buffers" ), 50 ) ) {

                serverBackend.requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED * 1000L;
                assertFalse( serverBackend.isLoggingAvailable() );
                val logger = new Logger( clientBackend );
                logger.log( "lfn1", "lft", 1, content );
                clientBackend.send();
                assertFalse( logger.isLoggingAvailable() );
                server.start();
                clientBackend.send();
                assertFalse( logger.isLoggingAvailable() );
                serverBackend.requiredFreeSpace = DEFAULT_FREE_SPACE_REQUIRED;
                assertTrue( serverBackend.isLoggingAvailable() );
                clientBackend.send();
                assertTrue( logger.isLoggingAvailable() );
                logger.log( "lfn2", "lft", 1, content );
                logger.log( "lfn1", "lft", 1, content );
                logger.log( "lfn1", "lft3", 1, content );
                clientBackend.send();
            } finally {
                server.stop();
            }
        }

        String localhost = "localhost";
        if( !Files.exists( tmpPath( "logs/localhost/2015-10/10/a-2015-10-10-01-00.log" ) ) ) {
            localhost = "127.0.0.1";
        }

        assertFile( tmpPath( "logs/" + localhost + "/2015-10/10/a-2015-10-10-01-00.log" ) )
            .hasContent( formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n"
                + formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n" );
        assertFile( tmpPath( "logs/" + localhost + "/2015-10/10/b-2015-10-10-01-00.log" ) )
            .hasContent( formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n" );
        assertFile( tmpPath( "logs/" + localhost + "/2015-10/10/d-2015-10-10-01-00.log" ) )
            .hasContent( formatDateWithMillis( currentTimeMillis() ) + "\t" + content + "\n" );
    }
}
