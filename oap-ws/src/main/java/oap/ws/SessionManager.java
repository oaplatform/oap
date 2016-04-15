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
import oap.http.Session;
import org.joda.time.DateTime;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class SessionManager {

    private final Cache<String, Session> sessions;
    public final String cookieDomain;
    public final String cookiePath;
    public final DateTime cookieExpiration;

    public SessionManager( int expirationTime, String cookieDomain, String cookiePath ) {
        this.sessions = CacheBuilder.newBuilder()
            .expireAfterAccess( expirationTime, TimeUnit.MINUTES )
            .build();
        this.cookieDomain = cookieDomain;
        this.cookiePath = cookiePath;
        this.cookieExpiration = DateTime.now().plusMinutes( expirationTime );
    }

    public Session getSessionById( String id ) {
        return id == null ? null : sessions.getIfPresent( id );
    }

    public void put( String sessionId, Session session ) {
        sessions.put( sessionId, session );
    }

    public void putSessionData( String sessionId, String key, Object value ) {
        final Session session = sessions.getIfPresent( sessionId );

        if( session != null ) {
            session.set( key, value );
        } else throw new NoSuchElementException( "Element does not exist: " + sessionId );
    }

    public Object getSessionData( String sessionId, String key ) {
        final Session session = sessionId == null ? null : sessions.getIfPresent( sessionId );

        return session == null ? null : session.get( key );
    }

    public void clear() {
        sessions.invalidateAll();
    }

    public void removeSessionById( String sessionId ) {
        sessions.invalidate( sessionId );
    }
}
