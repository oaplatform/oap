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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.security.acl.User2;
import oap.storage.Storage;
import oap.util.Stream;
import oap.sso.PasswordHasher;
import org.joda.time.DateTimeUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class AuthService2 implements Runnable {
    private final List<AuthProvider<User2>> providers;
    private final PasswordHasher passwordHasher;
    private final Storage<Token2> storage;
    private final long expirationTime;

    public AuthService2( List<AuthProvider<User2>> providers,
                         PasswordHasher passwordHasher,
                         Storage<Token2> storage, long expirationTime ) {
        this.providers = providers;
        this.passwordHasher = passwordHasher;
        this.storage = storage;
        this.expirationTime = expirationTime;
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
        return getTokenByUserId( user.getId(),
            () -> new Token2( null, user.getId(), DateTimeUtils.currentTimeMillis() ) );

    }

    public synchronized Optional<Token2> getToken( String tokenId ) {
        return storage.get( tokenId );
    }

    public void invalidateUser( String id ) {
        getTokenByUserId( id ).ifPresent( token -> {
                log.debug( "deleting token [{}]...", token.id );
                storage.delete( token.id );
            }
        );
    }

    @Override
    public void run() {
        val now = DateTimeUtils.currentTimeMillis();

        storage
            .select()
            .filter( t -> now - t.lastAccess > expirationTime )
            .forEach( t -> storage.delete( t.id ) );
    }

    private Optional<Token2> getTokenByUserId( String userId ) {
        return storage.select()
            .filter( t -> t.userId.equals( userId ) )
            .findFirst();
    }

    private Token2 getTokenByUserId( String userId, Supplier<Token2> init ) {

        return getTokenByUserId( userId )
            .map( token -> {
                log.debug( "updating existing token for user [{}]...", userId );
                token.lastAccess = DateTimeUtils.currentTimeMillis();
                return token;
            } ).orElseGet( () -> {
                log.debug( "generating new token for user [{}]...", userId );
                return storage.store( init.get() );
            } );
    }
}
