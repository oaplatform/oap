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

import io.undertow.util.Cookies;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.joda.time.DateTime;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode
@Builder( builderMethodName = "internalBuilder", setterPrefix = "with", builderClassName = "CookieBuilder" )
@Getter
public class Cookie implements Serializable {
    @Serial
    private static final long serialVersionUID = -6167221123115890689L;

    private final String name;
    private String value;
    private String path;
    private String domain;
    private Integer maxAge;
    private Date expires;
    private boolean discard;
    private boolean secure;
    private boolean httpOnly;
    private int version = 0;
    private String comment;
    private boolean sameSite;
    private String sameSiteMode;

    public static CookieBuilder builder( String name, String value ) {
        return internalBuilder().withName( name ).withValue( value );
    }

    public static Cookie parseSetCookieHeader( String cookie ) {
        io.undertow.server.handlers.Cookie uc = Cookies.parseSetCookieHeader( cookie );

        return builder( uc.getName(), uc.getValue() )
            .withPath( uc.getPath() )
            .withDomain( uc.getDomain() )
            .withMaxAge( uc.getMaxAge() )
            .withExpires( uc.getExpires() )
            .withDiscard( uc.isDiscard() )
            .withSecure( uc.isSecure() )
            .withHttpOnly( uc.isHttpOnly() )
            .withVersion( uc.getVersion() )
            .withComment( uc.getComment() )
            .withSameSite( uc.isSameSite() )
            .withSameSiteMode( uc.getSameSiteMode() )
            .build();
    }

    public static class CookieBuilder {
        public CookieBuilder withExpires( DateTime expires ) {
            this.expires = expires.toDate();
            return this;
        }

        public CookieBuilder withExpires( Date expires ) {
            this.expires = expires;
            return this;
        }
    }
}
