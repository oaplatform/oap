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
import lombok.ToString;
import oap.util.function.Try;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@ToString
@EqualsAndHashCode( doNotUseGetters = true )
public final class Result<S, F> implements Serializable {
    public final S successValue;
    public final F failureValue;
    private final boolean success;

    Result( S successValue, F failureValue, boolean success ) {
        this.successValue = successValue;
        this.failureValue = failureValue;
        this.success = success;
    }

    public static <S, F> Result<S, F> success( S value ) {
        return new Result<>( value, null, true );
    }

    public static <S, F> Result<S, F> failure( F value ) {
        return new Result<>( null, value, false );
    }

    public static <S> Result<S, Throwable> catching( Try.ThrowingSupplier<S> supplier ) {
        try {
            return success( supplier.get() );
        } catch( Throwable e ) {
            return failure( e );
        }

    }

    @Deprecated( forRemoval = true )
    public static <S> Result<S, Throwable> trying( oap.util.Try.ThrowingSupplier<S> supplier ) {
        return catching( supplier::get );
    }

    /**
     * @see #catchingInterruptible(Try.ThrowingSupplier)
     * reason: "blocking" is not actual semantics but usecase
     */
    @Deprecated
    public static <S> Result<S, Throwable> blockingTrying( oap.util.Try.ThrowingSupplier<S> supplier ) throws InterruptedException {
        return tryingInterruptible( supplier );
    }

    @Deprecated
    public static <S> Result<S, Throwable> tryingInterruptible( oap.util.Try.ThrowingSupplier<S> supplier ) throws InterruptedException {
        return catchingInterruptible( supplier::get );
    }

    public static <S> Result<S, Throwable> catchingInterruptible( Try.ThrowingSupplier<S> supplier ) throws InterruptedException {
        try {
            return success( supplier.get() );
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw e;
        } catch( Throwable e ) {
            return failure( e );
        }
    }

    public <NS, NF> Result<NS, NF> map( Function<S, Result<NS, NF>> onSuccess, Function<F, Result<NS, NF>> onFailure ) {
        if( success ) return onSuccess.apply( successValue );
        else return onFailure.apply( failureValue );
    }

    public Optional<Result<S, F>> filter( Predicate<S> predicate ) {
        return success && predicate.test( successValue ) ? Optional.of( this ) : Optional.empty();
    }

    public <R> Result<R, F> mapSuccess( Function<? super S, ? extends R> mapper ) {
        return success ? success( mapper.apply( successValue ) ) : failure( failureValue );
    }

    public <F2> Result<S, F2> mapFailure( Function<? super F, ? extends F2> mapper ) {
        return success ? success( successValue ) : failure( mapper.apply( failureValue ) );
    }

    public boolean isSuccess() {
        return success;
    }

    public Result<S, F> ifSuccess( Consumer<S> consumer ) {
        if( success ) consumer.accept( successValue );
        return this;
    }

    public Result<S, F> ifFailure( Consumer<F> consumer ) {
        if( !success ) consumer.accept( failureValue );
        return this;
    }

    public void ifSuccessOrElse( Consumer<S> onSuccess, Consumer<F> onFailure ) {
        ifSuccess( onSuccess ).ifFailure( onFailure );
    }

    public S getSuccessValue() {
        return successValue;
    }

    public F getFailureValue() {
        return failureValue;
    }

    public Optional<S> toOptional() {
        return isSuccess() ? Optional.of( successValue ) : Optional.empty();
    }

    public <X extends Throwable> S orElseThrow( Function<F, ? extends X> f ) throws X {
        if( success ) return successValue;
        else throw f.apply( failureValue );
    }

    public S orElse( S value ) {
        return success ? successValue : value;
    }

    @Override
    public String toString() {
        return success
            ? "Result.success (reason: " + Strings.deepToString( successValue ) + ")"
            : "Result.failure (reason: " + Strings.deepToString( failureValue ) + ")";
    }
}
