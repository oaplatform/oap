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

package oap.ws.sso.interceptor;

import lombok.extern.slf4j.Slf4j;
import oap.util.Dates;
import oap.ws.InvocationContext;
import oap.ws.Response;
import oap.ws.interceptor.Interceptor;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.ws.sso.SSO.SESSION_USER_KEY;
import static org.joda.time.DateTimeZone.UTC;

/**
 * The main purpose of ThrottleLoginInterceptor is to prevent users from brut-forcing our login endpoints
 */
@Slf4j
public class ThrottleLoginInterceptor implements Interceptor {
    private static final long DEFAULT = Dates.s( 5 );

    private final ConcurrentMap<String, DateTime> attemptCache = new ConcurrentHashMap<>();
    public long delay;

    /**
     * @param delay timeout between login attempt. In seconds
     */
    public ThrottleLoginInterceptor( long delay ) {
        log.info( "delay {}", Dates.durationToString( delay ) );

        this.delay = delay;
    }

    public ThrottleLoginInterceptor() {
        this.delay = DEFAULT;
    }

    @Override
    public Optional<Response> before( InvocationContext context ) {
        var id = context.session.id;
        if( validateId( id ) || context.session.containsKey( SESSION_USER_KEY ) )
            return Optional.empty();
        log.trace( "Please wait {} before next attempt", Dates.durationToString( delay ) );
        return Optional.of( new Response( FORBIDDEN, "Please wait " + Dates.durationToString( delay ) + " before next attempt" ) );
    }

    /**
     * Utility method for managing timeline between login attempts
     *
     * @param id user session id
     */
    private synchronized boolean validateId( String id ) {
        var now = DateTime.now( UTC );
        var ts = attemptCache.putIfAbsent( id, now );
        if( ts == null ) {
            return true;
        }
        var duration = new Duration( ts, now );
        if( duration.getMillis() <= this.delay ) {
            log.trace( "{} is too short period has passed since previous attempt", Dates.durationToString( duration.getMillis() ) );
            attemptCache.computeIfPresent( id, ( k, v ) -> now );
            return false;
        }
        log.trace( "{} key has expired {}", id, Dates.durationToString( duration.getMillis() ) );
        attemptCache.remove( id );
        return true;
    }
}
