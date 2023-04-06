package oap.tools;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CommonTemplateClassLoader extends ClassLoader {

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final MemoryFileManager manager = new MemoryFileManager( compiler );
    private Set<String> loadedClasses = Collections.synchronizedSet( new HashSet<>( 200, 0.75f ) );
    private Timer compilerTimer = Metrics.timer( "templates.compiler", "templates.compile", "time" );
    private Timer lookupTimer = Metrics.timer( "templates.lookup", "templates.lookup", "time" );

    @Override
    protected Class<?> findClass( String name ) throws ClassNotFoundException {
        long time = System.currentTimeMillis();
        try {
            if ( loadedClasses.add( name ) ) {
                synchronized ( manager ) {
                    var mc = manager.map.remove( name );
                    if ( mc != null ) {
                        var array = mc.toByteArray();
                        return defineClass( name, array, 0, array.length );
                    }
                }
            }
            return super.findClass( name );
        } finally {
            long lookupTime = System.currentTimeMillis() - time;
            lookupTimer.record( lookupTime, TimeUnit.MILLISECONDS );
        }
    }

    public Boolean compile( StringWriter out, DiagnosticCollector<JavaFileObject> diagnostics, List<MemoryClassLoaderJava.Source> list ) {
        long time = System.currentTimeMillis();
        try {
            var task = compiler.getTask( out, manager, diagnostics, List.of(), null, list );
            return task.call();
        } finally {
            String originalName = list.get( 0 ).originalName;
            long compilationTime = System.currentTimeMillis() - time;
            compilerTimer.record( compilationTime, TimeUnit.MILLISECONDS );
            log.trace( "Class '{}' compiled, took {} ms, total classes: {}", originalName, compilationTime, loadedClasses.size() + 1 );
        }
    }

    static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        final Map<String, MemoryClassLoaderJava.Output> map = new HashMap<>();

        MemoryFileManager( JavaCompiler compiler ) {
            super( compiler.getStandardFileManager( null, null, null ) );
        }

        @Override
        public JavaFileObject getJavaFileForOutput( Location location, String name, JavaFileObject.Kind kind, FileObject source ) {
            var mc = new MemoryClassLoaderJava.Output( name, kind );
            this.map.put( name, mc );
            return mc;
        }
    }
}
