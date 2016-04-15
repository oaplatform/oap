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

package oap.dictionary;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Igor Petrenko on 14.04.2016.
 */
@EqualsAndHashCode
@ToString
public final class Dictionary {
   public final String name;
   public final List<DictionaryValue> values;

   private final HashMap<Long, String> indexByRtb = new HashMap<>();
   private final HashMap<String, Long> indexById = new HashMap<>();

   public Dictionary( String name, List<DictionaryValue> values ) {
      this.name = name;
      this.values = values;

      for( DictionaryValue dv : values ) {
         indexById.put( dv.id, dv.rtb );
         indexByRtb.put( dv.rtb, dv.id );
      }
   }

   public final String getOrDefautl( long rtb, String defaultValue ) {
      final String id = indexByRtb.get( rtb );
      if( id == null ) return defaultValue;
      return id;
   }

   public final long getOrDefault( String id, long defaultValue ) {
      final Long rtb = indexById.get( id );
      if( rtb == null ) return defaultValue;
      return rtb;
   }

   @EqualsAndHashCode
   @ToString
   public static class DictionaryValue {
      public final String id;
      public final String title;
      public final boolean enabled;
      public final long rtb;

      public DictionaryValue( String id, String title, boolean enabled, long rtb ) {
         this.id = id;
         this.title = title;
         this.enabled = enabled;
         this.rtb = rtb;
      }
   }
}
