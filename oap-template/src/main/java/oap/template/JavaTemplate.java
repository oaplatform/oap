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
import oap.tools.MemoryClassLoaderJava;
import oap.util.function.TriConsumer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Slf4j
public class JavaTemplate<TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>> implements Template<TIn, TOut, TOutMutable, TA> {
    private final TriConsumer<TIn, Map<String, Supplier<String>>, TemplateAccumulator<?, ?, ?>> cons;
    private final TA acc;

    private static ConcurrentMap<String, Holder> classLoaders = new ConcurrentHashMap<>();

    private static class Holder {
        MemoryClassLoaderJava classLoader;
        String source;
        Path cacheFile;
    }

    @SuppressWarnings( "unchecked" )
    public JavaTemplate( String name, String template, TypeRef<TIn> type, Path cacheFile, TA acc, AstRoot ast ) {
        this.acc = acc;
        try {
            var render = Render.init( name, template, new TemplateType( type.type() ), acc );
            ast.render( render );

//            var line = new AtomicInteger( 0 );
//            log.trace( "\n{}", new BufferedReader( new StringReader( render.out() ) )
//                .lines()
//                .map( l -> String.format( "%3d", line.incrementAndGet() ) + " " + l )
//                .collect( joining( "\n" ) )
//            );

            var fullTemplateName = getClass().getPackage().getName() + "." + render.nameEscaped();
            var mcl = classLoaders.compute( fullTemplateName, ( k, v ) -> {
                String fileContent = render.out();
                if ( v == null ) {
                    Holder holder = new Holder();
                    holder.classLoader = new MemoryClassLoaderJava( fullTemplateName, fileContent, cacheFile );
                    holder.source = fileContent;
                    holder.cacheFile = cacheFile;
                    return holder;
                }
                return v;
            } );
            cons = ( TriConsumer<TIn, Map<String, Supplier<String>>, TemplateAccumulator<?, ?, ?>> )
                    mcl.classLoader
                            .loadClass( fullTemplateName )
                            .getDeclaredConstructor()
                            .newInstance();
        } catch( Exception e ) {
            throw new TemplateException( e );
        }
    }

    public TOut render( TIn obj ) {
        var newAcc = acc.newInstance();
        cons.accept( obj, Map.of(), newAcc );

        return newAcc.get();
    }

    @Override
    public void render( TIn obj, TOutMutable tOut ) {
        var newAcc = acc.newInstance( tOut );
        cons.accept( obj, Map.of(), newAcc );
    }
}
