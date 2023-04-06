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

package oap.tools;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.content.ContentReader;
import org.joda.time.DateTimeUtils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static oap.io.content.ContentWriter.ofString;

@Slf4j
public class MemoryClassLoaderJava extends ClassLoader {
    private static final Counter METRICS_COMPILE = Metrics.counter( "oap_template", "type", "compile" );
    private static final Counter METRICS_DISK = Metrics.counter( "oap_template", "type", "disk" );
    private static final Counter METRICS_ERROR = Metrics.counter( "oap_template", "type", "error" );

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private ConcurrentMap<String, Output> compiledTemplatesClasses = new ConcurrentHashMap<>();

    public MemoryClassLoaderJava( String classname, String filecontent, Path diskCache ) {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<Source> list = new ArrayList<>();

        if( diskCache != null ) {
            var sourceFile = diskCache.resolve( classname + ".java" );
            var classFile = diskCache.resolve( classname + ".class" );
            if( Files.exists( classFile ) ) {

                log.trace( "found: {}", classname );

                var bytes = oap.io.Files.read( classFile, ContentReader.ofBytes() );
                compiledTemplatesClasses.put( classname, new Output( classname, JavaFileObject.Kind.CLASS, bytes ) );
                var currentTimeMillis = DateTimeUtils.currentTimeMillis();
                oap.io.Files.setLastModifiedTime( sourceFile, currentTimeMillis );
                oap.io.Files.setLastModifiedTime( classFile, currentTimeMillis );

                METRICS_DISK.increment();
            } else {
                log.trace( "not found: {} -> {}", classname, sourceFile );
                list.add( new Source( classname, JavaFileObject.Kind.SOURCE, filecontent ) );
            }
        } else {
            list.add( new Source( classname, JavaFileObject.Kind.SOURCE, filecontent ) );
        }

        if( list.isEmpty() ) {
            try {
                findClass( classname );
            } catch( ClassFormatError e ) {
                log.trace( e.getMessage(), e );
                list.add( new Source( classname, JavaFileObject.Kind.SOURCE, filecontent ) );
            } catch( ClassNotFoundException ignored ) {
                log.warn( "Cannot generate|compile|find {}", list );
            }
        }

        if( !list.isEmpty() ) {
            var out = new StringWriter();
            MemoryFileManager manager = new MemoryFileManager( compiledTemplatesClasses, compiler );
            var task = compiler.getTask( out, manager, diagnostics, List.of(), null, list );
            if( task.call() ) {
                METRICS_COMPILE.increment();
                if( diskCache != null ) {
                    for( var source : list ) {
                        var javaFile = diskCache.resolve( source.originalName + ".java" );
                        var classFile = diskCache.resolve( source.originalName + ".class" );

                        try {
                            oap.io.Files.write( javaFile, source.content, ofString() );
                            var bytes = compiledTemplatesClasses.get( source.originalName ).toByteArray();
                            oap.io.Files.write( classFile, bytes );
                        } catch( Exception e ) {
                            oap.io.Files.delete( javaFile );
                            oap.io.Files.delete( classFile );
                        }
                    }
                }
                if( log.isDebugEnabled() && out.toString().length() > 0 ) log.debug( out.toString() );
            } else {
                METRICS_ERROR.increment();
                diagnostics.getDiagnostics().forEach( a -> {
                    if( a.getKind() == Diagnostic.Kind.ERROR ) {
                        log.error( "Could not compile {}: {}", list, a );
                    }
                } );
                if( out.toString().length() > 0 ) log.error( out.toString() );
            }
            try {
                manager.close();
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    @Override
    protected Class<?> findClass( String name ) throws ClassNotFoundException {
        var mc = compiledTemplatesClasses.remove( name );
        if( mc != null ) {
            var array = mc.toByteArray();
            return defineClass( name, array, 0, array.length );
        }
        return super.findClass( name );
    }

    private static class Source extends SimpleJavaFileObject {
        final String originalName;
        private final String content;

        Source( String name, Kind kind, String content ) {
            super( URI.create( "memo:///" + name.replace( '.', '/' ) + kind.extension ), kind );
            this.content = content;
            this.originalName = name;
        }

        @Override
        public CharSequence getCharContent( boolean ignore ) {
            return this.content;
        }
    }

    private static class Output extends SimpleJavaFileObject {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Output( String name, Kind kind ) {
            super( URI.create( "memo:///" + name.replace( '.', '/' ) + kind.extension ), kind );
        }

        @SneakyThrows
        Output( String name, Kind kind, byte[] bytes ) {
            this( name, kind );

            baos.write( bytes );
        }

        byte[] toByteArray() {
            return this.baos.toByteArray();
        }

        @Override
        public ByteArrayOutputStream openOutputStream() {
            return this.baos;
        }
    }

    private static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, Output> compiledClasses;

        MemoryFileManager( Map<String, Output> map, JavaCompiler compiler ) {
            super( compiler.getStandardFileManager( null, null, null ) );
            this.compiledClasses = map;
        }

        @Override
        public JavaFileObject getJavaFileForOutput( Location location, String name, JavaFileObject.Kind kind, FileObject source ) {
            var mc = new Output( name, kind );
            compiledClasses.put( name, mc );
            return mc;
        }
    }
}
