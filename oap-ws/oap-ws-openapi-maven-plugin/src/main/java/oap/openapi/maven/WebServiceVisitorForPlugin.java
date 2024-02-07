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

package oap.openapi.maven;

import oap.application.module.Module;
import oap.application.module.Service;
import oap.ws.WsConfig;
import oap.ws.openapi.OpenapiGenerator;
import oap.ws.openapi.WebServiceVisitor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

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

    private final Log log;
    private final OpenapiGenerator openapiGenerator;
    private final PluginDescriptor pluginDescriptor;
    private final LinkedHashSet<String> moduleConfigurations;
    private final List<String> classpath;
    private final List<String> excludeModules;
    private final LinkedHashSet<String> description = new LinkedHashSet<>();
    private String outputPath;

    public WebServiceVisitorForPlugin( PluginDescriptor pluginDescriptor,
                                       OpenapiGenerator openapiGenerator,
                                       List<String> classpath,
                                       String outputPath,
                                       List<String> excludeModules,
                                       Log log ) {
        this.pluginDescriptor = pluginDescriptor;
        this.openapiGenerator = openapiGenerator;
        this.log = log;
        this.moduleConfigurations = new LinkedHashSet<>();
        this.classpath = classpath;
        this.outputPath = outputPath;
        this.excludeModules = excludeModules;
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
        if( pluginDescriptor == null ) throw new ClassNotFoundException( "PluginDescriptor is null" );
        ClassRealm realm = pluginDescriptor.getClassRealm();
        return realm.loadClass( service.implementation );
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
            File file = new File( classpath.get( 0 ) + "/META-INF/oap-module.conf" );
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
