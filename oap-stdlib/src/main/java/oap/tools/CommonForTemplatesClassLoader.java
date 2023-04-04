package oap.tools;

import lombok.extern.slf4j.Slf4j;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class CommonForTemplatesClassLoader extends ClassLoader {
    private final Set<String> loadedClasses = new HashSet<>();
    protected JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    protected MemoryFileManager manager = new MemoryFileManager( compiler );
    private final Object sync = new Object();

    @Override
    protected Class<?> findClass( String name ) throws ClassNotFoundException {
        synchronized( sync ) {
            if ( loadedClasses.add( name ) && manager != null ) {
                var mc = manager.getAsClass( name, true );
                if ( mc != null ) {
                    log.info( "Looking for class {}, defining class {}...", name, name );
                    return defineClass( name, mc, 0, mc.length );
                }
            }
            log.info( "Looking for class {}...", name );
        }
        return super.findClass( name );
    }

    public Set<String> getLoadedClasses() {
        synchronized ( sync ) {
            return loadedClasses;
        }
    }

    static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final ConcurrentMap<String, MemoryClassLoaderJava.Output> internalMap = new ConcurrentHashMap<>();

        MemoryFileManager( JavaCompiler compiler ) {
            super( compiler.getStandardFileManager( null, null, null ) );
        }

        @Override
        public JavaFileObject getJavaFileForOutput( Location location, String name, JavaFileObject.Kind kind, FileObject source ) {
            var mc = new MemoryClassLoaderJava.Output( name, kind );
            this.internalMap.put( name, mc );
            return mc;
        }

        public void putAsCompiledClass( String classname, byte[] bytes ) {
            internalMap.put( classname, new MemoryClassLoaderJava.Output( classname, JavaFileObject.Kind.CLASS, bytes ) );
        }

        public byte[] getAsClass( String classname, boolean remove ) {
            MemoryClassLoaderJava.Output output = remove ? internalMap.remove( classname ) : internalMap.get( classname );
            return output != null && output.getKind() == JavaFileObject.Kind.CLASS
                    ? output.toByteArray()
                    : null;
        }
    }

}
