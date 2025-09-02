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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.google.JodaTicker;
import oap.io.Resources;
import oap.reflect.TypeRef;
import oap.template.render.AstRender;
import oap.template.render.AstRenderRoot;
import oap.template.render.TemplateAstUtils;
import oap.template.render.TemplateType;
import oap.template.tree.Elements;
import oap.util.Dates;
import oap.util.function.Try;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@ToString( of = { "ttl", "maxSize", "diskCache" } )
public class TemplateEngine implements Runnable {
    public static final String METRICS_NAME = "oap_template_cache";
    public final Path diskCache;
    public final long ttl;
    private final Map<String, List<Method>> builtInFunction = new HashMap<>();
    private final Cache<String, TemplateFunction> templates;
    public long maxSize = 1_000_000;

    public TemplateEngine( long ttl ) {
        this( null, ttl );
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

        log.info( "diskCache: {} ttl: {} functions: {}", diskCache, Dates.durationToString( ttl ), builtInFunction.keySet() );
        log.info( "functions: {}", builtInFunction.values().stream().flatMap( Collection::stream ).map( Method::getName ).distinct().toList() );

        Metrics.gauge( METRICS_NAME, Tags.of( "type", "size" ), templates, Cache::size );
        Metrics.gauge( METRICS_NAME, Tags.of( "type", "hit" ), templates, c -> c.stats().hitCount() );
        Metrics.gauge( METRICS_NAME, Tags.of( "type", "miss" ), templates, c -> c.stats().missCount() );
        Metrics.gauge( METRICS_NAME, Tags.of( "type", "eviction" ), templates, c -> c.stats().evictionCount() );
    }

    public static String getHashName( String template ) {
        long hash = getHash( template );
        return hashToName( hash );
    }

    public static long getHash( String template ) {
        HashFunction hashFunction = Hashing.murmur3_128();
        return hashFunction.hashUnencodedChars( template ).asLong();
    }

    private static String hashToName( long hash ) {
        return "template_" + ( hash >= 0 ? String.valueOf( hash ) : "_" + String.valueOf( hash ).substring( 1 ) );
    }

    private void loadFunctions() {
        HashSet<Class<?>> functions = new HashSet<Class<?>>();
        try( Stream<String> stream = Resources.lines( "META-INF/oap-template-macros.list" ) ) {
            stream.forEach( Try.consume( cs -> functions.add( Class.forName( cs ) ) ) );
        }

        for( Class<?> clazz : functions ) {
            for( Method method : clazz.getDeclaredMethods() ) {
                if( !Modifier.isStatic( method.getModifiers() ) ) continue;

                builtInFunction.computeIfAbsent( method.getName(), _ -> new ArrayList<>() ).add( method );
                JsonAlias jsonAlias = method.getAnnotation( JsonAlias.class );
                if( jsonAlias != null ) {
                    for( String alias : jsonAlias.value() ) {
                        builtInFunction.computeIfAbsent( alias, _ -> new ArrayList<>() ).add( method );
                    }
                }
            }
        }
    }

    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>> Template<TIn, TOut, TOutMutable, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Consumer<AstRender> postProcess ) {
        return getTemplate( name, type, template, acc, Map.of(), ErrorStrategy.ERROR, postProcess );
    }

    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>> Template<TIn, TOut, TOutMutable, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Map<String, String> aliases, Consumer<AstRender> postProcess ) {
        return getTemplate( name, type, template, acc, aliases, ErrorStrategy.ERROR, postProcess );
    }

    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>> Template<TIn, TOut, TOutMutable, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, ErrorStrategy errorStrategy, Consumer<AstRender> postProcess ) {
        return getTemplate( name, type, template, acc, Map.of(), errorStrategy, postProcess );
    }

    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>> Template<TIn, TOut, TOutMutable, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Map<String, String> aliases, ErrorStrategy errorStrategy ) {
        return getTemplate( name, type, template, acc, aliases, errorStrategy, _ -> {
        } );
    }

    @SuppressWarnings( "unchecked" )
    public <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>> Template<TIn, TOut, TOutMutable, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Map<String, String> aliases, ErrorStrategy errorStrategy, Consumer<AstRender> postProcess ) {
        Objects.requireNonNull( template );
        Objects.requireNonNull( acc );

        Hasher hasher = Hashing.murmur3_128().newHasher();
        hasher
            .putString( template, UTF_8 )
            .putString( acc.getClass().toString(), UTF_8 );

        if( postProcess != null ) hasher.putString( postProcess.getClass().toString(), UTF_8 );

        aliases.forEach( ( k, v ) -> hasher.putString( k, UTF_8 ).putString( v, UTF_8 ) );

        String id = hasher.hash().toString();

        log.trace( "id '{}' acc '{}' template '{}' aliases '{}'", id, acc.getClass(), template, aliases );

        try {
            TemplateFunction tFunc = templates.get( id, () -> {
                TemplateLexer lexer = new TemplateLexer( CharStreams.fromString( template ) );
                TemplateGrammar grammar = new TemplateGrammar( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
                if( errorStrategy == ErrorStrategy.ERROR ) {
                    lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
                    grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
                }
                Elements elements = grammar.elements( aliases ).ret;
                log.trace( "\n" + elements.print() );

                AstRenderRoot ast = TemplateAstUtils.toAst( elements, new TemplateType( type.type() ), builtInFunction, errorStrategy );

                if( postProcess != null )
                    postProcess.accept( ast );

                log.trace( "\n" + ast.print() );

                JavaTemplate<TIn, TOut, TOutMutable, TA> tf = new JavaTemplate<>( name + '_' + id, template, type, diskCache, acc, ast );
                return new TemplateFunction( tf, new Exception().getStackTrace() );
            } );

            return ( Template<TIn, TOut, TOutMutable, TA> ) tFunc.template;
        } catch( RejectedExecutionException e ) {
            throw e;
        } catch( Exception e ) {
            if( e.getCause() == null ) throw new RuntimeException( e );
            if( e.getCause() instanceof TemplateException ) {
                throw ( TemplateException ) e.getCause();
            }
            throw new TemplateException( e.getCause() );
        }
    }

    public long getCacheSize() {
        return templates.size();
    }

    @Override
    public void run() {
        templates.cleanUp();
        if( diskCache == null ) return;
        long now = System.currentTimeMillis();
        try( Stream<Path> stream = Files.walk( diskCache ) ) {
            stream
                .forEach( path -> {
                    try {
                        if( now - Files.getLastModifiedTime( path ).toMillis() > ttl ) {
                            log.debug( "delete {}", path );
                            Files.deleteIfExists( path );
                        }
                    } catch( Exception e ) {
                        log.error( "Cannot delete file/dir: {}", path, e );
                    }
                } );
        } catch( Exception e ) {
            log.error( "Could not walk through: " + diskCache, e );
        }
    }

    public static class TemplateFunction {
        @JsonIgnore
        public final Template<?, ?, ?, ?> template;
        public final StackTraceElement[] stackTrace;

        public TemplateFunction( Template<?, ?, ?, ?> template, StackTraceElement[] stackTrace ) {
            this.template = template;
            this.stackTrace = stackTrace;
        }
    }
}
