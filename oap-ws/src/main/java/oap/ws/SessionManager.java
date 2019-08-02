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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import oap.http.Session;
import oap.util.Cuid;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    public static final String COOKIE_ID = "SID";

    public final String cookieDomain;
    public final String cookiePath;
    public final int cookieExpiration;
    private final Cache<String, Session> sessions;
    public Cuid cuid = Cuid.UNIQUE;

    public SessionManager( int expirationTime, String cookieDomain, String cookiePath ) {
        this.sessions = CacheBuilder.newBuilder()
            .expireAfterAccess( expirationTime, TimeUnit.MINUTES )
            .build();
        this.cookieDomain = cookieDomain;
        this.cookiePath = cookiePath;
        this.cookieExpiration = expirationTime;
    }

    public Optional<Session> get( String id ) {
        return Optional.ofNullable( sessions.getIfPresent( id ) );
    }

    @SneakyThrows
    public Session getOrInit( String id ) {
        String sessionId = id == null ? cuid.next() : id;
        return sessions.get( sessionId, () -> new Session( sessionId ) );
    }

    public void clear() {
        sessions.invalidateAll();
    }

    public void remove( String id ) {
        sessions.invalidate( id );
    }
}
