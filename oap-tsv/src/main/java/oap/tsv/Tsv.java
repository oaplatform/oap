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

import oap.io.IoStreams;
import oap.io.Resources;
import oap.util.Stream;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public class Tsv {

    public static Handler tsv = new Handler() {
        @Override
        protected List<String> parse( String line, int max ) {
            return Tokenizer.parse( line, Delimiters.TAB, max, false );
        }
    };

    public static Handler csv = new Handler() {
        @Override
        protected List<String> parse( String line, int max ) {
            return Tokenizer.parse( line, Delimiters.COMMA, max, true );
        }
    };

    public static String print( Stream<List<Object>> stream ) {
        return Printer.print( stream, Printer.TAB );
    }

    public static String print( List<?> list ) {
        return Printer.print( list, Printer.TAB );
    }

    public abstract static class Handler {
        public <T> Optional<Stream<T>> fromResource( Class<?> contextClass, String name, Model<T> model ) {
            return Resources.url( contextClass, name ).map( url -> fromUrl( url, model ) );
        }

        public <T> Stream<T> fromPath( Path path, Model<T> model ) {
            return fromStream( path, IoStreams.lines( path ), model );
        }

        public <T> Stream<T> fromPaths( List<Path> paths, Model.Complex<T> complex ) {
            return Stream.of( paths )
                .flatMap( path -> fromStream(
                    path,
                    IoStreams.lines( path ),
                    complex.modelFor( path.toString() )
                ) );
        }

        public <T> Stream<T> fromURLs( List<URL> urls, Model.Complex<T> complex ) {
            return Stream.of( urls )
                .flatMap( url -> fromStream(
                    url,
                    IoStreams.lines( url ),
                    complex.modelFor( url.toString() )
                ) );
        }

        public <T> Stream<T> fromPaths( List<Path> paths, Model<T> model ) {
            return Stream.of( paths )
                .flatMap( path -> fromStream(
                    path,
                    IoStreams.lines( path ),
                    model )
                );
        }

        public <T> Stream<T> fromUrl( URL url, Model<T> model ) {
            return fromUrl( url, model, IoStreams.Encoding.PLAIN, p -> {
            } );
        }

        public <T> Stream<T> fromUrl( URL url, Model<T> model, IoStreams.Encoding encoding,
                                      Consumer<Integer> progressCallback ) {
            return fromStream( url, IoStreams.lines( url, encoding, progressCallback ), model );
        }

        public <T> Stream<T> fromString( String tsv, Model<T> model ) {
            return fromStream( Stream.of( new BufferedReader( new StringReader( tsv ) ).lines() ), model );
        }

        public <T> Stream<T> fromStream( Stream<String> stream, Model<T> model ) {
            return fromStream( "unknown", stream, model );
        }

        private <T> Stream<T> fromStream( Object source, Stream<String> stream, Model<T> model ) {
            int skip = model.withHeader ? 1 : 0;
            return fromStream( stream, model.maxOffset() )
                .skip( skip )
                .filter( model.filter() )
                .mapWithIndex( ( index, line ) -> {
                    try {
                        return model.map( line );
                    } catch( TsvException e ) {
                        throw new TsvException( "[" + ( index + skip ) + "] " + source + ": " + e, e.getCause() );
                    } catch( Exception e ) {
                        throw new TsvException( "[" + ( index + skip ) + "] " + source + ": " + e, e );
                    }
                } );
        }

        public Stream<List<String>> fromStream( Stream<String> stream ) {
            return fromStream( stream, Integer.MAX_VALUE );
        }

        public Stream<List<String>> fromStream( Stream<String> stream, int max ) {
            return stream.map( line -> parse( line, max ) );
        }

        protected abstract List<String> parse( String line, int max );
    }
}
