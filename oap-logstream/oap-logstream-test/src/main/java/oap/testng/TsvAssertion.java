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

import oap.util.Lists;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.data.MapEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@Deprecated
public class TsvAssertion extends AbstractCharSequenceAssert<TsvAssertion, CharSequence> {
    protected TsvAssertion( CharSequence value ) {
        super( value, TsvAssertion.class );
    }

    public static TsvAssertion assertTsv( CharSequence actual ) {
        return new TsvAssertion( actual );
    }

    public static TsvAssertion assertTsv( Path path ) {
        try {
            return new TsvAssertion( Files.readString( path ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static TsvAssertion assertTsv( File file ) {
        try {
            return new TsvAssertion( Files.readString( file.toPath() ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static TsvAssertion assertTsv( InputStream inputStream ) {
        try {
            return new TsvAssertion( IOUtils.toString( inputStream, StandardCharsets.UTF_8 ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static TsvAssertion assertTsv( Reader reader ) {
        try {
            return new TsvAssertion( IOUtils.toString( reader ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public final TsvAssertion containsHeader( String headerName ) {
        try( var parser = getParser() ) {
            var headers = parser.getHeaderMap();

            assertThat( headers ).containsKey( headerName );

            return this;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public final TsvAssertion containsHeaders( String... headerNames ) {
        try( var parser = getParser() ) {
            var headers = parser.getHeaderMap();

            assertThat( headers ).containsKeys( headerNames );

            return this;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public final TsvAssertion containsHeaders( Iterable<String> headerNames ) {
        try( var parser = getParser() ) {
            var headers = parser.getHeaderMap();

            assertThat( headers ).containsKeys( IteratorUtils.toArray( headerNames.iterator(), String.class ) );

            return this;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public final TsvAssertion containsOnlyHeaders( String... headerNames ) {
        try( var parser = getParser() ) {
            var headers = parser.getHeaderMap();

            assertThat( headers ).containsOnlyKeys( headerNames );

            return this;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public final TsvAssertion containsOnlyHeaders( Iterable<String> headerNames ) {
        try( var parser = getParser() ) {
            var headers = parser.getHeaderMap();

            assertThat( headers ).containsOnlyKeys( headerNames );

            return this;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    @SafeVarargs
    public final TsvAssertion containsRowCols( MapEntry<String, String>... entries ) {
        try( var parser = getParser() ) {
            var headers = parser.getHeaderMap();

            containsHeaders( Lists.map( List.of( entries ), e -> e.key ) );

            var result = new ArrayList<List<String>>();

            for( var record : parser ) {
                var arr = new ArrayList<String>();

                for( var entry : entries ) {
                    arr.add( record.get( entry.key ) );
                }

                result.add( arr );
            }

            assertThat( result ).contains( Lists.map( List.of( entries ), e -> e.value ) );

            return this;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    private CSVParser getParser() {
        try {
            return new CSVParser( new StringReader( actual.toString() ), CSVFormat.TDF
                .withFirstRecordAsHeader()
                .withIgnoreSurroundingSpaces( false ) );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }
}
