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
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static oap.io.content.ContentWriter.ofString;

@Slf4j
public class MemoryClassLoaderJava {
    private static final Counter METRICS_COMPILE = Metrics.counter( "oap_template", "type", "compile" );
    private static final Counter METRICS_DISK = Metrics.counter( "oap_template", "type", "disk" );
    private static final Counter METRICS_ERROR = Metrics.counter( "oap_template", "type", "error" );

    private CommonTemplateClassLoader classLoader;

    public MemoryClassLoaderJava( String classname, String filecontent, Path diskCache ) {
        this( new CommonTemplateClassLoader(), classname, filecontent, diskCache );
    }

    public Class<?> loadClass( String name ) throws ReflectiveOperationException {
        return classLoader.loadClass( name );
    }

    public MemoryClassLoaderJava( CommonTemplateClassLoader classLoader, String classname, String filecontent, Path diskCache ) {
        this.classLoader = classLoader;
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<Source> list = new ArrayList<>();

        if( diskCache != null ) {
            var sourceFile = diskCache.resolve( classname + ".java" );
            var classFile = diskCache.resolve( classname + ".class" );
            if( Files.exists( classFile ) ) {

//                log.trace( "found: {}", classname );

                var bytes = oap.io.Files.read( classFile, ContentReader.ofBytes() );
                classLoader.manager.map.put( classname, new Output( classname, JavaFileObject.Kind.CLASS, bytes ) );
                var currentTimeMillis = DateTimeUtils.currentTimeMillis();
                oap.io.Files.setLastModifiedTime( sourceFile, currentTimeMillis );
                oap.io.Files.setLastModifiedTime( classFile, currentTimeMillis );

                METRICS_DISK.increment();
            } else {
//                log.trace( "not found: {} -> {}", classname, sourceFile );
                list.add( new Source( classname, JavaFileObject.Kind.SOURCE, filecontent ) );
            }
        } else {
            list.add( new Source( classname, JavaFileObject.Kind.SOURCE, filecontent ) );
        }

        if( list.isEmpty() ) {
            try {
                classLoader.findClass( classname );
            } catch( ClassFormatError e ) {
                log.error( "Error in class {}", classname, e );
                list.add( new Source( classname, JavaFileObject.Kind.SOURCE, filecontent ) );
            } catch( ClassNotFoundException ignored ) {
                log.error( "not found class {}", classname  );
            }
        }

        if( !list.isEmpty() ) {
            var out = new StringWriter();
            if( classLoader.compile( out, diagnostics, list ) ) {
                METRICS_COMPILE.increment();
                if( diskCache != null ) {
                    for( var source : list ) {
                        var javaFile = diskCache.resolve( source.originalName + ".java" );
                        var classFile = diskCache.resolve( source.originalName + ".class" );

                        try {
                            oap.io.Files.write( javaFile, source.content, ofString() );
                            var bytes = classLoader.manager.map.get( source.originalName ).toByteArray();
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
//                        System.err.println( a );
                        log.error( "compilation error {}", a );
                    }
                } );
//                if( out.toString().length() > 0 ) log.error( out.toString() );
            }
        }
    }

    static class Source extends SimpleJavaFileObject {
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

    static class Output extends SimpleJavaFileObject {
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
}
