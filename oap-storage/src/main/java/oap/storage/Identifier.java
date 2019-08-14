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

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface Identifier<T> {
    void set( T object, String id );

    String getOrInit( T object, Function<String, Optional<T>> storage );

    String get( T object );

    static <T> IdentifierBuilder<T> forPath( String path ) {
        return IdentifierBuilder.forPath( path );
    }

    static <T> IdentifierBuilder<T> forAnnotation() {
        return IdentifierBuilder.forAnnotation();
    }

    static <T> Identifier<T> forAnnotationFixed() {
        return IdentifierBuilder.<T>forAnnotation().build();
    }

    static <T> IdentifierBuilder<T> forId( final Function<T, String> getter ) {
        return IdentifierBuilder.forId( getter );
    }

    static <T> IdentifierBuilder<T> forId( final Function<T, String> getter, BiConsumer<T, String> setter ) {
        return IdentifierBuilder.forId( getter, setter );
    }
}
