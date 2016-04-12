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

import oap.http.Session;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    public Session getSessionById( String id ) {
        return sessionMap.get( id );
    }

    public void put( String sessionId, Session session ) {
        sessionMap.put( sessionId, session );
    }

    public void putSessionData( String sessionId, String key, Object value ) {
        final Session session = sessionMap.get( sessionId );

        if( session != null ) {
            session.set( key, value );
        } else throw new NoSuchElementException( "Element does not exist: " + sessionId );
    }

    public Object getSessionData( String sessionId, String key ) {
        final Session session = sessionMap.get( sessionId );

        return session != null ? session.get( key ) :
            new NoSuchElementException( "Element does not exist: " + sessionId );
    }

    public void clear() {
        sessionMap.clear();
    }

    public void removeSessionById( String sessionId ) {
        sessionMap.remove( sessionId );
    }
}
