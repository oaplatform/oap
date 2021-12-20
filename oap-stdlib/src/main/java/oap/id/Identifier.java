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

package oap.id;

import oap.util.Arrays;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static oap.id.Identifier.Option.COMPACT;
import static oap.id.Identifier.Option.FILL;

public interface Identifier<I, T> {
    void set( T object, I id );

    I getOrInit( T object, Predicate<I> conflict );

    I get( T object );

    I fromString( String id );

    String toString( I id );

    static <T> StringIdentifierBuilder<T> forPath( String path ) {
        return StringIdentifierBuilder.forPath( path );
    }

    static <T> StringIdentifierBuilder<T> forAnnotation() {
        return StringIdentifierBuilder.forAnnotation();
    }

    static <T> StringIdentifier<T> forAnnotationFixed() {
        return StringIdentifierBuilder.<T>forAnnotation().build();
    }

    static <T> StringIdentifierBuilder<T> forId( Function<T, String> getter ) {
        return StringIdentifierBuilder.forId( getter );
    }

    static <T> StringIdentifierBuilder<T> forId( Function<T, String> getter, BiConsumer<T, String> setter ) {
        return StringIdentifierBuilder.forId( getter, setter );
    }

    static <T, I> Predicate<I> toConflict( Function<I, Optional<T>> f ) {
        return id -> f.apply( id ).isPresent();
    }

    enum Option {
        COMPACT,
        FILL
    }

    Pattern SAFE_COMPACT = Pattern.compile( "[^bcdfghjklmnpqrstvwxz0-9]+", CASE_INSENSITIVE );
    Pattern SAFE = Pattern.compile( "[^abcdefghijklmnopqrstuvwxyz0-9_\\-]+", CASE_INSENSITIVE );

    static String generate( String base, int length, Predicate<String> conflict, Option... options ) {
        requireNonNull( base );

        String id = ( Arrays.contains( COMPACT, options ) ? SAFE_COMPACT : SAFE )
            .matcher( base )
            .replaceAll( "" )
            .toUpperCase();


        String baseId = id.length() > length
            ? id.substring( 0, length )
            : Arrays.contains( FILL, options )
                ? id + "X".repeat( length - id.length() )
                : id;

        StringBuilder sb = new StringBuilder( baseId );
        //10k max attempts
        for( int i = 0; i < 10000; i++ ) {
            if( !conflict.test( sb.toString() ) ) break;
            String suffix = Integer.toString( i, 36 ).toUpperCase();
            if( suffix.length() > length ) break;
            sb.replace( sb.length() - suffix.length(), sb.length(), suffix );
        }

        id = sb.toString();

        if( conflict.test( id ) )
            throw new IllegalArgumentException( format( "cannot resolve conflict for base '%s' with max id length %s", id, length ) );

        return id;
    }


}
