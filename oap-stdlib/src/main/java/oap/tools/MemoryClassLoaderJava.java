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
import java.util.List;

import static oap.io.Files.delete;
import static oap.io.Files.setLastModifiedTime;
import static oap.io.Files.write;
import static oap.io.content.ContentWriter.ofBytes;
import static oap.io.content.ContentWriter.ofString;

@Slf4j
public class MemoryClassLoaderJava {
    private static final Counter METRICS_COMPILE = Metrics.counter( "oap_template", "type", "compile" );
    private static final Counter METRICS_DISK = Metrics.counter( "oap_template", "type", "disk" );
    private static final Counter METRICS_ERROR = Metrics.counter( "oap_template", "type", "error" );

    private final CommonForTemplatesClassLoader classLoader;

    public Class<?> loadClass( String name ) throws ClassNotFoundException {
        return classLoader.loadClass( name );
    }

    private static class DiskCachedFiles {
        Path sourceFile;
        Path classFile;
        boolean classFileExists;
        boolean sourceFileExists;
    }

    public MemoryClassLoaderJava( CommonForTemplatesClassLoader classLoaderArg, String classname, String filecontent, Path diskCache ) {
        this.classLoader = classLoaderArg == null
                ? new CommonForTemplatesClassLoader()
                : classLoaderArg;

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Source notCompiledSource = null;

        DiskCachedFiles files = new DiskCachedFiles();
        if( diskCache != null ) {
            files.sourceFile = diskCache.resolve( classname + ".java" );
            files.classFile = diskCache.resolve( classname + ".class" );
            files.classFileExists = Files.exists( files.classFile );
            if( files.classFileExists ) {
                log.trace( "template class '{}' found", classname );
                var bytes = oap.io.Files.read( files.classFile, ContentReader.ofBytes() );
                classLoader.manager.putAsCompiledClass( classname, bytes );
                var currentTimeMillis = DateTimeUtils.currentTimeMillis();
                setLastModifiedTime( files.sourceFile, currentTimeMillis );
                setLastModifiedTime( files.classFile, currentTimeMillis );

                METRICS_DISK.increment();
            } else {
                log.trace( "template class '{}' not found, source '{}'", classname, files.sourceFile );
                notCompiledSource = new Source( classname, JavaFileObject.Kind.SOURCE, filecontent );
            }
        } else {
            log.trace( "template class as given source '{}' ", classname );
            notCompiledSource = new Source( classname, JavaFileObject.Kind.SOURCE, filecontent );
        }

        if( notCompiledSource == null ) {
            try {
                classLoader.findClass( classname );
            } catch( ClassFormatError cfe ) {
                log.trace( "Invalid class '{}'", classname, cfe );
                notCompiledSource = new Source( classname, JavaFileObject.Kind.SOURCE, filecontent );
            } catch( ClassNotFoundException ignored ) {
                log.trace( "Class '{}' not found", classname, ignored );
            }
        }

        // now notCompiledSources contains sources of templates
        if( notCompiledSource != null ) {
            var out = new StringWriter();
            var task = classLoader.compiler.getTask( out, classLoader.manager, diagnostics, List.of(), null, List.of( notCompiledSource ) );
            if( task.call() ) {
                //do compile
                METRICS_COMPILE.increment();
                if( diskCache != null ) {
                    files.sourceFile = diskCache.resolve( notCompiledSource.originalName + ".java" );
                    files.classFile = diskCache.resolve( notCompiledSource.originalName + ".class" );
                    try {
                        write( files.sourceFile, notCompiledSource.content, ofString() );
                        files.sourceFileExists = true;
                        var bytes = classLoader.manager.getAsClass( notCompiledSource.originalName, false );
                        write( files.classFile, bytes, ofBytes() );
                        files.classFileExists = true;
                    } catch( Exception e ) {
                        log.warn( "Template error found, deleting template source&class '{}'", notCompiledSource.originalName );
                        if ( files.sourceFileExists ) delete( files.sourceFile );
                        if ( files.classFileExists ) delete( files.classFile );
                    }
                }
                if( !out.getBuffer().isEmpty() ) log.debug( "source '" + out + "' is compiled" );
            } else {
                METRICS_ERROR.increment();
                diagnostics.getDiagnostics().forEach( a -> {
                    if( a.getKind() == Diagnostic.Kind.ERROR ) {
                        log.error( "Could not compile '{}'", a );
                    }
                } );
                if( !out.getBuffer().isEmpty() ) log.error( "Compiler error: {}", out.toString() );
            }
        }
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

    public CommonForTemplatesClassLoader getClassLoader() {
        return classLoader;
    }
}
