package oap.openapi.maven;

import com.google.common.base.Joiner;
import oap.application.ApplicationException;
import oap.io.Files;
import oap.ws.openapi.OpenapiGenerator;
import oap.ws.openapi.WebServicesWalker;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * mvn oap:oap-ws-openapi-maven-plugin:18.13.1.5:openapi
 */

@Mojo(
    name = "openapi",
    defaultPhase = LifecyclePhase.COMPILE,
    configurator = "include-project-dependencies",
    requiresDependencyResolution = ResolutionScope.COMPILE,
    requiresDependencyCollection = ResolutionScope.COMPILE
)
public class OpenApiGeneratorPlugin extends AbstractMojo {

    @Parameter( property = "project.compileClasspathElements", required = true, readonly = true )
    private List<String> classpath;
    @Parameter( defaultValue = "${plugin}", required = true, readonly = true )
    private PluginDescriptor pluginDescriptor;
    @Parameter( required = true, readonly = true, defaultValue = "swagger" )
    private String outputPath;

    @Parameter( required = true, readonly = true, defaultValue = "JSON" )
    private String outputType;

    @Parameter( required = false, readonly = true, defaultValue = "true" )
    private String skipDeprecated;

    @Parameter( required = false, readonly = true )
    private List<String> excludeModules;

    @Override
    public void execute() {
        Objects.requireNonNull( outputPath );
        getLog().info( "OpenAPI generation..." );
        try {
            var settings = new OpenapiGenerator.Settings( OpenapiGenerator.Settings.OutputType.valueOf( outputType ), Boolean.valueOf( skipDeprecated ) );
            var openapiGenerator = new OpenapiGenerator( "title", "", settings );
            openapiGenerator.beforeProcesingServices();

            var visitor = new WebServiceVisitorForPlugin( pluginDescriptor, openapiGenerator, classpath, outputPath, excludeModules, getLog() );
            WebServicesWalker.walk( visitor );

            getLog().info( "Configurations (from oap-module.conf files) loaded: " + visitor.getModuleConfigurations() );
            openapiGenerator.setDescription( "WS services: " + Joiner.on( ", " ).join( visitor.getDescription() ) );
            outputPath = visitor.getOutputPath() + settings.outputType.fileExtension;
            try {
                Files.ensureFile( Paths.get( outputPath ) );
            } catch( Exception ex ) {
                //no such path, just ignore
                getLog().info( "OpenAPI " + settings.outputType + " output path not found, skipping -> " + outputPath );
                return;
            }
            openapiGenerator.afterProcesingServices();
            getLog().info( "OpenAPI " + settings.outputType + " generated -> " + outputPath );
            Files.write( Paths.get( outputPath ), openapiGenerator.build(), settings.outputType.writer );
            getLog().info( "OpenAPI " + settings.outputType + " is written to " + outputPath );
        } catch( Exception e ) {
            if ( ReflectiveOperationException.class.isAssignableFrom( e.getClass() ) ) {
                getLog().error( "OpenAPI generator plugin error: " + e.getMessage() );
            } else if ( e.getCause() != null && ReflectiveOperationException.class.isAssignableFrom( e.getCause().getClass() ) ) {
                getLog().error( "OpenAPI generator plugin error: " + e.getCause().getMessage() );
            } else {
                getLog().error( "OpenAPI generator plugin error", e );
            }
            throw new ApplicationException( e );
        }
    }

    void setOutputPath( String outputPath ) {
        this.outputPath = outputPath;
    }

    void setOutputType( String outputType ) {
        this.outputType = outputType;
    }

    void setExcludeModules( List<String> excludeModules ) {
        this.excludeModules = excludeModules;
    }
}
