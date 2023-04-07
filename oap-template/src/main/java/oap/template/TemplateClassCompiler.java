package oap.template;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.util.Result;
import org.jetbrains.annotations.NotNull;

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
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singleton;

/**
 * Compiles provided java sources using in-memory strategy
 * (no I/O at all), using memory file system. It saves
 * JAR file's descriptors while compiling many files,
 * closing it after operation. Result of compilation may be
 * fetched with 'compiledJavaFiles' field.
 *
 * USAGE:
 * List<TemplateClassCompiler.SourceJavaFile> javaFiles = ...;
 * var compilationResult = new TemplateClassCompiler().compile( javaFiles );
 * if ( compilationResult.isSuccess() ) {
 *     var compiledClasses = compilationResult.getSuccessValue();
 *     ... // here 'TemplateClassSupplier' might be using to load & initialize the compiled classes
 * }
 */
@Slf4j
public class TemplateClassCompiler {
    DiagnosticCollector<JavaFileObject> diagnostics = null;

    Map<String, CompiledJavaFile> compiledJavaFiles = new HashMap<>();

    public static class SourceJavaFile extends SimpleJavaFileObject {
        final String javaName;
        private final String content;

        SourceJavaFile( String name, String content ) {
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

        CompiledJavaFile( String name, Kind kind ) {
            super( URI.create( "memo:///" + name.replace( '.', '/' ) + kind.extension ), kind );
            className = name;
        }

        @SneakyThrows
        CompiledJavaFile( String name, Kind kind, byte[] bytes ) {
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

        @Override
        public String toString() {
            return "CompiledJavaFile{" + className + ".class, " + baos.size() + " bytes}";
        }
    }

    private static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, CompiledJavaFile> compiledClasses;

        MemoryFileManager( Map<String, CompiledJavaFile> map, JavaCompiler compiler ) {
            super( compiler.getStandardFileManager( null, null, null ) );
            this.compiledClasses = map;
        }

        @Override
        public JavaFileObject getJavaFileForOutput( Location location, String name, JavaFileObject.Kind kind, FileObject source ) {
            var mc = new CompiledJavaFile( name, kind );
            compiledClasses.put( name, mc );
            return mc;
        }
    }

    /**
     * Main entry-point for compilation
     * @param javaFiles list of files to be compiled
     * @return map of compilation result or failures
     */
    public Result<Map<String, CompiledJavaFile>, String> compile( List<SourceJavaFile> javaFiles ) {
        compiledJavaFiles = new HashMap<>();
        diagnostics = new DiagnosticCollector<>();
        var fileManager = new MemoryFileManager( compiledJavaFiles, ToolProvider.getSystemJavaCompiler() );
        try {
            boolean compilationOk = javaFiles
                    .stream()
                    .map( javaFile -> compileSingleFile( fileManager, javaFile ) )
                    .allMatch( Result::isSuccess );
            if ( compilationOk ) return Result.success( compiledJavaFiles );
            return Result.failure( diagnostics.getDiagnostics().toString() );
        } finally {
            try {
                fileManager.close();
            } catch ( IOException e ) {
                throw new RuntimeException( "Cannot close file manager", e );
            }
        }
    }

    private Result<Map<String, CompiledJavaFile>, String> compileSingleFile( JavaFileManager fileManager, SourceJavaFile javaFile ) {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        // Set up the in-memory filesystem.
        List<String> options = getCompilationOptions();
        // Now compile!
        JavaCompiler.CompilationTask compilationTask =
                javaCompiler.getTask(
                        null, // Null: log any unhandled errors to stderr.
                        fileManager,
                        diagnostics,
                        options,
                        null,
                        singleton( javaFile ) );
        long time = System.currentTimeMillis();
        boolean compilationOk = compilationTask.call();
        log.trace( "Compiling class '{}' - {}. took {} ms...", javaFile.javaName, compilationOk ? "OK" : "FAILURE", System.currentTimeMillis() - time );
        if ( compilationOk ) return Result.success( compiledJavaFiles );
        return Result.failure( diagnostics.getDiagnostics().toString() );
    }

    /**
     * @return result of compilation in case of failure
     */
    public DiagnosticCollector<JavaFileObject> getDiagnostics() {
        return diagnostics;
    }

    @NotNull
    private List<String> getCompilationOptions() {
        List<String> options = new LinkedList<>();
        return options;
    }
}
