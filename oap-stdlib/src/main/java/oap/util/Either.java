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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @see oap.util.Result
 * @param <A>
 * @param <B>
 */
@Deprecated
@EqualsAndHashCode
public final class Either<A, B> {
   public final A leftValue;
   public final B rightValue;
   private boolean isLeft;

   public Either( A leftValue, B rightValue, boolean isLeft ) {
      this.leftValue = leftValue;
      this.rightValue = rightValue;
      this.isLeft = isLeft;
   }

   public boolean isLeft() {
      return isLeft;
   }

   public boolean isRight() {
      return !isLeft;
   }

   public RightProjection right() {
      return new RightProjection( this );
   }

   public LeftProjection left() {
      return new LeftProjection( this );
   }

   public <X> X fold( Function<A, X> fa, Function<B, X> fb ) {
      return isLeft ? fa.apply( leftValue ) : fb.apply( rightValue );
   }

   public void consume( Consumer<Object> c ) {
      if( isLeft ) {
         c.accept( leftValue );
      } else
         c.accept( rightValue );
   }

   public Either<B, A> swap() {
      return isLeft ? right( leftValue ) : left( rightValue );
   }

   public static <A, B> Either<A, B> either( A leftValue, B rightValue, boolean isLeft ) {
      return new Either<>( leftValue, rightValue, isLeft );
   }

   @Override
   public String toString() {
      return isLeft ? "Left(" + leftValue + ")" : "Right(" + rightValue + ")";
   }

   public static <A, B> Either<A, B> left( final A a ) {
      return new Either<>( a, null, true );
   }

   public static <A, B> Either<A, B> right( final B b ) {
      return new Either<>( null, b, false );
   }


   public static <A, B> Either<List<A>, List<B>> fold2( List<Either<List<A>, B>> list ) {
      return fold2( Stream.of( list.stream() ) );
   }

   public static <A, B> Either<List<A>, List<B>> fold2( java.util.stream.Stream<Either<List<A>, B>> stream ) {
      return fold2( Stream.of( stream ) );
   }

   public Result<B, A> toResult() {
      return isRight() ? Result.success( rightValue ) : Result.failure( leftValue );
   }

   public static <A, B> Either<List<A>, List<B>> fold2( Stream<Either<List<A>, B>> stream ) {
      return stream
         .partition( Either::isLeft )
         .<Either<List<A>, List<B>>>fold( ( errorResults, valueResults ) -> {
            List<A> errors = errorResults.flatMap( e -> e.left().get().stream() ).toList();
            return errors.isEmpty() ?
               Either.right( valueResults.map( e -> e.right().get() ).toList() ) :
               Either.left( errors );
         } );
   }


   public final class LeftProjection {
      private Either<A, B> either;

      public LeftProjection( Either<A, B> either ) {
         this.either = either;
      }

      public A get() {
         if( either.isLeft() ) return either.leftValue;
         else throw new NoSuchElementException( "Either.leftValue on Right" );
      }

      public Optional<A> toOptional() {
         return either.isLeft() ? Optional.of( either.leftValue ) : Optional.empty();
      }

      public void forEach( Consumer<A> f ) {
         if( either.isLeft() ) f.accept( either.leftValue );
      }

      public A orElseGet( Supplier<? extends A> or ) {
         return isLeft() ? either.leftValue : or.get();
      }

      public <X> Either<X, B> map( Function<A, X> f ) {
         return isLeft() ? left( f.apply( either.leftValue ) ) : right( either.rightValue );
      }

      public <X> Either<X, B> flatMap( Function<A, Either<X, B>> f ) {
         return isLeft() ? f.apply( either.leftValue ) : right( either.rightValue );
      }
   }

   public final class RightProjection {
      private Either<A, B> either;

      public RightProjection( Either<A, B> either ) {
         this.either = either;
      }

      public B get() {
         if( either.isRight() ) return either.rightValue;
         else throw new NoSuchElementException( "Either.rightValue on Left" );
      }

      public Optional<B> toOptional() {
         return either.isRight() ? Optional.of( either.rightValue ) : Optional.empty();
      }

      public void forEach( Consumer<B> f ) {
         if( either.isRight() ) f.accept( either.rightValue );
      }

      public B orElseGet( Supplier<? extends B> or ) {
         return isRight() ? either.rightValue : or.get();
      }

      public <X> Either<A, X> map( Function<B, X> f ) {
         return isRight() ? right( f.apply( either.rightValue ) ) : left( either.leftValue );
      }

      public <X> Either<A, X> flatMap( Function<B, Either<A, X>> f ) {
         return isRight() ? f.apply( either.rightValue ) : left( either.leftValue );
      }
   }
}
