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


import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.AbstractComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom ComponentConfigurator which adds the project's runtime classpath elements
 * to the plugin
 */
@Component( role = ComponentConfigurator.class, hint = "include-project-dependencies" )
public class IncludeProjectDependenciesComponentConfigurator extends AbstractComponentConfigurator {

    @Override
    public void configureComponent( Object component,
                                    PlexusConfiguration configuration,
                                    ExpressionEvaluator expressionEvaluator,
                                    ClassRealm containerRealm,
                                    ConfigurationListener listener ) throws ComponentConfigurationException {
        addProjectDependenciesToClassRealm( expressionEvaluator, containerRealm );

        ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();
        converter.processConfiguration(
            converterLookup,
            component,
            containerRealm.getParentClassLoader(),
            configuration,
            expressionEvaluator,
            listener
        );
    }

    @SuppressWarnings( "unchecked" )
    private void addProjectDependenciesToClassRealm( ExpressionEvaluator expressionEvaluator,
                                                     ClassRealm realm ) throws ComponentConfigurationException {
        List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = ( List<String> ) expressionEvaluator.evaluate( "${project.runtimeClasspathElements}" );
        } catch ( ExpressionEvaluationException e ) {
            throw new ComponentConfigurationException( "There was a problem evaluating: ${project.runtimeClasspathElements}", e );
        }

        // Add the project dependencies to the ClassRealm
        final URL[] urls = buildUrls( runtimeClasspathElements );
        for ( URL url : urls ) {
            realm.addURL( url );
        }
    }

    private URL[] buildUrls( List<String> runtimeClasspathElements ) throws ComponentConfigurationException {
        // Add the projects classes and dependencies
        List<URL> urls = new ArrayList<>( runtimeClasspathElements.size() );
        for ( String element : runtimeClasspathElements ) {
            try {
                URL url = new File( element ).toURI().toURL();
                urls.add( url );
            } catch ( MalformedURLException e ) {
                throw new ComponentConfigurationException( "Unable to access project dependency: " + element, e );
            }
        }

        // Add the plugin's dependencies (so Trove stuff works if Trove isn't on
        return urls.toArray( new URL[ 0 ] );
    }

}
