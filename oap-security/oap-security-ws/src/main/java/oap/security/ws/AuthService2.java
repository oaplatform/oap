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
import oap.security.acl.User2;
import oap.util.Stream;
import oap.ws.security.PasswordHasher;
import org.joda.time.DateTimeUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Created by igor.petrenko on 22.12.2017.
 */
public class AuthService2 {
    private final List<AuthProvider<User2>> providers;
    private final PasswordHasher passwordHasher;
    private final TokenCache tokens;

    public AuthService2( List<AuthProvider<User2>> providers, PasswordHasher passwordHasher, int expirationTime ) {
        this.providers = providers;
        this.passwordHasher = passwordHasher;
        this.tokens = new TokenCache( expirationTime );
    }

    public synchronized Optional<Token2> generateToken( String email, String password ) {
        return Stream.of( providers )
            .findFirstWithMap( p -> p.getByEmail( email ) )
            .filter( user -> passwordHasher.hashPassword( password ).equals( user.getPassword() ) )
            .map( this::generateToken );
    }

    public synchronized Optional<Token2> generateToken( String id ) {
        return Stream.of( providers )
            .findFirstWithMap( p -> p.getById( id ) )
            .map( this::generateToken );
    }

    public synchronized Token2 generateToken( User2 user ) {
        return tokens.getByUserId( user.getId(),
            () -> new Token2( UUID.randomUUID().toString(), user.getId(), DateTimeUtils.currentTimeMillis() ) );

    }

    public synchronized Optional<Token2> getToken( String tokenId ) {
        return tokens.get( tokenId );
    }

    public void invalidateUser( String id ) {
        tokens.invaidateByUserId( id );
    }

    @Slf4j
    private static class TokenCache {
        private final Cache<String, Token2> cache;

        public TokenCache( long expirationTime ) {
            this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess( expirationTime, TimeUnit.MILLISECONDS )
                .build();
        }

        public Optional<Token2> get( String tokenId ) {
            return Optional.ofNullable( cache.getIfPresent( tokenId ) );
        }

        public synchronized Optional<Token2> getByUserId( String userId ) {
            for( Token2 t : cache.asMap().values() )
                if( t.userId.equals( userId ) ) return Optional.of( t );
            return Optional.empty();
        }

        public synchronized Token2 getByUserId( String userId, Supplier<Token2> init ) {
            Optional<Token2> byUserId = getByUserId( userId );
            if( byUserId.isPresent() )
                log.debug( "updating existing token for user [{}]...", userId );
            Token2 token = byUserId.orElseGet( () -> {
                log.debug( "generating new token for user [{}]...", userId );
                return init.get();
            } );
            cache.put( token.id, token );
            return token;
        }

        public synchronized void invaidateByUserId( String userId ) {
            getByUserId( userId ).ifPresent( token -> {
                    log.debug( "deleting token [{}]...", token.id );
                    cache.invalidate( token.id );
                }
            );
        }
    }
}
