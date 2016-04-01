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

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@EqualsAndHashCode
@ToString
final public class Result<S, F> implements Serializable {
   public final S successValue;
   public final F failureValue;
   private boolean success = false;

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

   public S getSuccessValue() {
      return successValue;
   }

   public F getFailureValue() {
      return failureValue;
   }

   public Either<F, S> toEither() {
      return new Either<>( failureValue, successValue, !isSuccess() );
   }

   public <X extends Throwable> S orElseThrow( Function<F, ? extends X> f ) throws X {
      if( success ) return successValue;
      else throw f.apply( failureValue );
   }

   public S orElse( S value ) {
      return success ? successValue : value;
   }

   public static <S> Result<S, Throwable> trying( Try.ThrowingSupplier<S> supplier ) {
      try {
         return success( supplier.get() );
      } catch( Throwable e ) {
         return failure( e );
      }
   }
}
