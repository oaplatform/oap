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

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode
public class Cookie implements Serializable {
    @Serial
    private static final long serialVersionUID = -6167221123115890689L;

    public final io.undertow.server.handlers.Cookie delegate;

    public Cookie( String name, Object value ) {
        this( name, String.valueOf( value ) );
    }

    public Cookie( String name, String value ) {
        this( new CookieImpl( name, value ) );
    }

    private Cookie( io.undertow.server.handlers.Cookie cookie ) {
        this.delegate = cookie;
    }

    public static Cookie parseSetCookieHeader( String headerValue ) {
        return new Cookie( Cookies.parseSetCookieHeader( headerValue ) );
    }

    public Cookie withDomain( String domain ) {
        this.delegate.setDomain( domain );
        return this;
    }

    public Cookie withSameSite( boolean sameSite ) {
        this.delegate.setSameSite( sameSite );
        return this;
    }

    public Cookie withExpires( DateTime expires ) {
        this.delegate.setExpires( expires.toDate() );
        return this;
    }

    public Cookie withPath( String path ) {
        this.delegate.setPath( path );
        return this;
    }

    public Cookie withMaxAge( long seconds ) {
        this.delegate.setMaxAge( ( int ) seconds );
        return this;
    }

    public Cookie httpOnly() {
        return this.httpOnly( true );
    }

    public Cookie httpOnly( boolean httpOnly ) {
        this.delegate.setHttpOnly( httpOnly );
        return this;
    }

    public Cookie secure( boolean secure ) {
        this.delegate.setSecure( secure );
        return this;
    }

    public String toString() {
        return delegate.toString();
    }

    public String getName() {
        return delegate.getName();
    }

    public String getValue() {
        return delegate.getValue();
    }

    public String getDomain() {
        return delegate.getDomain();
    }

    public Date getExpires() {
        return delegate.getExpires();
    }

    public String getPath() {
        return delegate.getPath();
    }

    public Integer getMaxAge() {
        return delegate.getMaxAge();
    }

    public boolean isSameSite() {
        return delegate.isSameSite();
    }

    public boolean isSecure() {
        return delegate.isSecure();
    }

    public boolean isHttpOnly() {
        return delegate.isHttpOnly();
    }
}
