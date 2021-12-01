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

import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.Cookies;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;

import java.util.Date;

@EqualsAndHashCode
public class Cookie {
    public final io.undertow.server.handlers.Cookie cookie;

    public Cookie( String name, Object value ) {
        this( name, String.valueOf( value ) );
    }

    public Cookie( String name, String value ) {
        this( new CookieImpl( name, value ) );
    }

    private Cookie( io.undertow.server.handlers.Cookie cookie ) {
        this.cookie = cookie;
    }

    public static Cookie parseSetCookieHeader( String headerValue ) {
        return new Cookie( Cookies.parseSetCookieHeader( headerValue ) );
    }

    public Cookie withDomain( String domain ) {
        this.cookie.setDomain( domain );
        return this;
    }

    public Cookie withSameSite( boolean sameSite ) {
        this.cookie.setSameSite( sameSite );
        return this;
    }

    public Cookie withExpires( DateTime expires ) {
        this.cookie.setExpires( expires.toDate() );
        return this;
    }

    public Cookie withPath( String path ) {
        this.cookie.setPath( path );
        return this;
    }

    public Cookie withMaxAge( long seconds ) {
        this.cookie.setMaxAge( ( int ) seconds );
        return this;
    }

    public Cookie httpOnly() {
        return this.httpOnly( true );
    }

    public Cookie httpOnly( boolean httpOnly ) {
        this.cookie.setHttpOnly( httpOnly );
        return this;
    }

    public Cookie secure( boolean secure ) {
        this.cookie.setSecure( secure );
        return this;
    }

    public String toString() {
        return cookie.toString();
    }

    public String getName() {
        return cookie.getName();
    }

    public String getValue() {
        return cookie.getValue();
    }

    public String getDomain() {
        return cookie.getDomain();
    }

    public Date getExpires() {
        return cookie.getExpires();
    }

    public String getPath() {
        return cookie.getPath();
    }

    public Integer getMaxAge() {
        return cookie.getMaxAge();
    }

    public boolean isSameSite() {
        return cookie.isSameSite();
    }

    public boolean isSecure() {
        return cookie.isSecure();
    }

    public boolean isHttpOnly() {
        return cookie.isHttpOnly();
    }
}
