package oap.openapi.gradle;

import oap.io.Files;
import oap.ws.openapi.OpenapiGenerator;
import oap.ws.openapi.WebServicesWalker;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.StreamSupport;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@DisableCachingByDefault
public abstract class GenerateOpenApiTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getClasspath();

    @Input
    public abstract Property<String> getOutputPath();

    @Input
    public abstract Property<String> getOutputType();

    @Input
    @Optional
    public abstract Property<Boolean> getSkipDeprecated();

    @Input
    @Optional
    public abstract ListProperty<String> getExcludeModules();

    @TaskAction
    public void generate() throws Exception {
        Logger log = getLogger();
        log.info( "OpenAPI generation..." );

        String outputPathValue = getOutputPath().get();
        String outputTypeValue = getOutputType().get();
        boolean skipDep = getSkipDeprecated().getOrElse( true );
        List<String> excludeMods = getExcludeModules().getOrElse( List.of() );

        URL[] urls = StreamSupport.stream( getClasspath().spliterator(), false )
            .map( f -> {
                try {
                    return f.toURI().toURL();
                } catch( Exception e ) {
                    throw new RuntimeException( e );
                }
            } )
            .toArray( URL[]::new );

        URLClassLoader classLoader = new URLClassLoader( urls, Thread.currentThread().getContextClassLoader() );
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( classLoader );
        try {
            var settings = new OpenapiGenerator.Settings(
                OpenapiGenerator.Settings.OutputType.valueOf( outputTypeValue ),
                skipDep
            );
            var openapiGenerator = new OpenapiGenerator( "title", "", settings );
            openapiGenerator.beforeProcesingServices();

            List<String> classpathList = getClasspath().getFiles().stream().map( f -> f.getAbsolutePath() ).toList();
            var visitor = new WebServiceVisitorForPlugin( openapiGenerator, classpathList, outputPathValue, excludeMods, log, classLoader );
            WebServicesWalker.walk( visitor );

            log.info( "Configurations loaded: " + visitor.getModuleConfigurations() );
            String resolvedOutputPath = visitor.getOutputPath() + settings.outputType.fileExtension;

            java.nio.file.Path outPath = Paths.get( resolvedOutputPath );
            try {
                Files.ensureFile( outPath );
            } catch( Exception ex ) {
                log.info( "OpenAPI " + settings.outputType + " output path not found, skipping -> " + resolvedOutputPath );
                return;
            }
            openapiGenerator.afterProcesingServices();
            log.info( "OpenAPI " + settings.outputType + " generated -> " + resolvedOutputPath );
            Files.write( outPath, openapiGenerator.build(), settings.outputType.writer );
            log.info( "OpenAPI " + settings.outputType + " is written to " + resolvedOutputPath );
        } catch( Exception e ) {
            if( ReflectiveOperationException.class.isAssignableFrom( e.getClass() ) ) {
                log.error( "OpenAPI generator plugin error: " + e.getMessage() );
            } else if( e.getCause() != null && ReflectiveOperationException.class.isAssignableFrom( e.getCause().getClass() ) ) {
                log.error( "OpenAPI generator plugin error: " + e.getCause().getMessage() );
            } else {
                log.error( "OpenAPI generator plugin error: " + e.getMessage() );
            }
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader( original );
        }
    }
}
