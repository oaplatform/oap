package oap.ws.cat;/*
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

import oap.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public class CatApi {
    private static final ContentType CONTENT_TYPE =
        ContentType.create( "text/tab-separated-values", StandardCharsets.UTF_8 );

    public static String formatDouble( Object d, int decimal ) {
        if( d instanceof Number ) return String.format( "%." + decimal + "f", ( ( Number ) d ).doubleValue() );
        else return formatDouble( Double.parseDouble( d.toString() ), decimal );
    }

    @SafeVarargs
    public static HttpResponse table( List<Object>... rows ) {
        return table( asList( rows ) );
    }

    public static HttpResponse table( List<List<Object>> rows ) {
        if( rows.isEmpty() ) return HttpResponse.ok( "", true, CONTENT_TYPE ).response();

        final int cols = rows.iterator().next().size();
        final int[] size = new int[cols];

        Arrays.fill( size, 0 );

        for( List<Object> row : rows ) {
            assert row.size() == cols;

            for( int i = 0; i < cols; i++ ) {
                final String item = row.get( i ).toString();
                if( size[i] < item.length() ) size[i] = item.length();
            }
        }

        final StringBuilder body = new StringBuilder();

        for( List<Object> row : rows ) {
            final StringBuilder rowBody = new StringBuilder();

            for( int i = 0; i < row.size(); i++ ) {
                if( rowBody.length() != 0 ) rowBody.append( ' ' );

                rowBody.append( StringUtils.rightPad( row.get( i ).toString(), size[i], ' ' ) );
            }

            body.append( rowBody.toString() ).append( '\n' );
        }

        return HttpResponse.ok( body.toString(), true, CONTENT_TYPE ).response();
    }
}
