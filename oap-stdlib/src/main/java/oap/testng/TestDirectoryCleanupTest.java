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

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static oap.testng.TestDirectory.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is utility for teamcity to clean up orphaned test dirs
 */
@Slf4j
public class TestDirectoryCleanupTest {
    private static final long TEN_HOURS = 1000 * 60 * 60 * 10;

    @Test
    public void test() {}

    @AfterClass
    public void checkForPollution() throws IOException {
        long count = Files.list( Env.tmpRoot ).count();
        assertThat( count )
            .withFailMessage( "POLLUTION DETECTED, the previous test left test directory with content" )
            .isEqualTo( 0 );
    }

    @AfterSuite
    public void tryGeneralCleanup() {
        final long now = System.currentTimeMillis();
        boolean empty = true;
        if( !java.nio.file.Files.exists( Env.tmp ) ) return;

        try( var stream = java.nio.file.Files.newDirectoryStream( Env.tmp ) ) {
            for( Path build : stream ) {
                boolean self = Env.tmpRoot.equals( build );
                long lastModified = java.nio.file.Files.getLastModifiedTime( build ).toMillis();
                long diff = now - lastModified;
                if( self || diff > TEN_HOURS ) {
                    log.info( "delete {}", build );
                    deleteDirectory( build );
                } else {
                    log.info( "skip {}, diff = {}", build, diff );
                    log.trace( "build={}", build );
                    log.trace( "now={}, lastModified={}", now, lastModified );
                    empty = false;
                }
            }
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }

        if( empty ) deleteDirectory( Env.tmp );
    }
}
