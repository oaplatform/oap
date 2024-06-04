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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.Resources;
import oap.util.Maps;

import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class Dictionaries {
    public static final String DEFAULT_PATH = "/opt/oap-dictionary";
    private static final Map<String, URL> dictionaries = new HashMap<>();
    private static final ConcurrentMap<String, DictionaryRoot> cache = new ConcurrentHashMap<>();

    static {
        addAllPaths( Files.fastWildcard( DEFAULT_PATH, "*.json" ) );
        addAllPaths( Files.fastWildcard( DEFAULT_PATH, "*.conf" ) );
        addAllPaths( Files.fastWildcard( DEFAULT_PATH, "*.yaml" ) );

        addAllUrls( Resources.urls( "dictionary", "json" ) );
        addAllUrls( Resources.urls( "dictionary", "conf" ) );
        addAllUrls( Resources.urls( "dictionary", "yaml" ) );

        log.info( "dictionaries: {}", dictionaries );
    }

    @SneakyThrows
    private static void addAllPaths( List<Path> dictionaries ) {
        for( var dictionaryPath : dictionaries ) {
            var dictionary = dictionaryPath.toUri().toURL();
            var name = Files.nameWithoutExtention( dictionary );
            Dictionaries.dictionaries.put( name, dictionary );
        }
    }

    private static void addAllUrls( List<URL> dictionaries ) {
        for( var dictionary : dictionaries ) {
            var name = Files.nameWithoutExtention( dictionary );
            Dictionaries.dictionaries.put( name, dictionary );
        }
    }

    public static Set<String> getDictionaryNames() {
        return dictionaries.keySet();
    }

    public static DictionaryRoot getDictionary( String name ) {
        return getDictionary( name, new DictionaryParser.AutoIdStrategy() );
    }

    public static DictionaryRoot getDictionary( String name, DictionaryParser.IdStrategy idStrategy ) {
        return Maps.get( dictionaries, name )
            .map( d -> DictionaryParser.parse( d, idStrategy ) )
            .orElseThrow( () -> new DictionaryNotFoundError( name ) );
    }

    public static DictionaryRoot getCachedDictionary( String name ) {
        return cache.computeIfAbsent( name, Dictionaries::getDictionary );
    }
}
