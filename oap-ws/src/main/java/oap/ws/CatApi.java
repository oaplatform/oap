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

package oap.ws;

import com.google.common.collect.Iterables;
import oap.http.HttpResponse;
import org.apache.http.entity.ContentType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * Created by Igor Petrenko on 21.12.2015.
 */
public class CatApi {
    private static final ContentType CONTENT_TYPE =
        ContentType.create( "text/tab-separated-values", StandardCharsets.UTF_8 );

    public static HttpResponse table( String... rows ) {
        return HttpResponse.ok( String.join( "\n", rows ) + "\n", true,
            CONTENT_TYPE );
    }

    @SuppressWarnings( "unchecked" )
    public static HttpResponse table( Collection<String>... rows ) {
        return HttpResponse.ok( String.join( "\n", Iterables.concat( rows ) ) + "\n", true,
            CONTENT_TYPE );
    }

    public static String row( Object... tabs ) {
        return String.join( "\t", Arrays.stream( tabs ).map( Object::toString ).collect( toList() ) );
    }

    public static List<String> rows( Object... tabs ) {
        return Collections.singletonList( row( tabs ) );
    }

    public static String influxToZabbix( String metricWithTags ) {
        final Matcher matcher = Pattern.compile( ",[^=]+=" ).matcher( metricWithTags );
        final StringBuffer sb = new StringBuffer();
        boolean first = true;
        while( matcher.find() ) {
            matcher.appendReplacement( sb, first ? "[" : "," );
            first = false;
        }

        matcher.appendTail( sb );
        if( !first ) sb.append( "]" );
        return sb.toString();
    }
}
