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

import oap.io.content.ContentWriter;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.Lists;
import oap.util.Sets;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.LZ4;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.io.IoStreams.Encoding.ZIP;
import static oap.io.content.ContentWriter.ofString;
import static oap.net.Inet.HOSTNAME;
import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class FilesTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public FilesTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture( getClass() ) );
        fixture( new SystemTimerFixture( getClass() ) );
    }

    @Test
    public void wildcard() {
        Files.write( testDirectoryFixture.testPath( "/wildcard/1.txt" ), "1", ContentWriter.ofString() );
        assertThat( Files.wildcard( testDirectoryFixture.testPath( "/wildcard" ), "*.txt" ) )
            .containsOnly( testDirectoryFixture.testPath( "/wildcard/1.txt" ) );
        assertThat( Files.wildcard( "/aaa", "*.txt" ) ).isEmpty();

        Files.write( testDirectoryFixture.testPath( "/wildcard/a/a/1.txt" ), "1", ContentWriter.ofString() );
        Files.write( testDirectoryFixture.testPath( "/wildcard/b/1.txt" ), "1", ContentWriter.ofString() );
        assertThat( Files.wildcard( testDirectoryFixture.testPath( "/wildcard" ), "**/*.txt" ) )
            .containsOnly(
                testDirectoryFixture.testPath( "/wildcard/a/a/1.txt" ),
                testDirectoryFixture.testPath( "/wildcard/b/1.txt" )
            );
    }

    @Test
    public void path() {
        assertEquals( Paths.get( "a", "b/c", "d" ), Paths.get( "a", "b", "c", "d" ) );
    }

    @Test
    public void copy() {
        Files.write( testDirectoryFixture.testPath( "src/a/1.txt" ), "1", ContentWriter.ofString() );
        Files.write( testDirectoryFixture.testPath( "src/a/2.txt" ), "1", ContentWriter.ofString() );
        Files.write( testDirectoryFixture.testPath( "src/2.txt" ), "${x}", ContentWriter.ofString() );
        if( !Resources.IS_WINDOWS )
            Files.setPosixPermissions( testDirectoryFixture.testPath( "src/2.txt" ), OWNER_EXECUTE, OWNER_READ, OWNER_WRITE );

        Files.copyContent( testDirectoryFixture.testPath( "src" ), testDirectoryFixture.testPath( "all" ) );
        assertFile( testDirectoryFixture.testPath( "all/a/1.txt" ) ).hasContent( "1" );
        assertFile( testDirectoryFixture.testPath( "all/a/2.txt" ) ).hasContent( "1" );
        assertFile( testDirectoryFixture.testPath( "all/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( testDirectoryFixture.testPath( "all/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( testDirectoryFixture.testPath( "src" ), testDirectoryFixture.testPath( "selected" ), Lists.of( "**/2.txt" ), Lists.of() );
        assertFile( testDirectoryFixture.testPath( "selected/a/2.txt" ) ).hasContent( "1" );
        assertFile( testDirectoryFixture.testPath( "selected/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( testDirectoryFixture.testPath( "selected/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( testDirectoryFixture.testPath( "src" ), testDirectoryFixture.testPath( "selected" ), Lists.of(), Lists.of( "**/1.txt" ) );
        assertFile( testDirectoryFixture.testPath( "selected/a/2.txt" ) ).hasContent( "1" );
        assertFile( testDirectoryFixture.testPath( "selected/2.txt" ) ).hasContent( "${x}" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( testDirectoryFixture.testPath( "selected/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );

        Files.copyContent( testDirectoryFixture.testPath( "src" ), testDirectoryFixture.testPath( "filtered" ), Lists.of( "**/2.txt" ), Lists.of(), true,
            macro -> "x".equals( macro ) ? "y" : macro );
        assertFile( testDirectoryFixture.testPath( "filtered/a/2.txt" ) ).hasContent( "1" );
        assertFile( testDirectoryFixture.testPath( "filtered/2.txt" ) ).hasContent( "y" );

        if( !Resources.IS_WINDOWS )
            assertEquals( Files.getPosixPermissions( testDirectoryFixture.testPath( "filtered/2.txt" ) ),
                Sets.of( OWNER_EXECUTE, OWNER_READ, OWNER_WRITE ) );
    }

    @Test
    public void isDirectoryEmpty() {
        Files.write( testDirectoryFixture.testPath( "/wildcard/1.txt" ), "1", ContentWriter.ofString() );

        assertThat( Files.isDirectoryEmpty( testDirectoryFixture.testPath( "/wildcard" ) ) ).isFalse();

        Files.delete( testDirectoryFixture.testPath( "/wildcard/1.txt" ) );

        assertThat( Files.isDirectoryEmpty( testDirectoryFixture.testPath( "/wildcard" ) ) ).isTrue();
    }

    @Test
    public void wildcardMatch() {
        assertTrue( Files.wildcardMatch( "bid_v15-2016-07-13-08-02.tsv.lz4", "bid_v*-2016-07-13-08-02.tsv.*" ) );
        assertTrue( Files.wildcardMatch( "bid", "bid*" ) );
        assertTrue( Files.wildcardMatch( "bid_", "bid?" ) );
        assertTrue( Files.wildcardMatch( "bid_v", "*d_v" ) );

        assertFalse( Files.wildcardMatch( "bid_v", "bb" ) );
        assertFalse( Files.wildcardMatch( "b", "bb" ) );

        assertFalse( Files.wildcardMatch( "file.txt123", "oap/io/CompressionPerformance/file.txt" ) );
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
            Path path = testDirectoryFixture.testPath( "file" + ext );
            Files.write( path, encoding, "value", ContentWriter.ofString() );
            Files.ensureFileEncodingValid( path );
            if( IoStreams.Encoding.from( ext ) != encoding ) fail( "should throw exception" );
        } catch( InvalidFileEncodingException e ) {
            if( IoStreams.Encoding.from( ext ) == encoding ) fail( "should not throw exception: " + e );
        }
    }

    @Test
    public void move() {
        Path path = testDirectoryFixture.testPath( "oap/io/CompressionPerformance/file.txt" );
        Path newPath = testDirectoryFixture.testPath( "test/newFile.txt" );
        Files.write( path, "test", ofString() );
        Files.write( newPath, "test2", ofString() );
        Files.move( path, newPath, REPLACE_EXISTING );

        assertThat( path ).doesNotExist();
        assertThat( newPath ).exists();
        assertThat( newPath ).hasContent( "test" );
    }

    @Test
    public void deleteEmptyDirectories() {
        Files.write( testDirectoryFixture.testPath( "/dir1/1.txt" ), "1", ofString() );
        Files.write( testDirectoryFixture.testPath( "/dir1/dir2/1.txt" ), "1", ofString() );

        var dir3 = testDirectoryFixture.testPath( "/dir1/dir3" );
        var dir4 = testDirectoryFixture.testPath( "/dir1/dir3/dir4" );

        Files.ensureDirectory( dir3 );
        Files.ensureDirectory( dir4 );

        Files.deleteEmptyDirectories( testDirectoryFixture.testPath( "/" ), false );

        assertThat( dir4 ).doesNotExist();
        assertThat( dir3 ).doesNotExist();
        assertThat( testDirectoryFixture.testPath( "/dir1/dir2" ) ).exists();
        assertThat( testDirectoryFixture.testPath( "/dir1/1.txt" ).toString() ).contains( "1" );

        Files.ensureDirectory( dir3 );
        Files.ensureDirectory( dir4 );

        Files.deleteEmptyDirectories( dir3, false );
        assertThat( dir4 ).doesNotExist();
        assertThat( dir3 ).exists();

        Files.deleteEmptyDirectories( dir3, true );
        assertThat( dir3 ).doesNotExist();
    }

    @Test
    public void format() {
        Dates.setTimeFixed( 2020, 1, 1, 1, 1 );
        assertThat( Files.format( Path.of( "a/b/c" ), "${HOST}/${NOW:DT:yyyy-MM-dd}/${A}-${B}.txt", Map.of( "A", "AAA", "B", "BBB" ) ) )
            .isEqualTo( Path.of( "a/b/c/" + HOSTNAME + "/2020-01-01/AAA-BBB.txt" ) );
        assertThat( Files.format( Path.of( "a/b/c" ), "${HOST}/${A:DT:yyyy-MM-dd}-${B}.txt", Map.of( "A", new DateTime( 2021, 2, 2, 11, 0 ), "B", "BBB" ) ) )
            .isEqualTo( Path.of( "a/b/c/" + HOSTNAME + "/2021-02-02-BBB.txt" ) );
        assertThat( Files.format( Path.of( "a/b/c" ), "${HOST}/${A:DT:yyyy-MM-dd}-${B}.txt", Map.of( "A", new LocalDate( 2021, 2, 2 ), "B", "BBB" ) ) )
            .isEqualTo( Path.of( "a/b/c/" + HOSTNAME + "/2021-02-02-BBB.txt" ) );
        assertThat( Files.format( Path.of( "a/b/c" ), "${HOST}/${A:DT:yyyy-MM-dd}-${B}.txt", Map.of( "A", java.time.LocalDateTime.of( 2021, 2, 2, 11, 0 ), "B", "BBB" ) ) )
            .isEqualTo( Path.of( "a/b/c/" + HOSTNAME + "/2021-02-02-BBB.txt" ) );
    }
}
