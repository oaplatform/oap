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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by igor.petrenko on 30.08.2016.
 */
@Slf4j
public class MemoryClassLoader extends ClassLoader {
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final MemoryFileManager manager = new MemoryFileManager( compiler );

    public MemoryClassLoader( String classname, String filecontent, Path diskCache ) {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<Source> list = new ArrayList<>();

        if( diskCache != null ) {
            final Path sourceFile = diskCache.resolve( classname + ".java" );
            final Path classFile = diskCache.resolve( classname + ".class" );
            if(
                Files.exists( sourceFile )
                    && filecontent.equals( oap.io.Files.readString( sourceFile ) )
                    && Files.exists( classFile ) ) {

                log.trace( "found: {}", classname );

                final byte[] bytes = oap.io.Files.read( classFile );
                manager.map.put( classname, new Output( classname, JavaFileObject.Kind.CLASS, bytes ) );
                val currentTimeMillis = DateTimeUtils.currentTimeMillis();
                oap.io.Files.setLastModifiedTime( sourceFile, currentTimeMillis );
                oap.io.Files.setLastModifiedTime( classFile, currentTimeMillis );
            } else {
                log.trace( "not found: {}", classname );
                list.add( new Source( classname, JavaFileObject.Kind.SOURCE, filecontent ) );
            }
        } else {
            list.add( new Source( classname, JavaFileObject.Kind.SOURCE, filecontent ) );
        }

        if( !list.isEmpty() ) {
            val out = new StringWriter();
            val task = compiler.getTask( out, manager, diagnostics, null, null, list );
            if( task.call() ) {
                if( diskCache != null ) {
                    for( Source source : list ) {
                        oap.io.Files.writeString( diskCache.resolve( source.originalName + ".java" ), source.content );
                        final byte[] bytes = manager.map.get( source.originalName ).toByteArray();
                        oap.io.Files.write( diskCache.resolve( source.originalName + ".class" ), bytes );
                    }
                }
                if( log.isDebugEnabled() && out.toString().length() > 0 ) log.debug( out.toString() );
            } else {
                diagnostics.getDiagnostics().forEach( a -> {
                    if( a.getKind() == Diagnostic.Kind.ERROR ) {
                        log.error( "{}", a );
                    }
                } );
                if( out.toString().length() > 0 ) log.error( out.toString() );
            }
        }
    }

    @Override
    protected Class<?> findClass( String name ) throws ClassNotFoundException {
        synchronized( manager ) {
            Output mc = manager.map.remove( name );
            if( mc != null ) {
                byte[] array = mc.toByteArray();
                return defineClass( name, array, 0, array.length );
            }
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
        private final Map<String, Output> map = new HashMap<>();

        MemoryFileManager( JavaCompiler compiler ) {
            super( compiler.getStandardFileManager( null, null, null ) );
        }

        @Override
        public JavaFileObject getJavaFileForOutput( Location location, String name, JavaFileObject.Kind kind, FileObject source ) {
            Output mc = new Output( name, kind );
            this.map.put( name, mc );
            return mc;
        }
    }
}
