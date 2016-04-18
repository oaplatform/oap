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
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Igor Petrenko on 15.04.2016.
 */
@Slf4j
public class Dictionaries {
    public static final List<String> dictionaries = new ArrayList<>();
    private static final ConcurrentHashMap<String, Dictionary> cache = new ConcurrentHashMap<>();

    private synchronized static void load() {
        if( dictionaries.isEmpty() ) {
            final Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                    .setUrls( ClasspathHelper.forPackage( "dictionary" ) )
                    .setScanners( new ResourcesScanner() )
            );

            final Set<String> resources = reflections.getResources( str -> str.endsWith( ".json" ) );

            log.info( "dictionaries: {}", resources );

            dictionaries.addAll( resources );
        }
    }

    public static Dictionary getDictionary( String name ) throws DictionaryNotFoundError {
        load();

        return dictionaries
            .stream()
            .filter( d -> d.contains( name ) )
            .findAny()
            .map( d -> DictionaryParser.parse( "/" + d ) )
            .orElseThrow( () -> new DictionaryNotFoundError( name ) );
    }

    public static Dictionary getCachedDictionary( String name ) throws DictionaryNotFoundError {
        return cache.computeIfAbsent( name, Dictionaries::getDictionary );
    }
}
