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

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Properties;

import static java.nio.file.attribute.PosixFilePermission.*;

@Mojo( name = "startup-scripts", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class StartupScriptsMojo extends AbstractMojo {
    @Parameter( defaultValue = "${project.build.directory}/oap/scripts" )
    private String destinationDirectory;

    @Parameter( defaultValue = "false" )
    private boolean failIfUnsupportedOperationException;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Properties properties = project.getProperties();
        String serviceBin = properties.getOrDefault( "oap.service.home", "/opt/oap-service" ) + "/bin";
        Path functions = Files.path( destinationDirectory, serviceBin, "functions.sh" );
        Resources.readString( getClass(), "/bin/functions.sh" )
            .ifPresent( value -> Files.writeString( functions, value ) );
        PosixFilePermission[] permissions = {
            OWNER_EXECUTE, OWNER_READ, OWNER_WRITE,
            GROUP_EXECUTE, GROUP_READ,
            OTHERS_EXECUTE, OTHERS_READ };
        script( "/bin/oap.sh", serviceBin, ".sh", permissions );
        script( "/bin/service.systemd", "usr/lib/systemd/system", ".service" );
        script( "/bin/service.sysvinit", "etc/initd", "", permissions );
    }

    private void script( String script, String preffix, String suffix, PosixFilePermission... permissions ) {
        Properties properties = project.getProperties();
        Path path = Files.path( destinationDirectory, preffix,
            properties.getOrDefault( "oap.service.name", "oap-service" ) + suffix );
        Resources.readString( getClass(), script )
            .ifPresent( value -> Files.writeString( path,
                Strings.substitute( value, properties::getProperty ) ) );
        if( permissions.length > 0 ) {
            try {
                Files.chmod( path, permissions );
            } catch( UnsupportedOperationException e ) {
                if( failIfUnsupportedOperationException ) throw e;
                getLog().error( e.getMessage() );
            }
        }
    }
}
