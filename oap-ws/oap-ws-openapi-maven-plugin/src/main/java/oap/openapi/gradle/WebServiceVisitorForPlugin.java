package oap.openapi.gradle;

import oap.application.module.Module;
import oap.application.module.Service;
import oap.ws.WsConfig;
import oap.ws.openapi.OpenapiGenerator;
import oap.ws.openapi.WebServiceVisitor;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class WebServiceVisitorForPlugin implements WebServiceVisitor {

    private final Logger log;
    private final OpenapiGenerator openapiGenerator;
    private final LinkedHashSet<String> moduleConfigurations;
    private final List<String> classpath;
    private final List<String> excludeModules;
    private final LinkedHashSet<String> description = new LinkedHashSet<>();
    private final ClassLoader classLoader;
    private String outputPath;

    public WebServiceVisitorForPlugin( OpenapiGenerator openapiGenerator,
                                       List<String> classpath,
                                       String outputPath,
                                       List<String> excludeModules,
                                       Logger log,
                                       ClassLoader classLoader ) {
        this.openapiGenerator = openapiGenerator;
        this.log = log;
        this.moduleConfigurations = new LinkedHashSet<>();
        this.classpath = classpath;
        this.outputPath = outputPath;
        this.excludeModules = excludeModules;
        this.classLoader = classLoader;
    }

    String getOutputPath() {
        return outputPath;
    }

    LinkedHashSet<String> getModuleConfigurations() {
        return moduleConfigurations;
    }

    LinkedHashSet<String> getDescription() {
        return description;
    }

    @Override
    public void visit( WsConfig wsService, Class<?> clazz, String basePath ) {
        OpenapiGenerator.Result result = openapiGenerator.processWebservice( clazz, wsService.path.stream().findFirst().orElse( "" ) );
        log.info( "WebService class " + clazz.getCanonicalName() + " " + result );
        description.add( clazz.getCanonicalName() );
    }

    @Override
    public Class<?> loadClass( Service service ) throws ClassNotFoundException {
        return classLoader.loadClass( service.implementation );
    }

    @Override
    public List<URL> getWebServiceUrls() {
        List<URL> urls = new ArrayList<>( Module.CONFIGURATION.urlsFromClassPath()
            .stream()
            .filter( url -> excludeModules == null || excludeModules.stream().noneMatch( element -> url.toString().contains( "/" + element + "/" ) ) )
            .filter( url -> moduleConfigurations.add( url.toString() ) )
            .toList() );
        if( classpath != null && !classpath.isEmpty() ) {
            outputPath = classpath.get( 0 ) + "/swagger";
            File file = new File( classpath.get( 0 ) + "/META-INF/oap-module.oap" );
            if( !Files.exists( Paths.get( file.getPath() ) ) ) {
                log.info( "File " + file.getPath() + " is missing, nothing to do" );
                return urls;
            }
            URL currentModuleUrl;
            try {
                currentModuleUrl = file.toURI().toURL();
            } catch( MalformedURLException e ) {
                throw new UncheckedIOException( e );
            }
            if( moduleConfigurations.add( currentModuleUrl.toString() ) ) urls.add( currentModuleUrl );
        }
        return urls;
    }
}
