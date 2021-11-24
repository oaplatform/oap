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
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import oap.io.Resources;
import oap.reflect.TypeRef;
import oap.util.Dates;
import oap.util.function.Try;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class TemplateEngine implements Runnable {
    public final Path tmpPath;
    public final long ttl;
    private final HashMap<String, List<Method>> builtInFunction = new HashMap<>();
    private final Cache<String, TemplateFunction> templates;
    public long maxSize = 1_000_000;

    public TemplateEngine( Path tmpPath ) {
        this( tmpPath, Dates.d( 30 ) );
    }

    public TemplateEngine( Path tmpPath, long ttl ) {
        this.tmpPath = tmpPath;
        this.ttl = ttl;

        templates = CacheBuilder.newBuilder()
            .expireAfterAccess( ttl, TimeUnit.MILLISECONDS )
            .recordStats()
            .maximumSize( maxSize )
            .build();

        loadFunctions();

        log.info( "functions {}", builtInFunction.keySet() );

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

    public <TIn, TOut, TA extends TemplateAccumulator<TOut, TA>> Template<TIn, TOut, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Consumer<Ast> postProcess ) {
        return getTemplate( name, type, template, acc, Map.of(), ErrorStrategy.ERROR, postProcess );
    }

    public <TIn, TOut, TA extends TemplateAccumulator<TOut, TA>> Template<TIn, TOut, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Map<String, String> aliases, Consumer<Ast> postProcess ) {
        return getTemplate( name, type, template, acc, aliases, ErrorStrategy.ERROR, postProcess );
    }

    public <TIn, TOut, TA extends TemplateAccumulator<TOut, TA>> Template<TIn, TOut, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, ErrorStrategy errorStrategy, Consumer<Ast> postProcess ) {
        return getTemplate( name, type, template, acc, Map.of(), errorStrategy, postProcess );
    }

    @SuppressWarnings( "unchecked" )
    public <TIn, TOut, TA extends TemplateAccumulator<TOut, TA>> Template<TIn, TOut, TA>
    getTemplate( String name, TypeRef<TIn> type, String template, TA acc, Map<String, String> aliases, ErrorStrategy errorStrategy, Consumer<Ast> postProcess ) {
        assert template != null;
        assert acc != null;

        var id = name
            + "_"
            + getHash( template )
            + "_"
            + aliases.hashCode()
            + "_"
            + acc.getClass().hashCode()
            + ( postProcess != null ? "_" + postProcess.getClass().hashCode() : "" );

        log.trace( "id '{}' acc '{}' template '{}' aliases '{}'", id, acc.getClass(), template, aliases );

        try {
            TemplateFunction tFunc = templates.get( id, () -> {
                var lexer = new TemplateLexer( CharStreams.fromString( template ) );
                var grammar = new TemplateGrammar( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
                if( errorStrategy == ErrorStrategy.ERROR ) {
                    lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
                    grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
                }

                var ast = grammar.template( new TemplateType( type.type() ), aliases ).rootAst;

                log.trace( "\n" + ast.print() );

                if( postProcess != null )
                    postProcess.accept( ast );

                var tf = new JavaTemplate<>( name, type, tmpPath, acc, ast );
                return new TemplateFunction( tf, new Exception().getStackTrace() );

            } );

            return ( Template<TIn, TOut, TA> ) tFunc.template;
        } catch( UncheckedExecutionException | ExecutionException e ) {
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

    public static class TemplateFunction {
        @JsonIgnore
        public final Template<?, ?, ?> template;
        public final StackTraceElement[] stackTrace;

        public TemplateFunction( Template<?, ?, ?> template, StackTraceElement[] stackTrace ) {
            this.template = template;
            this.stackTrace = stackTrace;
        }
    }
}
