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

import lombok.extern.slf4j.Slf4j;
import oap.reflect.TypeRef;
import oap.tools.MemoryClassLoaderJava13;
import oap.util.Functions;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

/**
 * Created by igor.petrenko on 2020-07-13.
 */
@Slf4j
public class JavaTemplate<TIn, TOut, TA extends TemplateAccumulator<TOut, TA>> implements Template2<TIn, TOut, TA> {
    private final Functions.TriConsumer<TIn, Map<String, Supplier<String>>, TemplateAccumulator<?, ?>> cons;
    private final TA acc;

    @SuppressWarnings( "unchecked" )
    public JavaTemplate( String name, TypeRef<TIn> type, String template, Map<String, List<Method>> builtInFunction, Path cacheFile, TA acc, ErrorStrategy errorStrategy ) {
        this.acc = acc;
        try {
            var lexer = new TemplateLexer( CharStreams.fromString( template ) );
            var grammar = new TemplateGrammar( new BufferedTokenStream( lexer ), builtInFunction, errorStrategy );
            if( errorStrategy == ErrorStrategy.ERROR ) {
                lexer.addErrorListener( ThrowingErrorListener.INSTANCE );
                grammar.addErrorListener( ThrowingErrorListener.INSTANCE );
            }

            var ast = grammar.template( new TemplateType( type.type() ) ).rootAst;

            log.trace( "\n" + ast.print() );

            var render = new Render( name, new TemplateType( type.type() ), acc.getClass(), null, null, 0 );
            ast.render( render );

            var line = new AtomicInteger( 0 );
            log.trace( "\n{}", new BufferedReader( new StringReader( render.out() ) )
                .lines()
                .map( l -> String.format( "%3d", line.incrementAndGet() ) + " " + l )
                .collect( joining( "\n" ) )
            );

            var fullTemplateName = getClass().getPackage().getName() + "." + render.nameEscaped();
            var mcl = new MemoryClassLoaderJava13( fullTemplateName, render.out(), cacheFile );
            cons = ( Functions.TriConsumer<TIn, Map<String, Supplier<String>>, TemplateAccumulator<?, ?>> ) mcl.loadClass( fullTemplateName ).getDeclaredConstructor().newInstance();
        } catch( Exception e ) {
            throw new TemplateException( e );
        }
    }

    public TOut render( TIn obj ) {
        var newAcc = acc.newInstance();
        cons.accept( obj, Map.of(), newAcc );

        return newAcc.get();
    }
}
