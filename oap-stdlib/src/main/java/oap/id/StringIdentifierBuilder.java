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

import com.google.common.base.Preconditions;
import oap.reflect.Reflect;
import oap.util.Cuid;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static oap.id.Identifier.Option.COMPACT;

public final class StringIdentifierBuilder<T> extends AbstractBuilder<T, String> {
    public static final int DEFAULT_ID_SIZE = 10;
    protected Identifier.Option[] options = new Identifier.Option[] { COMPACT };
    protected Function<T, String> suggestion = obj -> Cuid.UNIQUE.next();
    protected int length = DEFAULT_ID_SIZE;

    private StringIdentifierBuilder( Function<T, String> getter, BiConsumer<T, String> setter ) {
        super( getter, setter );
    }

    public static <T> StringIdentifierBuilder<T> forId( Function<T, String> getter ) {
        return new StringIdentifierBuilder<>( getter, null );
    }

    public static <T> StringIdentifierBuilder<T> forId( Function<T, String> getter, BiConsumer<T, String> setter ) {
        return new StringIdentifierBuilder<>( getter, setter );
    }

    public static <T> StringIdentifierBuilder<T> forPath( String path ) {
        requireNonNull( path, "path of id must not be null" );

        return new StringIdentifierBuilder<>(
            object -> Reflect.get( object, path ),
            ( object, value ) -> Reflect.set( object, path, value )
        );
    }

    @Deprecated
    public static <T> StringIdentifierBuilder<T> identityPath( String idPath ) {
        return forPath( idPath );
    }


    public static <T> StringIdentifierBuilder<T> forAnnotation() {
        return new StringIdentifierBuilder<>( IdAccessor::get, IdAccessor::set );

    }

    public StringIdentifierBuilder<T> suggestion( Function<T, String> suggestion ) {
        this.suggestion = requireNonNull( suggestion, "suggestion must not be null" );

        return this;
    }

    public StringIdentifierBuilder<T> length( int length ) {
        Preconditions.checkArgument( length > 0, "length needs to be bigger than 0" );
        this.length = length;

        return this;
    }

    public StringIdentifierBuilder<T> options( Identifier.Option... options ) {
        this.options = options;
        return this;
    }

    public StringIdentifier<T> build() {
        return new StringIdentifier<>( this.getter, this.setter, this.suggestion, this.options, this.length );
    }
}
