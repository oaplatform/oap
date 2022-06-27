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
package oap.util;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;

@EqualsAndHashCode( doNotUseGetters = true )
public final class Either<L, R> implements Serializable {
    public final L leftValue;
    public final R rightValue;

    private Either( L leftValue, R right ) {
        if( leftValue == null && right == null ) throw new IllegalArgumentException( "either left or right should be defined" );
        this.leftValue = leftValue;
        this.rightValue = right;
    }

    public static <L, R> Either<L, R> left( L value ) {
        return new Either<>( value, null );
    }

    public L left() {
        return leftValue;
    }

    public static <L, R> Either<L, R> right( R value ) {
        return new Either<>( null, value );
    }

    public R right() {
        return rightValue;
    }

    public Optional<L> getLeft() {
        return Optional.ofNullable( leftValue );
    }

    public Optional<R> getRight() {
        return Optional.ofNullable( rightValue );
    }

    public boolean isLeft() {
        return leftValue != null;
    }

    public boolean isRight() {
        return rightValue != null;
    }

    public <NL, NR> Either<NL, NR> map( Function<L, NL> mapLeft, Function<R, NR> mapRight ) {
        if( isLeft() ) return left( mapLeft.apply( leftValue ) );
        else return right( mapRight.apply( rightValue ) );
    }

    public <O> O map( Function<Either<L, R>, O> mapper ) {
        return mapper.apply( this );
    }

    @Override
    public String toString() {
        return isLeft()
            ? "Either.left(" + Strings.deepToString( leftValue ) + ")"
            : "Either.right(" + Strings.deepToString( rightValue ) + ")";
    }
}
