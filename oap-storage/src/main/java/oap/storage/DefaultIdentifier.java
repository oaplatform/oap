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

package oap.storage;

import oap.util.Strings;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static oap.util.Strings.FriendlyIdOption.FILL;
import static oap.util.Strings.FriendlyIdOption.NO_VOWELS;

public final class DefaultIdentifier<T> implements Identifier<T> {

    private final Function<? super T, String> getId;
    private final BiConsumer<? super T, String> setId;
    private final Function<? super T, String> suggestion;
    private final int size;

    DefaultIdentifier( final IdentifierBuilder<? super T> identifierBuilder ) {
        this.size = identifierBuilder.getSize();
        this.getId = identifierBuilder.getIdentityFunction();
        this.setId = identifierBuilder.getSetIdFunction().orElse( null );
        this.suggestion = identifierBuilder.getSuggestion().orElse( null );
    }

    @Override
    public String get( T object ) {
        return getId.apply( object );
    }

    @Override
    public synchronized String getOrInit( T object, Function<String, Optional<T>> storage ) {
        String id = getId.apply( object );

        if( id == null ) {
            Objects.requireNonNull( suggestion, "Suggestion is not specified for nullable identifier" );
            Objects.requireNonNull( setId, "Set of nullable identifier is not specified" );

            id = Strings.toUserFriendlyId( suggestion.apply( object ),
                size, newId -> storage.apply( newId ).isPresent(), NO_VOWELS, FILL );

            setId.accept( object, id );
        }

        return id;
    }

}
