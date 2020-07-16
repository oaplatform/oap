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

package oap.template;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;
import oap.reflect.TypeRef;
import oap.util.Dates;
import oap.util.Try;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by igor.petrenko on 2020-07-14.
 */
@Slf4j
public class TemplateEngine implements Runnable {
    public final Path tmpPath;
    public final long ttl;
    private final HashMap<String, List<Method>> builtInFunction = new HashMap<>();
    private final Cache<String, Template2<?, ?, ?>> templates;

    public TemplateEngine( Path tmpPath ) {
        this( tmpPath, Dates.d( 30 ) );
    }

    public TemplateEngine( Path tmpPath, long ttl ) {
        this.tmpPath = tmpPath;
        this.ttl = ttl;

        templates = CacheBuilder.newBuilder()
            .expireAfterAccess( ttl, TimeUnit.MILLISECONDS )
            .build();

        loadFunctions();

        log.info( "functions {}", builtInFunction.keySet() );

        Metrics.gauge( "oap_template_cache_size", templates, Cache::size );
    }

    public static String getHashName( String template ) {
        var hashFunction = Hashing.murmur3_128();

        var hash = hashFunction.hashUnencodedChars( template ).asLong();
        return hashToName( hash );
    }

    private static String hashToName( long hash ) {
        return "template_" + ( hash >= 0 ? String.valueOf( hash ) : "_" + String.valueOf( hash ).substring( 1 ) );
    }

    private void loadFunctions() {
        var functions = new HashSet<Class<?>>();
        Resources
            .lines( "META-INF/oap-template-macros.list" )
            .forEach( Try.consume( cs -> functions.add( Class.forName( cs ) ) ) );


        for( var clazz : functions ) {
            for( var method : clazz.getDeclaredMethods() ) {
                if( !Modifier.isStatic( method.getModifiers() ) ) continue;

                builtInFunction.computeIfAbsent( method.getName(), m -> new ArrayList<>() ).add( method );
            }
        }
    }

    public <TIn, TOut, TA extends TemplateAccumulator<TOut, TA>> Template2<TIn, TOut, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc ) {
        return getTemplate( name, type, template, acc, ErrorStrategy.ERROR );
    }

    @SuppressWarnings( "unchecked" )
    public <TIn, TOut, TA extends TemplateAccumulator<TOut, TA>> Template2<TIn, TOut, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc, ErrorStrategy errorStrategy ) {
        assert template != null;
        assert acc != null;
        
        var id = name + "_" + getHashName( template ) + "_" + getHashName( acc.getClass().getName() );

        log.trace( "id = {}, acc = {}, template = {}", id, acc.getClass(), template );

        var tFunc = ( Template2<TIn, TOut, TA> ) templates.getIfPresent( id );
        if( tFunc == null ) {
            synchronized( id.intern() ) {
                tFunc = ( Template2<TIn, TOut, TA> ) templates.getIfPresent( id );
                if( tFunc == null ) {
                    tFunc = new JavaTemplate<>( name, type, template, builtInFunction, tmpPath, acc, errorStrategy );
                    templates.put( id, tFunc );
                }
            }
        }

        return tFunc;
    }

    public long getCacheSize() {
        return templates.size();
    }

    @Override
    public void run() {
        try {
            templates.cleanUp();

            var now = System.currentTimeMillis();
            Files.walk( tmpPath ).forEach( path -> {
                try {
                    if( now - Files.getLastModifiedTime( path ).toMillis() > ttl ) {
                        log.debug( "delete {}", path );
                        Files.deleteIfExists( path );
                    }
                } catch( IOException e ) {
                    log.error( e.getMessage() );
                }
            } );
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }
    }
}
