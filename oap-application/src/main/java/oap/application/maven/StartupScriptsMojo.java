/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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
package oap.application.maven;

import oap.io.Files;
import oap.io.Resources;
import oap.util.Strings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Properties;

@Mojo( name = "startup-scripts", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class StartupScriptsMojo extends AbstractMojo {
    @Parameter( defaultValue = "${project.build.directory}/oap/scripts" )
    private String destinationDirectory;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Resources.readString( getClass(), "/bin/functions.sh" )
            .ifPresent( value -> Files.writeString(
                Files.path( destinationDirectory, "functions.sh" ), value ) );
        script( "/bin/oap.sh", ".sh" );
        script( "/bin/service.systemd", ".service" );
        script( "/bin/service.sysvinit", "" );
 }

    private void script( String script, String suffix ) {
        Properties properties = project.getProperties();
        Resources.readString( getClass(), script )
            .ifPresent( value ->
                Files.writeString(
                    Files.path( destinationDirectory,
                        properties.getOrDefault( "oap.service.name", "oap-service" ) + suffix ),
                    Strings.substitute( value, properties::getProperty ) ) );
    }
}
