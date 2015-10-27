/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class Tsv {

    public static Optional<Stream<List<Object>>> fromResource( Class<?> contextClass, String name, Model model ) {
        return Resources.url( contextClass, name ).map( url -> fromUrl( url, model ) );
    }

    public static Stream<List<Object>> fromPath( Path path, Model model ) {
        return fromPath( path, IoStreams.Encoding.PLAIN, model );
    }

    public static Stream<List<Object>> fromPath( Path path, IoStreams.Encoding encoding, Model model ) {
        return fromStream( IoStreams.lines( path, encoding ), model );
    }

    public static Stream<List<Object>> fromPaths( List<Path> paths, Model model ) {
        return fromPaths( paths, IoStreams.Encoding.PLAIN, model );
    }

    public static Stream<List<Object>> fromPaths( List<Path> paths, IoStreams.Encoding encoding, Model model ) {
        return Stream.of( paths ).flatMap( path -> fromStream( IoStreams.lines( path, encoding ), model ) );
    }

    public static Stream<List<Object>> fromUrl( URL url, Model model ) {
        return fromUrl( url, model, IoStreams.Encoding.PLAIN, p -> {
        } );
    }

    public static Stream<List<Object>> fromUrl( URL url, Model model, IoStreams.Encoding encoding,
        Consumer<Integer> progressCallback ) {
        return fromStream( IoStreams.lines( url, encoding, progressCallback ), model );
    }

    public static Stream<List<Object>> fromStream( Stream<String> stream, Model model ) {
        int skip = model.withHeader ? 1 : 0;
        return fromStream( stream ).skip( skip )
            .filter( model.filter() )
            .mapWithIndex( ( index, line ) -> {
                try {
                    return model.convert( line );
                } catch( TsvException e ) {
                    throw new TsvException( "at line " + (index + skip) + " " + e, e.getCause() );
                } catch( Exception e ) {
                    throw new TsvException( "at line " + (index + skip) + " " + e, e );
                }
            } );
    }

    public static Stream<List<String>> fromStream( Stream<String> stream ) {
        return stream.map( Tsv::parse );
    }

    public static List<String> parse( String tsv ) {
        return Arrays.asList( StringUtils.splitByWholeSeparatorPreserveAllTokens( tsv, "\t" ) );
    }

    public static String print( List<?> list ) {
        return Strings.join( "\t", list.stream().map( e -> {
            String value = e == null ? "" : String.valueOf( e );
            String result = "";
            for( int i = 0; i < value.length(); i++ ) {
                char c = value.charAt( i );
                switch( c ) {
                    case '\r':
                        result += "\\r";
                        break;
                    case '\n':
                        result += "\\n";
                        break;
                    case '\t':
                        result += "\\t";
                        break;
                    default:
                        result += c;
                }
            }
            return result;
        } ).collect( toList() ) ) + "\n";
    }

}
