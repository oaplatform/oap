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

package oap.maven;

import oap.io.Files;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

@Mojo( name = "copy", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class CopyMojo extends AbstractMojo {
    @Parameter( required = true )
    private String outputDirectory;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( required = true )
    private List<FileSet> fileSets;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Properties properties = project.getProperties();
        for( FileSet file : fileSets ) {
            Path path = Paths.get( file.getDirectory() ).toAbsolutePath();
            getLog().debug( "copy " + path + "(exists=" + path.toFile().exists() + ") to " + outputDirectory );
            getLog().debug( "includes = " + file.getIncludes() );
            getLog().debug( "excludes = " + file.getExcludes() );
            getLog().debug( "filtering = " + file.isFiltering() );
            if( path.toFile().exists() ) Files.copyContent( path, Paths.get( outputDirectory ),
                file.getIncludes(), file.getExcludes(),
                file.isFiltering(), properties::get );
        }
    }
}
