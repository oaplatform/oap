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

import lombok.SneakyThrows;
import oap.concurrent.Times;
import oap.io.Files;
import oap.io.IoStreams;
import oap.io.IoStreams.Encoding;
import oap.io.content.ContentReader;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Arrays;
import oap.util.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.LZ4;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.io.IoStreams.Encoding.ZSTD;
import static oap.io.content.ContentWriter.ofString;
import static oap.testng.Asserts.assertFile;
import static oap.testng.Asserts.pathOfTestResource;
import static oap.testng.TestDirectoryFixture.testPath;
import static org.assertj.core.api.Assertions.assertThat;

public class IoStreamsTest extends Fixtures {
    public IoStreamsTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    @SneakyThrows
    public void emptyGz() {
        Path path = testPath( "test.gz" );
        try( OutputStream out = IoStreams.out( path, GZIP ) ) {
            out.flush();
        }
        try( InputStream in = IoStreams.in( path, GZIP ) ) {
            assertThat( in.read() ).isEqualTo( -1 );
        }
        assertFile( path ).hasContent( "", GZIP );
    }

    @DataProvider
    public Object[][] encodings() {
        return Arrays.map( Encoding[].class, e -> new Encoding[] { e },
            Arrays.filter( v -> v.appendable, Encoding.values() ) );
    }

    @Test( dataProvider = "encodings" )
    @SneakyThrows
    public void append( Encoding encoding ) {
        Path path = encoding.resolve( testPath( "test.txt" ) );
        try( OutputStream out = IoStreams.out( path, encoding ) ) {
            out.write( "12345".getBytes() );
            out.flush();
        }
        try( OutputStream out = IoStreams.out( path, encoding, true ) ) {
            out.write( "12345".getBytes() );
            out.flush();
        }
        assertFile( path ).hasContent( "1234512345", encoding );
    }

    @Test
    public void lz4() throws IOException {
        Path path = testPath( "test.lz4" );

        try( OutputStream out = IoStreams.out( path, LZ4 ) ) {
            out.write( "12345".getBytes() );
            out.flush();
        }

        assertFile( path ).hasContent( "12345", LZ4 );
    }

    @Test
    public void encodingResolve() {
        assertThat( LZ4.resolve( Paths.get( "/x/a.txt.gz" ) ) ).isEqualTo( Paths.get( "/x/a.txt.lz4" ) );
        assertThat( PLAIN.resolve( Paths.get( "/x/a.txt.gz" ) ) ).isEqualTo( Paths.get( "/x/a.txt" ) );
        assertThat( GZIP.resolve( Paths.get( "/x/a.txt" ) ) ).isEqualTo( Paths.get( "/x/a.txt.gz" ) );
        assertThat( ZSTD.resolve( Paths.get( "/x/a.txt.zst" ) ) ).isEqualTo( Paths.get( "/x/a.txt.zst" ) );
    }

    @Test
    @SneakyThrows
    public void compressionLevel() {
        List<List<String>> sets = new ArrayList<>();
        Random random = new Random();
        int columns = 30;
        for( int i = 0; i < columns; i++ ) {
            HashSet<String> set = new HashSet<>();
            boolean numbers = random.nextBoolean();
            int values = 1 + random.nextInt( 300 );
            if( numbers )
                Times.times( values, () -> set.add( RandomStringUtils.randomNumeric( 10 ) ) );
            else
                Times.times( values, () -> set.add( RandomStringUtils.randomAlphabetic( 10, 30 ) ) );
            System.out.println( "column[" + i + "] variance is " + set.size() + ( numbers ? " numbers" : " strings" ) );
            sets.add( new ArrayList<>( set ) );
        }
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < 100000; i++ ) {
            for( List<String> set : sets ) sb.append( Lists.random( set ).orElseThrow() ).append( '\t' );
            sb.append( '\n' );
        }
        String content = sb.toString();
//        String content = Files.readString( pathOfTestResource( getClass(), "log.tsv.gz" ), GZIP );
        for( Encoding encoding : Arrays.filter( v -> v.compressed && v.streamable, Encoding.values() ) ) {
            Path path = testPath( "compressed.tsv" + encoding.extension );
            Files.write( path, encoding, content, ofString() );
            System.out.println( encoding + ":\t" + content.length() + " -> " + path.toFile().length() );
        }
        System.out.println( "Low variance file" );
        content = Files.read( pathOfTestResource( getClass(), "log.tsv.gz" ), GZIP, ContentReader.ofString() );
        for( Encoding encoding : Arrays.filter( v -> v.compressed && v.streamable, Encoding.values() ) ) {
            Path path = testPath( "compressed.tsv" + encoding.extension );
            Files.write( path, encoding, content, ofString() );
            System.out.println( encoding + ":\t" + content.length() + " -> " + path.toFile().length() );
        }
    }
}
