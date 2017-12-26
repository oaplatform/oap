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

package oap.security.ws;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.ws.security.PasswordHasher;
import org.joda.time.DateTimeUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by igor.petrenko on 22.12.2017.
 */
@Slf4j
public class AuthService2 {
    private final UserStorage2 userStorage;
    private final PasswordHasher passwordHasher;
    private final Cache<String, Token2> tokenStorage;

    public AuthService2( UserStorage2 userStorage, PasswordHasher passwordHasher, int expirationTime ) {
        this.userStorage = userStorage;
        this.passwordHasher = passwordHasher;
        this.tokenStorage = CacheBuilder.newBuilder()
            .expireAfterAccess( expirationTime, TimeUnit.MILLISECONDS )
            .build();
    }

    public synchronized Optional<Token2> generateToken( String id, String password ) {
        final User2 user = userStorage.get( id ).orElse( null );
        if( user == null ) return Optional.empty();

        val inputPassword = passwordHasher.hashPassword( password );
        if( !user.getPassword().equals( inputPassword ) ) return Optional.empty();

        return Optional.of( generateToken( user ) );
    }

    public synchronized Token2 generateToken( User2 user ) {
        Token2 token = null;

        for( Token2 t : tokenStorage.asMap().values() ) {
            if( t.userId.equals( user.getId() ) ) {
                token = t;
                break;
            }
        }

        if( token != null ) {
            log.debug( "Updating existing token for user [{}]...", user.getId() );
            tokenStorage.put( token.id, token );

            return token;
        }

        log.debug( "Generating new token for user [{}]...", user.getId() );
        token = new Token2( UUID.randomUUID().toString(), user.getId(), DateTimeUtils.currentTimeMillis() );

        tokenStorage.put( token.id, token );

        return token;
    }

    public synchronized Optional<Token2> getToken( String tokenId ) {
        return Optional.ofNullable( tokenStorage.getIfPresent( tokenId ) );
    }

    public void invalidateUser( String id ) {
        final ConcurrentMap<String, Token2> tokens = tokenStorage.asMap();

        for( Map.Entry<String, Token2> entry : tokens.entrySet() ) {
            if( Objects.equals( entry.getValue().userId, id ) ) {
                log.debug( "Deleting token [{}]...", entry.getKey() );
                tokenStorage.invalidate( entry.getKey() );

                return;
            }
        }
    }
}
