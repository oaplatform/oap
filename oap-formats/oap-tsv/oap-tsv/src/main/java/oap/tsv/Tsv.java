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

package oap.tsv;

import lombok.EqualsAndHashCode;
import oap.io.IoStreams;
import oap.io.Resources;
import oap.io.content.ContentReader;
import oap.util.Stream;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.io.IoStreams.lines;

@EqualsAndHashCode
public class Tsv {

    public static final char DELIMITER_SEMICOLON = ';';
    public static final char DELIMITER_TAB = '\t';
    public static final char DELIMITER_COMMA = ',';

    public static final AbstractParser tsv = new AbstractParser() {
        @Override
        public List<String> parse( String line ) {
            return Tokenizer.parse( line, DELIMITER_TAB, Integer.MAX_VALUE, false );
        }
    };

    public static final AbstractParser csv = new AbstractParser() {
        @Override
        public List<String> parse( String line ) {
            return Tokenizer.parse( line, DELIMITER_COMMA, Integer.MAX_VALUE, true );
        }
    };

    public final List<String> headers;
    public final List<List<String>> data;

    public Tsv( List<String> headers, List<List<String>> data ) {
        this.headers = headers;
        this.data = data;
    }

    public TsvStream stream() {
        return TsvStream.of( headers, Stream.of( data ) );
    }

    @Override
    public String toString() {
        return stream().toTsvString();
    }

    public abstract static class AbstractParser {
        @Deprecated
        public TsvStream from( byte[] bytes ) {
            return from( new ByteArrayInputStream( bytes ) );
        }

        @Deprecated
        public TsvStream from( byte[] bytes, int offset, int length ) {
            return from( new ByteArrayInputStream( bytes, offset, length ) );
        }

        @Deprecated
        public TsvStream from( InputStream inputStream ) {
            return from( inputStream, UTF_8 );
        }

        public TsvStream from( InputStream inputStream, Charset charset ) {
            return from( new InputStreamReader( inputStream, charset ) );
        }

        public TsvStream from( Reader reader ) {
            return fromStream( Stream.of( new BufferedReader( reader ).lines() ) );
        }

        @Deprecated
        public TsvStream fromString( String tsv ) {
            return from( new StringReader( tsv ) );
        }

        public TsvStream fromStream( Stream<String> stream ) {
            return TsvStream.of( stream.map( this::parse ) );
        }

        @Deprecated
        public Optional<TsvStream> fromResource( Class<?> contextClass, String name ) {
            return Resources.url( contextClass, name ).map( this::fromUrl );
        }

        @Deprecated
        public TsvStream fromPath( Path path ) {
            return fromStream( lines( path ) );
        }

        @Deprecated
        public TsvStream fromUrl( URL url ) {
            return fromUrl( url, PLAIN, p -> {} );
        }

        public TsvStream fromUrl( URL url, IoStreams.Encoding encoding,
                                  Consumer<Integer> progressCallback ) {
            return fromStream( lines( url, encoding, progressCallback ) );
        }

        public ContentReader<TsvStream> ofSeparatedValues() {
            return new ContentReader<>() {
                @Override
                public TsvStream read( InputStream is ) {
                    return from( is );
                }
            };
        }

        public abstract List<String> parse( String line );
    }

}
