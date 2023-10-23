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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import oap.google.JodaTicker;
import oap.io.Resources;
import oap.reflect.TypeRef;
import oap.util.Dates;
import oap.util.function.Try;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class TemplateEngine implements Runnable, AutoCloseable {
    public final Path diskCache;
    public final long ttl;
    private final Map<String, List<Method>> builtInFunction = new HashMap<>();
    private final Cache<String, TemplateFunction> templates;
    public long maxSize = 1_000_000L;

    public TemplateEngine() {
        this( null );
    }

    public TemplateEngine( Path diskCache ) {
        this( diskCache, Dates.d( 30 ) );
    }

    public TemplateEngine( Path diskCache, long ttl ) {
        this.diskCache = diskCache;
        this.ttl = ttl;

        templates = CacheBuilder.newBuilder()
            .ticker( JodaTicker.JODA_TICKER )
            .expireAfterAccess( ttl, TimeUnit.MILLISECONDS )
            .recordStats()
            .softValues()
            .concurrencyLevel( Runtime.getRuntime().availableProcessors() )
            .maximumSize( maxSize )
            .build();

        loadFunctions();

        log.info( "functions '{}' loaded", builtInFunction.keySet() );

        Metrics.gauge( "oap_template_cache", Tags.of( "type", "size" ), templates, Cache::size );
        Metrics.gauge( "oap_template_cache", Tags.of( "type", "hit" ), templates, c -> c.stats().hitCount() );
        Metrics.gauge( "oap_template_cache", Tags.of( "type", "miss" ), templates, c -> c.stats().missCount() );
        Metrics.gauge( "oap_template_cache", Tags.of( "type", "eviction" ), templates, c -> c.stats().evictionCount() );
    }

    public static String getHashName( String template ) {
        long hash = getHash( template );
        return hashToName( hash );
    }

    public static long getHash( String template ) {
        var hashFunction = Hashing.murmur3_128();

        return hashFunction.hashUnencodedChars( template ).asLong();
    }

    private static String hashToName( long hash ) {
        return "template_" + ( hash >= 0 ? String.valueOf( hash ) : "_" + String.valueOf( hash ).substring( 1 ) );
    }

    private void loadFunctions() {
        var functions = new HashSet<Class<?>>();
        try( Stream<String> stream = Resources.lines( "META-INF/oap-template-macros.list" ) ) {
            stream.forEach( Try.consume( cs -> functions.add( Class.forName( cs ) ) ) );
        }

        for( var clazz : functions ) {
            for( var method : clazz.getDeclaredMethods() ) {
                if( !Modifier.isStatic( method.getModifiers() ) ) continue;

                builtInFunction.computeIfAbsent( method.getName(), m -> new ArrayList<>() ).add( method );
            }
        }
    }

    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Consumer<Ast> postProcess ) {
        return getTemplate( name, type, template, acc, Map.of(), ErrorStrategy.ERROR, postProcess );
    }

    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Map<String, String> aliases, Consumer<Ast> postProcess ) {
        return getTemplate( name, type, template, acc, aliases, ErrorStrategy.ERROR, postProcess );
    }

    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc, ErrorStrategy errorStrategy, Consumer<Ast> postProcess ) {
        return getTemplate( name, type, template, acc, Map.of(), errorStrategy, postProcess );
    }

    @SuppressWarnings( "unchecked" )
    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Map<String, String> aliases, ErrorStrategy errorStrategy, Consumer<Ast> postProcess ) {
        Objects.requireNonNull( template );
        Objects.requireNonNull( acc );

        String id = getTemplateId( template, acc, aliases, postProcess );

//        log.trace( "id '{}' acc '{}' template '{}' aliases '{}'", id, acc.getClass(), template, aliases );

        try {
            TemplateFunction tFunc = templates.get( id, () -> {
                var lexer = new TemplateLexer( CharStreams.fromString( template ) );
                var grammar = new TemplateGrammar( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
                if( errorStrategy == ErrorStrategy.ERROR ) {
                    lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
                    grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
                }
                var ast = grammar.template( new TemplateType( type.type() ), aliases ).rootAst;
//                log.trace( "\n" + ast.print() );
                if( postProcess != null )
                    postProcess.accept( ast );

                var tf = new JavaTemplate<>( name + '_' + id, template, type, acc, ast, diskCache );
                return new TemplateFunction( tf );
            } );

            return ( Template<TIn, TOut, TOutMutable, TA> ) tFunc.template;
        } catch( Exception e ) {
            if( e.getCause() instanceof TemplateException te ) {
                throw te;
            }
            throw new TemplateException( e.getCause() );
        }
    }

    @NotNull
    private static <TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    String getTemplateId( String template, TA acc, Map<String, String> aliases, Consumer<Ast> postProcess ) {
        Hasher hasher = Hashing.murmur3_128().newHasher();
        hasher
            .putString( template, UTF_8 )
            .putString( acc.getClass().toString(), UTF_8 );

        if( postProcess != null ) hasher.putString( postProcess.getClass().toString(), UTF_8 );
        aliases.forEach( ( k, v ) -> hasher.putString( k, UTF_8 ).putString( v, UTF_8 ) );
        var id = hasher.hash().toString();
        return id;
    }

    public long getCacheSize() {
        return templates.size();
    }

    @Override
    public void run() {
        templates.cleanUp();
        var now = System.currentTimeMillis();
        try( Stream<Path> stream = Files.walk( diskCache ) ) {
            stream
                .forEach( path -> {
                    try {
                        if( now - Files.getLastModifiedTime( path ).toMillis() > ttl ) {
                            boolean deleted = Files.deleteIfExists( path );
                            log.debug( "delete files at '{}': {}", deleted ? "done" : "failure", path );
                        }
                    } catch( IOException e ) {
                        log.error( "Cannot delete files at '{}'", path, e );
                    }
                } );
            log.info( "TemplateEngine has done its work in {} ms", System.currentTimeMillis() - now );
        } catch( IOException e ) {
            log.error( "Could not walk through '{}'", diskCache, e );
        }
    }

    @Override
    public void close() throws Exception {
        templates.cleanUp();
    }

    public static class TemplateFunction {
        @JsonIgnore
        public final Template<?, ?, ?, ?> template;

        public TemplateFunction( Template<?, ?, ?, ?> template ) {
            this.template = template;
        }
    }
}
