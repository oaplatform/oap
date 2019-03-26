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
import oap.util.Lists;
import oap.util.Sets;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.LZ4;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.io.IoStreams.Encoding.ZIP;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Env.tmpPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;


public class FilesTest extends AbstractTest {
    @Test
    public void wildcard() {
        Files.writeString( Env.tmp( "/wildcard/1.txt" ), "1" );
        assertThat( Files.wildcard( Env.tmp( "/wildcard" ), "*.txt" ) )
            .containsOnly( Env.tmpPath( "/wildcard/1.txt" ) );
        assertThat( Files.wildcard( "/aaa", "*.txt" ) ).isEmpty();

        Files.writeString( Env.tmp( "/wildcard/a/1.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/b/1.txt" ), "1" );
        Files.wildcard( Env.tmp( "/wildcard" ), "*/*.txt" ).forEach( System.out::println );
        assertThat( Files.wildcard( Env.tmp( "/wildcard" ), "*/*.txt" ) )
            .containsOnly(
                Env.tmpPath( "/wildcard/a/1.txt" ),
                Env.tmpPath( "/wildcard/b/1.txt" )
            );
    }

    @Test
    public void path() {
        assertEquals( Paths.get( "a", "b/c", "d" ), Paths.get( "a", "b", "c", "d" ) );
    }

    @Test
    public void copy() {
        Files.writeString( Env.tmpPath( "src/a/1.txt" ), "1" );
        Files.writeString( Env.tmpPath( "src/a/2.txt" ), "1" );
        Files.writeString( Env.tmpPath( "src/2.txt" ), "${x}" );
        if( !Resources.IS_WINDOWS )
            Files.setPosixPermissions( Env.tmpPath( "src/2.txt" ), OWNER_EXECUTE, OWNER_READ, OWNER_WRITE );

        Files.copyContent( Env.tmpPath( "src" ), Env.tmpPath( "all" ) );
        assertFile( Env.tmpPath( "all/a/1.txt" ) ).hasContent( "1" );
        assertFile( Env.tmpPath( "all/a/2.txt" ) ).hasContent( "1" );
        assertFile( Env.tmpPath( "all/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( Env.tmpPath( "all/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( Env.tmpPath( "src" ), Env.tmpPath( "selected" ), Lists.of( "**/2.txt" ), Lists.of() );
        assertFile( Env.tmpPath( "selected/a/2.txt" ) ).hasContent( "1" );
        assertFile( Env.tmpPath( "selected/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( Env.tmpPath( "selected/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( Env.tmpPath( "src" ), Env.tmpPath( "selected" ), Lists.of(), Lists.of( "**/1.txt" ) );
        assertFile( Env.tmpPath( "selected/a/2.txt" ) ).hasContent( "1" );
        assertFile( Env.tmpPath( "selected/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( Env.tmpPath( "selected/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( Env.tmpPath( "src" ), Env.tmpPath( "filtered" ), Lists.of( "**/2.txt" ), Lists.of(), true,
            macro -> "x".equals( macro ) ? "y" : macro );
        assertFile( Env.tmpPath( "filtered/a/2.txt" ) ).hasContent( "1" );
        assertFile( Env.tmpPath( "filtered/2.txt" ) ).hasContent( "y" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( Env.tmpPath( "filtered/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );
    }

    @Test
    public void isDirectoryEmpty() {
        Files.writeString( Env.tmp( "/wildcard/1.txt" ), "1" );

        assertThat( Files.isDirectoryEmpty( Env.tmpPath( "/wildcard" ) ) ).isFalse();

        Files.delete( Env.tmpPath( "/wildcard/1.txt" ) );

        assertThat( Files.isDirectoryEmpty( Env.tmpPath( "/wildcard" ) ) ).isTrue();
    }

    @Test
    public void wildcardMatch() {
        assertThat( Files.wildcardMatch( "bid_v15-2016-07-13-08-02.tsv.lz4", "bid_v*-2016-07-13-08-02.tsv.*" ) ).isTrue();
        assertThat( Files.wildcardMatch( "bid", "bid*" ) ).isTrue();
        assertThat( Files.wildcardMatch( "bid_", "bid?" ) ).isTrue();
        assertThat( Files.wildcardMatch( "bid_v", "*d_v" ) ).isTrue();

        assertThat( Files.wildcardMatch( "bid_v", "bb" ) ).isFalse();
        assertThat( Files.wildcardMatch( "b", "bb" ) ).isFalse();

    }

    @DataProvider
    public Object[][] variants() {
        return new Object[][] {
            { ".txt", PLAIN },
            { ".txt.gz", GZIP },
            { ".txt.gz", PLAIN },
            { ".zip", ZIP },
            { ".zip", PLAIN },
            { ".txt.lz4", LZ4 },
            { ".txt.lz4", PLAIN }
        };
    }

    @Test( dataProvider = "variants" )
    public void ensureFileEncodingValid( String ext, IoStreams.Encoding encoding ) {
        try {
            Path path = tmpPath( "file" + ext );
            Files.writeString( path, encoding, "value" );
            Files.ensureFileEncodingValid( path );
            if( IoStreams.Encoding.from( ext ) != encoding ) fail( "should throw exception" );
        } catch( InvalidFileEncodingException e ) {
            if( IoStreams.Encoding.from( ext ) == encoding ) fail( "should not throw exception: " + e );
        }
    }

    @Test
    public void testMove() {
        final Path path = tmpPath( "file.txt" );
        final Path newPath = tmpPath( "test/newFile.txt" );
        Files.writeString( path, "test" );
        Files.writeString( newPath, "test2" );
        Files.move( path, newPath, REPLACE_EXISTING );

        assertThat( path ).doesNotExist();
        assertThat( newPath ).exists();
        assertThat( newPath ).hasContent( "test" );
    }

    @Test
    public void testDeleteEmptyDirectories() throws IOException {
        Files.writeString( Env.tmp( "/dir1/1.txt" ), "1" );
        Files.writeString( Env.tmp( "/dir1/dir2/1.txt" ), "1" );

        var dir3 = Env.tmpPath( "/dir1/dir3" );
        var dir4 = Env.tmpPath( "/dir1/dir3/dir4" );

        java.nio.file.Files.createDirectories( dir3 );
        java.nio.file.Files.createDirectories( dir4 );

        Files.deleteEmptyDirectories( Env.tmpPath( "/" ), false );

        assertThat( dir4 ).doesNotExist();
        assertThat( dir3 ).doesNotExist();
        assertThat( Env.tmpPath( "/dir1/dir2" ) ).exists();
        assertThat( Env.tmp( "/dir1/1.txt" ) ).contains( "1" );

        java.nio.file.Files.createDirectories( dir3 );
        java.nio.file.Files.createDirectories( dir4 );

        Files.deleteEmptyDirectories( dir3, false );
        assertThat( dir4 ).doesNotExist();
        assertThat( dir3 ).exists();

        Files.deleteEmptyDirectories( dir3, true );
        assertThat( dir3 ).doesNotExist();
    }
}
