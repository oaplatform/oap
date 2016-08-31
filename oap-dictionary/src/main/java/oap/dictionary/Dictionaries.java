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

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.Resources;
import oap.util.Maps;
import oap.util.Stream;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static oap.util.Pair.__;

/**
 * Created by Igor Petrenko on 15.04.2016.
 */
@Slf4j
public class Dictionaries {
   private static final Map<String, URL> dictionaries = new HashMap<>();
   private static final ConcurrentHashMap<String, DictionaryRoot> cache = new ConcurrentHashMap<>();

   private synchronized static void load() {
      if( dictionaries.isEmpty() ) {
         dictionaries.putAll( Stream.of( Resources.urls( "dictionary", "json" ) )
            .concat( Stream.of( Resources.urls( "dictionary", "conf" ) ) )
            .mapToPairs( r -> __( Files.nameWithoutExtention( r ), r ) )
            .toMap() );
         log.info( "dictionaries: {}", dictionaries );
      }
   }

   public static Set<String> getDictionaryNames() {
      load();

      return dictionaries.keySet();
   }

   public static DictionaryRoot getDictionary( String name ) throws DictionaryNotFoundError {
      load();

      return Maps.get( dictionaries, name )
         .map( DictionaryParser::parse )
         .orElseThrow( () -> new DictionaryNotFoundError( name ) );
   }

   public static DictionaryRoot getCachedDictionary( String name ) throws DictionaryNotFoundError {
      return cache.computeIfAbsent( name, Dictionaries::getDictionary );
   }
}
