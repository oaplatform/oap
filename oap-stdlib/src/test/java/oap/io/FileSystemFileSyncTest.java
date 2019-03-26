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

package oap.io;

import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemFileSyncTest extends AbstractTest {
    @Test
    public void testSync() throws Exception {
        StringBuilder b = new StringBuilder();

        var remoteFile = Env.tmpPath( "rtest.file" ).toUri();
        var localFile = Env.tmpPath( "ltest.file" );

        Files.writeString( Paths.get( remoteFile ), "test" );

        Files.setLastModifiedTime( Paths.get( remoteFile ), 10 );

        final FileSync fileSync = FileSync.create( remoteFile, localFile );
        fileSync.addListener( path -> b.append( "f" ) );
        fileSync.run();

        assertThat( localFile ).hasContent( "test" );
        assertThat( java.nio.file.Files.getLastModifiedTime( localFile ).toMillis() ).isEqualTo( 10L );
        assertThat( b ).contains( "f" );

        Files.writeString( Paths.get( remoteFile ), "test2" );
        Files.setLastModifiedTime( Paths.get( remoteFile ), 10 );

        fileSync.run();
        assertThat( localFile ).hasContent( "test" );
        assertThat( b ).contains( "f" );

        Files.setLastModifiedTime( Paths.get( remoteFile ), 20L );
        fileSync.run();
        assertThat( localFile ).hasContent( "test2" );
        assertThat( b ).contains( "ff" );
    }

}
