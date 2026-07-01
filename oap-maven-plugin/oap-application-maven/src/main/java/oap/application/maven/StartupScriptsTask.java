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

package oap.application.maven;

import oap.io.Files;
import oap.io.Resources;
import oap.util.Strings;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Properties;

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static oap.io.content.ContentReader.ofString;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@DisableCachingByDefault( because = "Generates scripts from project properties that are not tracked as task inputs" )
public abstract class StartupScriptsTask extends DefaultTask {
    @OutputDirectory
    public abstract DirectoryProperty getDestinationDirectory();

    @Input
    public abstract Property<Boolean> getFailIfUnsupportedOperationException();

    @TaskAction
    public void generate() {
        Properties properties = new Properties();
        for( Map.Entry<String, ?> entry : getProject().getProperties().entrySet() ) {
            if( entry.getValue() != null ) properties.setProperty( entry.getKey(), entry.getValue().toString() );
        }

        String destDir = getDestinationDirectory().get().getAsFile().getAbsolutePath();
        String serviceBin = properties.getOrDefault( "oap.service.home", "/opt/oap-service" ) + "/bin";
        Path functions = java.nio.file.Paths.get( destDir, serviceBin, "functions.sh" );
        Resources.read( getClass(), "/bin/functions.sh", ofString() )
            .ifPresent( value -> Files.writeString( functions, value ) );
        PosixFilePermission[] permissions = {
            OWNER_EXECUTE, OWNER_READ, OWNER_WRITE,
            GROUP_EXECUTE, GROUP_READ,
            OTHERS_EXECUTE, OTHERS_READ };
        script( properties, destDir, "/bin/oap.sh", serviceBin, ".sh", permissions );
        script( properties, destDir, "/bin/service.systemd", "usr/lib/systemd/system", ".service" );
        script( properties, destDir, "/bin/service.sysvinit", "etc/init.d", "", permissions );
    }

    private void script( Properties properties, String destDir, String script, String prefix, String suffix,
                         PosixFilePermission... permissions ) {
        Path path = java.nio.file.Paths.get( destDir, prefix,
            properties.getOrDefault( "oap.service.name", "oap-service" ) + suffix );
        Resources.read( getClass(), script, ofString() )
            .ifPresent( value -> Files.writeString( path,
                Strings.substitute( value, properties::getProperty ) ) );
        if( permissions.length > 0 ) {
            try {
                Files.setPosixPermissions( path, permissions );
            } catch( UnsupportedOperationException e ) {
                if( Boolean.TRUE.equals( getFailIfUnsupportedOperationException().getOrElse( false ) ) ) throw e;
                getLogger().error( e.getMessage() );
            }
        }
    }
}
