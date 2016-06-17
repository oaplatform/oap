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

import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiFunction;

public class Pair<K, V> implements Serializable {
   public final K _1;
   public final V _2;

   public Pair( K _1, V _2 ) {
      this._1 = _1;
      this._2 = _2;
   }

   public static <K, V> Pair<K, V> __( K _1, V _2 ) {
      return new Pair<>( _1, _2 );
   }

   public <KR, VR> Pair<KR, VR> map( BiFunction<K, V, Pair<KR, VR>> mapper ) {
      return mapper.apply( _1, _2 );
   }

   public <R> R fold( BiFunction<K, V, R> mapper ) {
      return mapper.apply( _1, _2 );
   }

   @Override
   public boolean equals( Object o ) {
      if( this == o ) return true;
      if( o == null || getClass() != o.getClass() ) return false;

      Pair pair = ( Pair ) o;

      if( _1 != null ? !Objects.deepEquals( _1, pair._1 ) : pair._1 != null ) return false;
      if( _2 != null ? !Objects.deepEquals( _2, pair._2 ) : pair._2 != null ) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = _1 != null ? _1.hashCode() : 0;
      result = 31 * result + ( _2 != null ? _2.hashCode() : 0 );
      return result;
   }

   @Override
   public String toString() {
      return "__(" + _1 + ", " + _2 + ')';
   }
}
