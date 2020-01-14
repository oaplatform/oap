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

package oap.http;

import oap.util.BiStream;
import oap.util.Lists;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Cookie {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern( "EEE, dd-MMM-yyyy HH:mm:ss zzz" ).withLocale( Locale.ENGLISH );
    private String domain;
    private DateTime expires;
    private String path;
    private boolean httpOnly;
    private boolean secure = true;
    private SameSite sameSite = SameSite.None;
    private String name;
    private Object value;
    private Map<String, Object> values = new HashMap<>();

    public Cookie( String name, Object value ) {
        this.name = name;
        this.value = value;
    }

    public Cookie withDomain( String domain ) {
        this.domain = domain;
        return this;
    }

    public Cookie withExpires( DateTime expires ) {
        this.expires = expires;
        return this;
    }

    public Cookie withPath( String path ) {
        this.path = path;
        return this;
    }

    public Cookie withValue( String name, String value ) {
        if( StringUtils.isNoneBlank( name, value ) ) this.values.put( name, value );
        return this;
    }

    public Cookie httpOnly() {
        return this.httpOnly( true );
    }

    private Cookie httpOnly( boolean httpOnly ) {
        this.httpOnly = httpOnly;
        return this;
    }

    public Cookie sameSite( SameSite sameSite ) {
        this.sameSite = sameSite;
        return this;
    }

    public enum SameSite {
        Strict, Lax, None
    }


    public String toString() {
        List<String> values = Lists.of(
            name + "=" + value,
            StringUtils.isNotBlank( domain ) ? "domain=" + domain : null,
            expires != null ? "expires=" + FORMATTER.print( expires ) : null,
            StringUtils.isNotBlank( path ) ? "path=" + path : null,
            "SameSite=" + sameSite
        );
        values.addAll( BiStream.of( this.values ).mapToObj( ( k, v ) -> k + "=" + v ).toList() );
        if( secure ) values.add( "Secure" );
        if( httpOnly ) values.add( "HttpOnly" );
        return Strings.join( "; ", values );
    }

}
