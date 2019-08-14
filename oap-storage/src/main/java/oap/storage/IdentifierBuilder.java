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
import oap.util.IdAccessorFactory;
import oap.util.Strings;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static oap.util.Strings.FriendlyIdOption.NO_VOWELS;

public final class IdentifierBuilder<T> {

    private static final int DEFAULT_ID_SIZE = 10;

    private final Function<T, String> getter;
    private final BiConsumer<T, String> setter;
    private Strings.FriendlyIdOption[] options = new Strings.FriendlyIdOption[] { NO_VOWELS };

    private Function<T, String> suggestion = obj -> Cuid.UNIQUE.next();

    private int length = DEFAULT_ID_SIZE;

    private IdentifierBuilder( Function<T, String> getter, BiConsumer<T, String> setter ) {
        this.getter = getter;
        this.setter = setter;
    }

    /**
     * Specifies the path in object where to look for an existing identifier (or set it in case its null
     * and suggestion is specified)
     *
     * @param path - path in object to look for existing identifier and generate it in case its null
     * @return instance of the current builder
     */
    public static <T> IdentifierBuilder<T> forPath( String path ) {
        Objects.requireNonNull( path, "path of id must not be null" );

        return new IdentifierBuilder<>( object -> Reflect.get( object, path ),
            ( object, value ) -> Reflect.set( object, path, value )
        );

    }

    @Deprecated
    public static <T> IdentifierBuilder<T> identityPath( String idPath ) {
        return forPath( idPath );
    }


    public static <T> IdentifierBuilder<T> forAnnotation() {
        return new IdentifierBuilder<>( IdAccessorFactory::getter, IdAccessorFactory::setter );

    }


    @Deprecated
    public static <T> IdentifierBuilder<T> annotation() {
        return forAnnotation();
    }

    /**
     * @see #forAnnotation()
     */
    @Deprecated
    public static <T> Identifier<T> annotationBuild() {
        return IdentifierBuilder.<T>forAnnotation().build();
    }

    /**
     * Specifies the existing identifier, which should be retrieved from object (assumes, its not null, otherwise
     * see {@link #forPath(String)}
     *
     * @param getter - existing identifier
     * @param <T>    - object type
     * @return instance of the current builder
     */
    public static <T> IdentifierBuilder<T> forId( Function<T, String> getter ) {
        return new IdentifierBuilder<>( Objects.requireNonNull(
            getter, "getter must not be null" ), null );
    }

    public static <T> IdentifierBuilder<T> forId( Function<T, String> getter, BiConsumer<T, String> setter ) {
        return new IdentifierBuilder<>( Objects.requireNonNull(
            getter, "getter must not be null" ), setter );
    }

    /**
     * Specifies where to get the base string for identifier generation (needs to be specified if
     * {@link #forPath(String)} is used and there is a chance for identifier to be null)
     *
     * @param suggestion - base string for identifier generation
     * @return instance of the current builder
     */
    public IdentifierBuilder<T> suggestion( Function<T, String> suggestion ) {
        this.suggestion = Objects.requireNonNull( suggestion, "suggestion must not be null" );

        return this;
    }

    /**
     * Sets the length of identifier to be generated
     *
     * @param length - desired identifier length
     * @return instance of the current builder
     */
    public IdentifierBuilder<T> length( int length ) {
        Preconditions.checkArgument( length > 0, "length needs to be bigger than 0" );
        this.length = length;

        return this;
    }

    @Deprecated
    public IdentifierBuilder<T> idOptions( Strings.FriendlyIdOption... idOptions ) {
        return options( idOptions );
    }

    public IdentifierBuilder<T> options( Strings.FriendlyIdOption... idOptions ) {
        this.options = idOptions;

        return this;
    }

    public Identifier<T> build() {
        return new DefaultIdentifier<>( this );
    }

    Function<T, String> getGetter() {
        return getter;
    }

    Optional<BiConsumer<T, String>> getSetter() {
        return Optional.ofNullable( setter );
    }

    Optional<Function<T, String>> getSuggestion() {
        return Optional.ofNullable( suggestion );
    }

    int getLength() {
        return length;
    }

    public Strings.FriendlyIdOption[] getOptions() {
        return options;
    }
}
