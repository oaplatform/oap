package oap.tools;

import lombok.extern.slf4j.Slf4j;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CommonTemplateClassLoader extends ClassLoader {
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final MemoryFileManager manager = new MemoryFileManager( compiler );

    @Override
    protected Class<?> findClass( String name ) throws ClassNotFoundException {
        synchronized( manager ) {
            var mc = manager.map.remove( name );
            if( mc != null ) {
                var array = mc.toByteArray();
                return defineClass( name, array, 0, array.length );
            }
        }
        return super.findClass( name );
    }

    public Boolean compile( StringWriter out, DiagnosticCollector<JavaFileObject> diagnostics, List<MemoryClassLoaderJava.Source> list ) {
        log.info( "!!! compile " + list );
        var task = compiler.getTask( out, manager, diagnostics, List.of(), null, list );
        return task.call();
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
