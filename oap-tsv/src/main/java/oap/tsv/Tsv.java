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
import oap.util.Lists;
import oap.util.Stream;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Tsv {

    public static Optional<Stream<List<Object>>> fromResource( Class<?> contextClass, String name, Model model ) {
        return Resources.url( contextClass, name ).map( url -> fromUrl( url, model ) );
    }

    public static Stream<List<Object>> fromPath( Path path, Model model ) {
        return fromStream( path, IoStreams.lines( path ), model );
    }

    public static Stream<List<Object>> fromPaths( List<Path> paths, Model.Complex complexModel ) {
        return Stream.of( paths )
            .flatMap( path -> fromStream(
                path,
                IoStreams.lines( path ),
                complexModel.modelFor( path.toString() )
            ) );
    }

    public static Stream<List<Object>> fromURLs( List<URL> urls, Model.Complex complexModel ) {
        return Stream.of( urls )
            .flatMap( url -> fromStream(
                url,
                IoStreams.lines( url ),
                complexModel.modelFor( url.toString() )
            ) );
    }

    public static Stream<List<Object>> fromPaths( List<Path> paths, Model model ) {
        return Stream.of( paths )
            .flatMap( path -> fromStream(
                path,
                IoStreams.lines( path ),
                model )
            );
    }

    public static Stream<List<Object>> fromUrl( URL url, Model model ) {
        return fromUrl( url, model, IoStreams.Encoding.PLAIN, p -> {
        } );
    }

    public static Stream<List<Object>> fromUrl( URL url, Model model, IoStreams.Encoding encoding,
                                                Consumer<Integer> progressCallback ) {
        return fromStream( url, IoStreams.lines( url, encoding, progressCallback ), model );
    }

    public static Stream<List<Object>> fromString( String tsv, Model model ) {
        return fromStream( Stream.of( new BufferedReader( new StringReader( tsv ) ).lines() ), model );
    }

    public static Stream<List<Object>> fromStream( Stream<String> stream, Model model ) {
        return fromStream( "unknown", stream, model );
    }

    private static Stream<List<Object>> fromStream( Object source, Stream<String> stream, Model model ) {
        int skip = model.withHeader ? 1 : 0;
        return fromStream( stream, model.maxOffset() )
            .skip( skip )
            .filter( model.filter() )
            .mapWithIndex( ( index, line ) -> {
                try {
                    return model.convert( line );
                } catch( TsvException e ) {
                    throw new TsvException( "[" + ( index + skip ) + "] " + source + ": " + e, e.getCause() );
                } catch( Exception e ) {
                    throw new TsvException( "[" + ( index + skip ) + "] " + source + ": " + e, e );
                }
            } );
    }

    public static Stream<List<String>> fromStream( Stream<String> stream ) {
        return stream.map( Tsv::parse );
    }

    public static Stream<List<String>> fromStream( Stream<String> stream, int max ) {
        return stream.map( line -> Tsv.parse( line, max ) );
    }

    public static List<String> parse( String tsv ) {
        return Lists.of( StringUtils.splitByWholeSeparatorPreserveAllTokens( tsv, "\t" ) );
    }

    public static List<String> parse( String tsv, int max ) {
        return splitByWholeSeparatorPreserveAllTokens( tsv, max );
    }


    private static List<String> splitByWholeSeparatorPreserveAllTokens( final String str, final int max ) {
        final int len = str.length();

        final ArrayList<String> substrings = new ArrayList<>();
        int numberOfSubstrings = 0;
        int beg = 0;
        int end = 0;
        while( end < len ) {
            end = str.indexOf( '\t', beg );

            if( end > -1 )
                if( end > beg ) {
                    numberOfSubstrings += 1;
                    substrings.add( str.substring( beg, end ) );
                    if( numberOfSubstrings == max ) end = len;
                    else beg = end + 1;
                } else {
                    numberOfSubstrings += 1;
                    substrings.add( StringUtils.EMPTY );
                    beg = end + 1;
                }
            else {
                substrings.add( str.substring( beg ) );
                end = len;
            }
        }

        return substrings;
    }


    public static String print( Stream<List<Object>> stream ) {
        return stream.map( Tsv::print ).collect( Collectors.joining() );
    }

    public static String print( List<?> list ) {
        return Stream.of( list )
            .map( e -> {
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
            } )
            .collect( Collectors.joining( "\t" ) ) + "\n";

    }
}
