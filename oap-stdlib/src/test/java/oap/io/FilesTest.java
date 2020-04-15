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

import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Sets;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class FilesTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void wildcard() {
        Files.writeString( Env.tmp( "/wildcard/1.txt" ), "1" );
        assertThat( Files.wildcard( Env.tmp( "/wildcard" ), "*.txt" ) )
            .containsOnly( TestDirectoryFixture.testPath( "/wildcard/1.txt" ) );
        assertThat( Files.wildcard( "/aaa", "*.txt" ) ).isEmpty();

        Files.writeString( Env.tmp( "/wildcard/a/a/1.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/b/1.txt" ), "1" );
        assertThat( Files.wildcard( Env.tmp( "/wildcard" ), "**/*.txt" ) )
            .containsOnly(
                TestDirectoryFixture.testPath( "/wildcard/a/a/1.txt" ),
                TestDirectoryFixture.testPath( "/wildcard/b/1.txt" )
            );
    }

    @Test
    public void path() {
        assertEquals( Paths.get( "a", "b/c", "d" ), Paths.get( "a", "b", "c", "d" ) );
    }

    @Test
    public void copy() {
        Files.writeString( TestDirectoryFixture.testPath( "src/a/1.txt" ), "1" );
        Files.writeString( TestDirectoryFixture.testPath( "src/a/2.txt" ), "1" );
        Files.writeString( TestDirectoryFixture.testPath( "src/2.txt" ), "${x}" );
        if( !Resources.IS_WINDOWS )
            Files.setPosixPermissions( TestDirectoryFixture.testPath( "src/2.txt" ), OWNER_EXECUTE, OWNER_READ, OWNER_WRITE );

        Files.copyContent( TestDirectoryFixture.testPath( "src" ), TestDirectoryFixture.testPath( "all" ) );
        assertFile( TestDirectoryFixture.testPath( "all/a/1.txt" ) ).hasContent( "1" );
        assertFile( TestDirectoryFixture.testPath( "all/a/2.txt" ) ).hasContent( "1" );
        assertFile( TestDirectoryFixture.testPath( "all/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( TestDirectoryFixture.testPath( "all/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( TestDirectoryFixture.testPath( "src" ), TestDirectoryFixture.testPath( "selected" ), Lists.of( "**/2.txt" ), Lists.of() );
        assertFile( TestDirectoryFixture.testPath( "selected/a/2.txt" ) ).hasContent( "1" );
        assertFile( TestDirectoryFixture.testPath( "selected/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( TestDirectoryFixture.testPath( "selected/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( TestDirectoryFixture.testPath( "src" ), TestDirectoryFixture.testPath( "selected" ), Lists.of(), Lists.of( "**/1.txt" ) );
        assertFile( TestDirectoryFixture.testPath( "selected/a/2.txt" ) ).hasContent( "1" );
        assertFile( TestDirectoryFixture.testPath( "selected/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( TestDirectoryFixture.testPath( "selected/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( TestDirectoryFixture.testPath( "src" ), TestDirectoryFixture.testPath( "filtered" ), Lists.of( "**/2.txt" ), Lists.of(), true,
            macro -> "x".equals( macro ) ? "y" : macro );
        assertFile( TestDirectoryFixture.testPath( "filtered/a/2.txt" ) ).hasContent( "1" );
        assertFile( TestDirectoryFixture.testPath( "filtered/2.txt" ) ).hasContent( "y" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( TestDirectoryFixture.testPath( "filtered/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );
    }

    @Test
    public void isDirectoryEmpty() {
        Files.writeString( Env.tmp( "/wildcard/1.txt" ), "1" );

        assertThat( Files.isDirectoryEmpty( TestDirectoryFixture.testPath( "/wildcard" ) ) ).isFalse();

        Files.delete( TestDirectoryFixture.testPath( "/wildcard/1.txt" ) );

        assertThat( Files.isDirectoryEmpty( TestDirectoryFixture.testPath( "/wildcard" ) ) ).isTrue();
    }

    @Test
    public void wildcardMatch() {
        assertTrue( Files.wildcardMatch( "bid_v15-2016-07-13-08-02.tsv.lz4", "bid_v*-2016-07-13-08-02.tsv.*" ) );
        assertTrue( Files.wildcardMatch( "bid", "bid*" ) );
        assertTrue( Files.wildcardMatch( "bid_", "bid?" ) );
        assertTrue( Files.wildcardMatch( "bid_v", "*d_v" ) );

        assertFalse( Files.wildcardMatch( "bid_v", "bb" ) );
        assertFalse( Files.wildcardMatch( "b", "bb" ) );

        assertFalse( Files.wildcardMatch( "file.txt123", "file.txt" ) );
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
            Path path = TestDirectoryFixture.testPath( "file" + ext );
            Files.writeString( path, encoding, "value" );
            Files.ensureFileEncodingValid( path );
            if( IoStreams.Encoding.from( ext ) != encoding ) fail( "should throw exception" );
        } catch( InvalidFileEncodingException e ) {
            if( IoStreams.Encoding.from( ext ) == encoding ) fail( "should not throw exception: " + e );
        }
    }

    @Test
    public void move() {
        final Path path = TestDirectoryFixture.testPath( "file.txt" );
        final Path newPath = TestDirectoryFixture.testPath( "test/newFile.txt" );
        Files.writeString( path, "test" );
        Files.writeString( newPath, "test2" );
        Files.move( path, newPath, REPLACE_EXISTING );

        assertThat( path ).doesNotExist();
        assertThat( newPath ).exists();
        assertThat( newPath ).hasContent( "test" );
    }

    @Test
    public void deleteEmptyDirectories() {
        Files.writeString( Env.tmp( "/dir1/1.txt" ), "1" );
        Files.writeString( Env.tmp( "/dir1/dir2/1.txt" ), "1" );

        var dir3 = TestDirectoryFixture.testPath( "/dir1/dir3" );
        var dir4 = TestDirectoryFixture.testPath( "/dir1/dir3/dir4" );

        Files.ensureDirectory( dir3 );
        Files.ensureDirectory( dir4 );

        Files.deleteEmptyDirectories( TestDirectoryFixture.testPath( "/" ), false );

        assertThat( dir4 ).doesNotExist();
        assertThat( dir3 ).doesNotExist();
        assertThat( TestDirectoryFixture.testPath( "/dir1/dir2" ) ).exists();
        assertThat( Env.tmp( "/dir1/1.txt" ) ).contains( "1" );

        Files.ensureDirectory( dir3 );
        Files.ensureDirectory( dir4 );

        Files.deleteEmptyDirectories( dir3, false );
        assertThat( dir4 ).doesNotExist();
        assertThat( dir3 ).exists();

        Files.deleteEmptyDirectories( dir3, true );
        assertThat( dir3 ).doesNotExist();
    }
}
