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

import com.google.common.base.Preconditions;
import oap.util.Lists;
import oap.util.Pair;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import static oap.util.Pair.__;

public class Cookie {
    public static final long NO_MAX_AGE = -1;
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern( "EEE, dd-MMM-yyyy HH:mm:ss zzz" ).withLocale( Locale.ENGLISH );
    public String domain;
    public DateTime expires;
    public long maxAge = NO_MAX_AGE;
    public String path;
    public boolean httpOnly = false;
    public boolean secure = false;
    public SameSite sameSite = SameSite.None;
    public final String name;
    public final String value;

    public Cookie( String name, Object value ) {
        this( name, String.valueOf( value ) );
    }

    public Cookie( String name, String value ) {
        this.name = name;
        this.value = value;
    }

    public static Cookie parse( String cookieString ) {
        return Parser.parse( cookieString );
    }

    public static class Keys {
        public static final String Domain = "Domain";
        public static final String Expires = "Expires";
        public static final String Path = "Path";
        public static final String MaxAge = "Max-Age";
        public static final String HttpOnly = "HttpOnly";
        public static final String Secure = "Secure";
        public static final String SameSite = "SameSite";
    }

    private static class Parser {
        private List<Pair<String, String>> components;

        private Parser( String cookie ) {
            this.components = Stream.of( StringUtils.split( cookie, ";" ) )
                .map( String::trim )
                .mapToPairs( component -> component.contains( "=" )
                    ? Strings.split( component, "=" )
                    : __( component, null ) )
                .toList();
            Preconditions.checkArgument( !components.isEmpty(), "doesn't look like a cookie: " + cookie );
        }

        public static Cookie parse( String cookieString ) {
            Parser parser = new Parser( cookieString );
            Cookie cookie = new Cookie( parser.name(), parser.value() );
            cookie.httpOnly = parser.contains( Keys.HttpOnly );
            cookie.secure = parser.contains( Keys.Secure );
            parser.get( Keys.SameSite, ss -> Cookie.SameSite.of( ss ).orElse( Cookie.SameSite.None ) ).ifPresent( cookie::withSameSite );
            parser.get( Keys.Domain ).ifPresent( cookie::withDomain );
            parser.get( Keys.Path ).ifPresent( cookie::withPath );
            parser.get( Keys.MaxAge, Long::parseLong ).ifPresent( cookie::withMaxAge );
            parser.get( Keys.Expires, FORMATTER::parseDateTime ).ifPresent( cookie::withExpires );
            return cookie;
        }

        public String name() {
            return components.get( 0 )._1;
        }

        public String value() {
            return components.get( 0 )._2;
        }

        public boolean contains( String key ) {
            return find( key ).isPresent();
        }

        public <T> Optional<T> get( String key, Function<String, T> mapper ) {
            return find( key ).map( p -> mapper.apply( p._2 ) );
        }

        private Optional<Pair<String, String>> find( String key ) {
            for( Pair<String, String> component : components )
                if( component._1.equalsIgnoreCase( key ) ) return Optional.of( component );
            return Optional.empty();
        }

        public Optional<String> get( String key ) {
            return get( key, Function.identity() );
        }
    }

    public Cookie withDomain( String domain ) {
        this.domain = domain;
        return this;
    }

    public Cookie withSameSite( SameSite sameSite ) {
        this.sameSite = sameSite;
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

    public Cookie withMaxAge( long seconds ) {
        this.maxAge = seconds;
        return this;
    }

    public Cookie httpOnly() {
        return this.httpOnly( true );
    }

    public Cookie httpOnly( boolean httpOnly ) {
        this.httpOnly = httpOnly;
        return this;
    }

    public enum SameSite {
        Strict, Lax, None;

        public static Optional<SameSite> of( String value ) {
            for( SameSite ss : values() ) if( ss.name().equalsIgnoreCase( value ) ) return Optional.of( ss );
            return Optional.empty();
        }
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie
     */
    public String toString() {
        List<String> values = Lists.of( name + "=" + value );
        if( expires != null ) values.add( Keys.Expires + "=" + FORMATTER.print( expires ) );
        if( maxAge != NO_MAX_AGE ) values.add( Keys.MaxAge + "=" + maxAge );
        if( StringUtils.isNotBlank( domain ) ) values.add( Keys.Domain + "=" + domain );
        if( StringUtils.isNotBlank( path ) ) values.add( Keys.Path + "=" + path );
        if( secure ) values.add( "Secure" );
        if( httpOnly ) values.add( "HttpOnly" );
        values.add( Keys.SameSite + "=" + sameSite );
        return Strings.join( "; ", values );
    }

}
