package oap.tools;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.content.ContentReader;
import oap.util.Result;
import org.joda.time.DateTimeUtils;

import javax.annotation.Nullable;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singleton;
import static oap.io.content.ContentWriter.ofBytes;
import static oap.io.content.ContentWriter.ofString;

/**
 * Compiles provided java sources using in-memory strategy
 * (no I/O at all), using memory file system. It saves
 * JAR file's descriptors while compiling many files,
 * closing it after operation. Result of compilation may be
 * fetched with 'compiledJavaFiles' field.
 * <p>
 * USAGE:
 * List<TemplateClassCompiler.SourceJavaFile> javaFiles = ...;
 * var compilationResult = new TemplateClassCompiler().compile( javaFiles );
 * if ( compilationResult.isSuccess() ) {
 * var compiledClasses = compilationResult.getSuccessValue();
 * ... // here 'TemplateClassSupplier' might be using to load & initialize the compiled classes
 * }
 */
@Slf4j
public class TemplateClassCompiler {
    private static final Counter METRICS_COMPILE = Metrics.counter( "oap_template", "type", "compile" );
    private static final Timer METRICS_COMPILE_TIME = Metrics.timer( "oap_template", "type", "compile_time_in_millis" );
    private static final Counter METRICS_DISK = Metrics.counter( "oap_template", "type", "disk" );
    private static final Counter METRICS_ERROR = Metrics.counter( "oap_template", "type", "error" );
    private final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
    private final List<String> options = getCompilationOptions();
    private final Path diskCache;
    DiagnosticCollector<JavaFileObject> diagnostics = null;
    Map<String, CompiledJavaFile> compiledJavaFiles = new HashMap<>();

    public TemplateClassCompiler( @Nullable Path diskCache ) {
        this.diskCache = diskCache;
    }

    public static class SourceJavaFile extends SimpleJavaFileObject {
        final String javaName;
        private final String content;

        public SourceJavaFile( String name, String content ) {
            super( URI.create( "memo:///" + name.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
            this.content = Objects.requireNonNull( content );
            this.javaName = Objects.requireNonNull( name );
        }

        @Override
        public CharSequence getCharContent( boolean ignore ) {
            return this.content;
        }

        @Override
        public String toString() {
            return "SourceJavaFile{" + javaName + ".java, " + content.length() + " bytes}";
        }
    }

    public static class CompiledJavaFile extends SimpleJavaFileObject {
        private final String className;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        CompiledJavaFile( String name ) {
            super( URI.create( "memo:///" + name.replace( '.', '/' ) + Kind.CLASS.extension ), Kind.CLASS );
            className = name;
        }

        @SneakyThrows
        CompiledJavaFile( String name, byte[] bytes ) {
            this( name );
            baos.write( bytes );
        }

        byte[] toByteArray() {
            return this.baos.toByteArray();
        }

        @Override
        public ByteArrayOutputStream openOutputStream() {
            return this.baos;
        }

        @Override
        public String toString() {
            return "CompiledJavaFile{" + className + ".class, " + baos.size() + " bytes}";
        }
    }

    private static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> implements Closeable, AutoCloseable {
        private final Map<String, CompiledJavaFile> compiledClasses;

        MemoryFileManager( Map<String, CompiledJavaFile> map, JavaCompiler compiler ) {
            super( compiler.getStandardFileManager( null, null, null ) );
            this.compiledClasses = map;
        }

        @Override
        public JavaFileObject getJavaFileForOutput( Location location, String name, JavaFileObject.Kind kind, FileObject source ) {
            var mc = new CompiledJavaFile( name );
            compiledClasses.put( name, mc );
            return mc;
        }

        /**
         * Closing FileManager is very important!
         * That prevents:
         * - from memory holding (GC cannot freeing it)
         * - from stuck in file pointers (every used JAR is left opened)
         */
        @Override
        public void close() {
            try {
                super.close();
            } catch( IOException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    /**
     * Main entry-point for compilation
     *
     * @param javaFiles list of files to be compiled
     * @return map of compilation result or failures
     */
    public Result<Map<String, CompiledJavaFile>, String> compile( List<SourceJavaFile> javaFiles ) {
        compiledJavaFiles = new HashMap<>();
        diagnostics = new DiagnosticCollector<>();
        try( var fileManager = new MemoryFileManager( compiledJavaFiles, ToolProvider.getSystemJavaCompiler() ) ) {
            boolean compilationOk = javaFiles
                .stream()
                .map( javaFile -> compileSingleFile( fileManager, javaFile, diskCache ) )
                .allMatch( Result::isSuccess );
            if( compilationOk ) {
                return Result.success( compiledJavaFiles );
            }
            return Result.failure( diagnostics.getDiagnostics().toString() );
        }
    }

    private Result<Map<String, CompiledJavaFile>, String> compileSingleFile(
        JavaFileManager fileManager, SourceJavaFile javaFile,
        Path diskCache ) {
        if( diskCache != null ) {
            var sourceFile = diskCache.resolve( javaFile.javaName + ".java" );
            var classFile = diskCache.resolve( javaFile.javaName + ".class" );
            if( Files.exists( classFile ) && Files.exists( sourceFile ) && sourceContentEquals( sourceFile, javaFile.content ) ) {
                var bytes = oap.io.Files.read( classFile, ContentReader.ofBytes() );

                compiledJavaFiles.put( javaFile.javaName, new CompiledJavaFile( javaFile.javaName, bytes ) );

                var currentTimeMillis = DateTimeUtils.currentTimeMillis();
                oap.io.Files.setLastModifiedTime( sourceFile, currentTimeMillis );
                oap.io.Files.setLastModifiedTime( classFile, currentTimeMillis );

                METRICS_DISK.increment();

                return Result.success( compiledJavaFiles );
            }
        }

        // Now compile!
        StringWriter outer = new StringWriter();
        JavaCompiler.CompilationTask compilationTask =
            javaCompiler.getTask(
                outer,
                fileManager,
                diagnostics,
                options,
                null,
                singleton( javaFile ) );
        long time = System.currentTimeMillis();
        boolean compilationOk = compilationTask.call();
        long tookForCompilation = System.currentTimeMillis() - time;
        METRICS_COMPILE_TIME.record( tookForCompilation, TimeUnit.MILLISECONDS );
        log.trace( "Compiling class '{}' ({}) took {} ms", javaFile.javaName,
            compilationOk ? "SUCCESS" : "FAILURE", tookForCompilation );
        if( compilationOk ) {
            METRICS_COMPILE.increment();
            if( diskCache != null ) {
                for( var source : compiledJavaFiles.values() ) {
                    var javaPath = diskCache.resolve( javaFile.javaName + ".java" );
                    var classPath = diskCache.resolve( javaFile.javaName + ".class" );

                    try {
                        oap.io.Files.write( javaPath, javaFile.content, ofString() );
                        var bytes = source.toByteArray();
                        oap.io.Files.write( classPath, bytes, ofBytes() );
                    } catch( Exception e ) {
                        oap.io.Files.delete( javaPath );
                        oap.io.Files.delete( classPath );
                    }
                }
            }

            return Result.success( compiledJavaFiles );
        }
        METRICS_ERROR.increment();
        log.warn( "Compilation '{}' failed with error: {}", javaFile.javaName, outer );
        return Result.failure( diagnostics.getDiagnostics().toString() );
    }

    private boolean sourceContentEquals( Path sourceFile, String filecontent ) {
        var source = oap.io.Files.read( sourceFile, ContentReader.ofString() );
        boolean res = filecontent.equals( source );
        if( !res ) {
            log.warn( "{}: file content != template content", sourceFile );
        }

        return res;
    }

    /**
     * @return result of compilation in case of failure
     */
    public DiagnosticCollector<JavaFileObject> getDiagnostics() {
        return diagnostics;
    }

    private List<String> getCompilationOptions() {
        List<String> options = new ArrayList<>();
        // add classpath: options.addAll( Lists.of( "-classpath", "." ) );
        // add debug info: options.add(includeDebugInfo ? "-g" : "-g:none");
        return options;
    }
}
