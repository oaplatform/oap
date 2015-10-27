/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
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
package oap.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class OptionalList<T> {
    private final List<T> list;

    private OptionalList( List<T> list ) {
        this.list = list;
    }

    public OptionalList<T> add( Optional<T> value ) {
        value.ifPresent( list::add );
        return this;
    }

    public Optional<List<T>> build() {
        return list.isEmpty() ? Optional.<List<T>>empty() : Optional.of( list );
    }

    public <B> Either<List<T>, B> toEigher( Supplier<B> value ) {
        return build().map( Either::<List<T>, B>left ).orElse( Either.right( value.get() ) );
    }

    public <B> Either<List<T>, B> toEigher( B value ) {
        return build().map( Either::<List<T>, B>left ).orElse( Either.right( value ) );
    }

    public static <TR> OptionalList<TR> builder() {
        return new OptionalList<>( new ArrayList<>() );
    }
}
