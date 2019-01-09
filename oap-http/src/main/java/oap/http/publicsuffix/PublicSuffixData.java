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

package oap.http.publicsuffix;

import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;
import oap.util.Sets;
import org.apache.commons.lang3.StringUtils;

import java.net.IDN;
import java.util.Set;

@Slf4j
public class PublicSuffixData {
    public static final Set<String> suffixes;

    static {
        suffixes = Resources.lines( PublicSuffixData.class, "public_suffix_list.dat.txt" )
            .map( s -> s
                .map( l -> l.trim().toLowerCase() )
                .filter( l -> !l.isEmpty() && !l.startsWith( "//" ) && !l.startsWith( "!" ) )
                .map( l -> l.startsWith( "*." ) ? l.substring( 2 ) : l )
                .toSet() )
            .orElse( Sets.empty() );
        log.debug( "{} domain suffixes loaded", suffixes.size() );
    }

    public static String baseDomainOf( String domain ) {
        if( domain == null || domain.startsWith( "." ) ) return null;
        String normalized = IDN.toUnicode( domain.toLowerCase() );
        if( suffixes.contains( normalized ) ) return null;
        String[] split = StringUtils.split( normalized, '.' );
        if( split.length == 1 ) return null;
        for( int i = 1; i < split.length; i++ ) {
            StringBuilder sb = new StringBuilder( split[i] );
            for( int j = i + 1; j < split.length; j++ ) sb.append( '.' ).append( split[j] );
            String suffix = sb.toString();
            if( suffixes.contains( suffix ) ) return split[i - 1] + "." + suffix;
        }
        return split[split.length - 2] + "." + split[split.length - 1];
    }
}

