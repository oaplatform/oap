package oap.template;

import lombok.extern.slf4j.Slf4j;
import oap.util.Result;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Allows to load give compiled classes into main (parent) class loader
 * () or loads single class into separate classloader in case main class loader
 * is not provided with constructor.
 *
 * USAGE:
 * Map<String, TemplateClassCompiler.CompiledJavaFile> compiledJavaFiles = ... (for instance result of 'TemplateClassCompiler')
 * var supplier = new TemplateClassSupplier( compiledJavaFiles );
 * or
 * var supplier = new TemplateClassSupplier( new TemplateClassLoader( compiledJavaFiles ) );
 * var classes = supplier.loadClasses( Lists.of( "classA", "classB" ));
 * classes.get( "classA" ).getSuccessValue().getDeclaredConstructors()...
 */
@Slf4j
public class TemplateClassSupplier {

    private final Map<String, TemplateClassCompiler.CompiledJavaFile> compiledJavaFiles;
    private TemplateClassLoader mainClassLoader = null;

    public static class TemplateClassLoader extends ClassLoader {
        private final String className;
        private final Map<String, TemplateClassCompiler.CompiledJavaFile> compiledJavaFiles;
        private Set<String> loadedClasses = new HashSet<>();

        @Override
        public String toString() {
            return "TemplateClassLoader{'" + className + "'}";
        }

        public TemplateClassLoader( Map<String, TemplateClassCompiler.CompiledJavaFile> compiledJavaFiles ) {
            this( "parent", compiledJavaFiles );
        }

        public TemplateClassLoader( String className, Map<String, TemplateClassCompiler.CompiledJavaFile> compiledJavaFiles ) {
            super( "TemplatesClassLoader:" + className, getSystemClassLoader() );
            this.className = className;
            this.compiledJavaFiles = Objects.requireNonNull( compiledJavaFiles );
            if ( compiledJavaFiles.isEmpty() ) throw new IllegalArgumentException( "There are no compiled files" );
        }

        @Override
        protected Class<?> findClass( String name ) throws ClassNotFoundException {
            var mc = compiledJavaFiles.remove( name );
            if( mc != null ) {
                var array = mc.toByteArray();
                log.trace( "-> Loading class '{}' into {}", name, toString() );
                loadedClasses.add( name );
                return defineClass( name, array, 0, array.length );
            }
            log.trace( "<- Looking for class '{}' in {}", name, toString() );
            return super.findClass( name );
        }

        public Set<String> getLoadedClasses() {
            return Collections.unmodifiableSet( loadedClasses );
        }
    }

    /**
     * Allows to load classes provided within given classloader into same main (parent) classloader.
     * @param classLoader
     */
    public TemplateClassSupplier( TemplateClassLoader classLoader ) {
        this.compiledJavaFiles = Objects.requireNonNull( classLoader ).compiledJavaFiles;
        mainClassLoader = classLoader;
    }

    /**
     * Unlike another constructor it loads into separate clossloaders, each for loadClasses call
     * @param compiledJavaFiles
     */
    public TemplateClassSupplier( Map<String, TemplateClassCompiler.CompiledJavaFile> compiledJavaFiles ) {
        this.compiledJavaFiles = compiledJavaFiles;
        mainClassLoader = new TemplateClassLoader( compiledJavaFiles );
    }

    /**
     * Loads classes ready to be instantiated.
     * @param classNames of classes to be loaded
     * @return loaded classes ready to instantiate.
     */
    public Map<String, Result<Class, Throwable>> loadClasses( List<String> classNames ) {
        var classLoader = mainClassLoader;
        if ( mainClassLoader == null ) classLoader = new TemplateClassLoader( classNames.toString(), new HashMap<>( compiledJavaFiles ) );
        final var parentClassLoader = classLoader;
        Map<String, Result<Class, Throwable>> result = new HashMap<>();
        classNames.stream().forEach( className -> {
            try {
                result.put( className, Result.catchingInterruptible( () -> parentClassLoader.loadClass( className ) ) );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        } );
        return result;
    }
}
