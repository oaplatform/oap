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

import oap.util.Lists;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Cookie {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern( "EEE, dd-MMM-yyyy HH:mm:ss zzz" ).withLocale( Locale.ENGLISH );
    private String domain;
    private String expires;
    private String path;
    private List<String> values = new ArrayList<>();

    public Cookie( String name, String value ) {
        withValue( name, value );
    }

    public Cookie withDomain( String domain ) {
        this.domain = StringUtils.isNotBlank( domain ) ? "domain=" + domain : null;
        return this;
    }

    public Cookie withExpires( DateTime expires ) {
        this.expires = expires != null ? "expires=" + FORMATTER.print( expires ) : null;
        return this;
    }

    public Cookie withPath( String path ) {
        this.path = StringUtils.isNotBlank( path ) ? "path=" + path : null;
        return this;
    }

    public Cookie withValue( String name, String value ) {
        if( StringUtils.isNoneBlank( name, value ) ) this.values.add( name + "=" + value );
        return this;
    }

    public Cookie httpOnly() {
        this.values.add( "HttpOnly" );
        return this;
    }

    public String toString() {
        return Strings.join( "; ", Lists.of( Strings.join( "; ", values ), domain, expires, path ) );
    }

}
