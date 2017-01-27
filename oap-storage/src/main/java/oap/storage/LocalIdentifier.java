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

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.lang.String.format;

final class LocalIdentifier<T> implements Identifier<T> {

    private static final UnaryOperator<String> REPLACE_SPECIAL = id -> id.replaceAll( "[^a-zA-Z0-9]+", "" );
    private static final UnaryOperator<String> REPLACE_VOWELS = id -> id.replaceAll( "[AEIOUaeiou]", "" );

    private final Function<T, String> getId;
    private final BiConsumer<T, String> setId;
    private final Function<T, String> suggestion;
    private final MemoryStorage<T> memoryStorage;
    private final int size;

    LocalIdentifier( final MemoryStorage<T> memoryStorage, final Function<T, String> suggestion,
                     final Function<T, String> getId, final BiConsumer<T, String> setId,
                     final int size ) {
        this.suggestion = suggestion == null ? null : suggestion.andThen( REPLACE_SPECIAL ).andThen( REPLACE_VOWELS );
        this.size = size;
        this.memoryStorage = Objects.requireNonNull( memoryStorage, "memory storage must not be null" );
        this.getId = getId;
        this.setId = setId;
    }

    @Override
    public String getId( final T object ) {
        String id = getId.apply( object );

        if( id == null ) {
            Preconditions.checkState( suggestion != null, "Suggestion is not specified for " +
                "nullable identifier" );
            Preconditions.checkState( setId != null, "Set of nullable identifier is not " +
                "specified" );

            id = suggestion.apply( object );

            if( id.length() > size ) {
                id = id.substring( 0, size );
            } else {
                for( int i = id.length(); i <= size; i++ ) {
                    id += "X";
                }
            }

            synchronized( memoryStorage ) {
                id = resolveConflicts( id );
            }

            setId.accept( object, id );
        }

        return id;
    }

    private String resolveConflicts( final String newId ) {
        String uniqueId = newId;

        int currentIdChar = uniqueId.length() - 1;
        while( identifierExists( uniqueId ) ) {
            if( currentIdChar != -1 ) {
                for( char symbol = 48; symbol < 122; symbol++ ) {
                    if( ( symbol > 57 && symbol < 65 ) || ( symbol > 91 && symbol < 97 ) ) {
                        continue;
                    }

                    if( identifierExists( uniqueId ) ) {
                        final char[] chars = uniqueId.toCharArray();

                        chars[currentIdChar] = symbol;

                        uniqueId = new String( chars );
                    } else {
                        break;
                    }
                }

                currentIdChar--;
            } else {
                throw new RuntimeException( format( "Couldn't generate non-duplicated id for following set up: " +
                        "storage size - [%s]; required id size - [%s]; last id tried - [%s]", memoryStorage.size(),
                    size, uniqueId ) );
            }
        }

        return uniqueId;
    }

    private boolean identifierExists( final String identifier ) {
        return memoryStorage.ids().stream().anyMatch( id -> id.equals( identifier ) );
    }

}
