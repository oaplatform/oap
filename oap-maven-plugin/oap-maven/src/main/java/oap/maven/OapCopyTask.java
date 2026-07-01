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
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@DisableCachingByDefault( because = "Output depends on project properties that are not tracked as task inputs" )
public abstract class OapCopyTask extends DefaultTask {
    @OutputDirectory
    private File outputDirectory;

    @Input
    private List<FileSet> fileSets;

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory( File outputDirectory ) {
        this.outputDirectory = outputDirectory;
    }

    public List<FileSet> getFileSets() {
        return fileSets;
    }

    public void setFileSets( List<FileSet> fileSets ) {
        this.fileSets = fileSets;
    }

    @TaskAction
    public void copy() {
        Map<String, ?> properties = getProject().getProperties();
        for( FileSet file : fileSets ) {
            Path path = Paths.get( file.getDirectory() ).toAbsolutePath();
            getLogger().debug( "copy " + path + "(exists=" + path.toFile().exists() + ") to " + outputDirectory );
            getLogger().debug( "includes = " + file.getIncludes() );
            getLogger().debug( "excludes = " + file.getExcludes() );
            getLogger().debug( "filtering = " + file.isFiltering() );
            if( path.toFile().exists() ) Files.copyContent( path, outputDirectory.toPath(),
                file.getIncludes(), file.getExcludes(),
                file.isFiltering(), properties::get );
        }
    }
}
