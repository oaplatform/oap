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

package oap.testng;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.Resources;
import oap.util.Dates;
import org.joda.time.DateTimeUtils;

import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class TestDirectoryFixture extends FixtureWithScope<TestDirectoryFixture> {
    public static final TestDirectoryFixture FIXTURE = new TestDirectoryFixture();

    private static final Path globalTestDirectory = Paths.get( "/tmp/test" );
    private static final Path testDirectory = globalTestDirectory().resolve( "test-" + Suite.uniqueExecutionId() );

    private final DeployTestData deployTestData;

    public TestDirectoryFixture() {
        this( null );
    }

    public TestDirectoryFixture( DeployTestData deployTestData ) {
        this.deployTestData = deployTestData;
    }

    public static Path globalTestDirectory() {
        return globalTestDirectory;
    }

    public static void deleteDirectory( Path path ) {
        try {
            Files.delete( path );
        } catch( UncheckedIOException e ) {
            Files.wildcard( globalTestDirectory(), "**/*" ).forEach( System.out::println );
            throw e;
        }
    }

    public static Path testDirectory() {
        return testDirectory;
    }

    public static Path testPath( String name ) {
        Path path = testDirectory().resolve( name.startsWith( "/" ) || name.startsWith( "\\" ) ? name.substring( 1 ) : name );
        Files.ensureFile( path );
        return path;
    }

    public static URI testUri( String name ) {
        return testPath( name ).toUri();
    }

    @SneakyThrows
    public static URL testUrl( String name ) {
        return testUri( name ).toURL();
    }

    public static Path deployTestData( Class<?> contextClass ) {
        return deployTestData( contextClass, "" );
    }

    public static Path deployTestData( Class<?> contextClass, String name ) {
        Path to = testPath( name );
        Resources.filePaths( contextClass, contextClass.getSimpleName() )
            .forEach( path -> Files.copyDirectory( path, to ) );
        return to;
    }

    private void deployTestData() {
        if( deployTestData != null ) deployTestData( deployTestData.contextClass, deployTestData.name );
    }

    public TestDirectoryFixture withDeployTestData( Class<?> contextClass, String name ) {
        return new TestDirectoryFixture( new DeployTestData( contextClass, name ) );
    }

    public TestDirectoryFixture withDeployTestData( Class<?> contextClass ) {
        return withDeployTestData( contextClass, "" );
    }

    @Override
    protected void before() {
        log.debug( "initializing test directory " + testDirectory() );
        Files.ensureDirectory( testDirectory() );

        deployTestData();
    }

    @Override
    public void afterSuite() {
        super.afterSuite();

        cleanTestDirectories();
    }

    @Override
    protected void after() {
        TestDirectoryFixture.deleteDirectory( testDirectory() );
    }

    @SneakyThrows
    private void cleanTestDirectories() {
        try( var stream = java.nio.file.Files.list( globalTestDirectory() ) ) {
            stream
                .filter( java.nio.file.Files::isDirectory )
                .filter( path -> Files.getLastModifiedTime( path ) < DateTimeUtils.currentTimeMillis() - Dates.h( 2 ) )
                .forEach( path -> {
                    try {
                        Files.delete( path );
                    } catch( Exception ignored ) {
                    }
                } );
        }
    }

    @AllArgsConstructor
    private static class DeployTestData {
        public final Class<?> contextClass;
        public final String name;
    }
}
