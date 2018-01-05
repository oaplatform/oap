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
import oap.reflect.Reflect;
import oap.util.Cuid;
import oap.util.IdFactory;
import oap.util.Strings;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static oap.util.Strings.FriendlyIdOption.FILL;
import static oap.util.Strings.FriendlyIdOption.NO_VOWELS;

public final class IdentifierBuilder<T> {

    private static final int DEFAULT_ID_SIZE = 10;

    private final Function<T, String> getId;
    private final BiConsumer<T, String> setId;
    private Strings.FriendlyIdOption[] idOptions = new Strings.FriendlyIdOption[] { NO_VOWELS, FILL };

    private Function<T, String> suggestion = obj -> Cuid.next();

    private int size = DEFAULT_ID_SIZE;

    private IdentifierBuilder( final Function<T, String> getId, final BiConsumer<T, String> setId ) {
        this.getId = getId;
        this.setId = setId;
    }

    /**
     * Specifies the path in object where to look for an existing identifier (or set it in case its null
     * and suggestion is specified)
     *
     * @param idPath - path in object to look for existing identifier and generate it in case its null
     * @param <T>    - object type
     * @return instance of the current builder
     */
    public static <T> IdentifierBuilder<T> identityPath( final String idPath ) {
        final String path = Objects.requireNonNull( idPath, "path of id must not be null" );

        return new IdentifierBuilder<>( object -> Reflect.get( object, path ),
            ( object, value ) -> Reflect.set( object, path, value )
        );
    }

    public static <T> IdentifierBuilder<T> annotation() {
        return new IdentifierBuilder<>( IdFactory::getId, IdFactory::setId );
    }

    public static <T> Identifier<T> annotationBuild() {
        return new IdentifierBuilder<T>( IdFactory::getId, IdFactory::setId ).build();
    }

    /**
     * Specifies the existing identifier, which should be retrieved from object (assumes, its not null, otherwise
     * see {@link #identityPath(String)})
     *
     * @param identity - existing identifier
     * @param <T>      - object type
     * @return instance of the current builder
     */
    public static <T> IdentifierBuilder<T> identify( final Function<T, String> identity ) {
        return new IdentifierBuilder<>( Objects.requireNonNull(
            identity, "identity must not be null" ), null );
    }

    public static <T> IdentifierBuilder<T> identify( Function<T, String> identity, BiConsumer<T, String> setId ) {
        return new IdentifierBuilder<>( Objects.requireNonNull(
            identity, "identity must not be null" ), setId );
    }

    /**
     * Specifies where to get the base string for identifier generation (needs to be specified if
     * {@link #identityPath(String)} is used and there is a chance for identifier to be null)
     *
     * @param suggestion - base string for identifier generation
     * @return instance of the current builder
     */
    public IdentifierBuilder<T> suggestion( final Function<T, String> suggestion ) {
        this.suggestion = Objects.requireNonNull( suggestion, "suggestion must not be null" );

        return this;
    }

    /**
     * Sets the size of identifier to be generated
     *
     * @param size - desired identifier size
     * @return instance of the current builder
     */
    public IdentifierBuilder<T> size( final int size ) {
        Preconditions.checkArgument( size > 0, "size needs to be bigger than 0" );
        this.size = size;

        return this;
    }

    public IdentifierBuilder<T> idOptions( Strings.FriendlyIdOption... idOptions ) {
        this.idOptions = idOptions;

        return this;
    }

    public <T1 extends T> Identifier<T1> build() {
        return new DefaultIdentifier<>( this );
    }

    Function<T, String> getIdentityFunction() {
        return getId;
    }

    Optional<BiConsumer<T, String>> getSetIdFunction() {
        return Optional.ofNullable( setId );
    }

    Optional<Function<T, String>> getSuggestion() {
        return Optional.ofNullable( suggestion );
    }

    int getSize() {
        return size;
    }

    public Strings.FriendlyIdOption[] getIdOptions() {
        return idOptions;
    }
}
