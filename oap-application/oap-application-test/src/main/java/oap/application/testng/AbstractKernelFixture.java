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

package oap.application.testng;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.application.ApplicationConfiguration;
import oap.application.Kernel;
import oap.application.module.Module;
import oap.http.test.HttpAsserts;
import oap.io.IoStreams;
import oap.io.Resources;
import oap.json.Binder;
import oap.json.JsonException;
import oap.reflect.TypeRef;
import oap.testng.AbstractFixture;
import oap.testng.TestDirectoryFixture;
import oap.util.Pair;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static oap.http.test.HttpAsserts.httpPrefix;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.util.Pair.__;

/**
 * variables:
 * <ul>
 *     <li>TEST_HTTP_PORT</li>
 *     <li>TEST_DIRECTORY</li>
 *     <li>TEST_RESOURCE_PATH</li>
 *     <li>TEST_HTTP_PREFIX</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractKernelFixture<Self extends AbstractKernelFixture<Self>> extends AbstractFixture<Self> {
    public static final String ANY = "*";
    public static final String TEST_HTTP_PORT = "TEST_HTTP_PORT";
    public static final String TEST_DIRECTORY = "TEST_DIRECTORY";
    public static final String TEST_RESOURCE_PATH = "TEST_RESOURCE_PATH";
    public static final String TEST_HTTP_PREFIX = "TEST_HTTP_PREFIX";
    protected final URL applicationConf;
    protected final List<URL> additionalModules = new ArrayList<>();
    protected final TestDirectoryFixture testDirectoryFixture;
    private final ArrayList<Pair<Class<?>, String>> confd = new ArrayList<>();
    private final ArrayList<Pair<Class<?>, String>> conf = new ArrayList<>();
    private final LinkedHashMap<String, AbstractFixture<?>> dependencies = new LinkedHashMap<>();
    private final LinkedHashSet<String> bootMain = new LinkedHashSet<>();
    public Kernel kernel;
    protected Path confdPath;
    private int testHttpPort;

    public AbstractKernelFixture( TestDirectoryFixture testDirectoryFixture, URL conf ) {
        this( testDirectoryFixture, conf, null, List.of() );
    }

    public AbstractKernelFixture( TestDirectoryFixture testDirectoryFixture, URL conf, Path confd ) {
        this( testDirectoryFixture, conf, confd, List.of() );
    }

    public AbstractKernelFixture( TestDirectoryFixture testDirectoryFixture, URL conf, List<URL> additionalModules ) {
        this( testDirectoryFixture, conf, null, additionalModules );
    }

    public AbstractKernelFixture( TestDirectoryFixture testDirectoryFixture, URL conf, Path confdPath, List<URL> additionalModules ) {
        this.applicationConf = conf;
        this.confdPath = confdPath;
        this.additionalModules.addAll( additionalModules );
        this.testDirectoryFixture = testDirectoryFixture;

        defineDefaults();
    }

    @SuppressWarnings( "unchecked" )
    public Self withFileProperties( URL location ) throws JsonException {
        var map = Binder.Format.of( location, true ).binder.unmarshal( new TypeRef<Map<String, Object>>() {}, location );
        properties.putAll( map );

        return ( Self ) this;
    }

    @SuppressWarnings( "unchecked" )
    public Self withProperties( Map<String, Object> properties ) {
        this.properties.putAll( properties );

        return ( Self ) this;
    }

    public int defaultHttpPort() {
        return testHttpPort;
    }

    protected void defineDefaults() {
        testHttpPort = definePort( TEST_HTTP_PORT );
        define( TEST_DIRECTORY, FilenameUtils.separatorsToUnix( testDirectoryFixture.testDirectory().toString() ) );
        String resourcePath = Resources.path( getClass(), "/" ).orElseThrow();
        define( TEST_RESOURCE_PATH, resourcePath );
        define( TEST_HTTP_PREFIX, httpPrefix( testHttpPort ) );
    }

    @SuppressWarnings( "unchecked" )
    public Self withConfdResources( Class<?> clazz, String confdResource ) throws UncheckedIOException {
        initConfd();

        confd.add( __( clazz, confdResource ) );

        return ( Self ) this;
    }

    public Self withLocalConfdResources( Class<?> clazz, String confdResource ) throws UncheckedIOException {
        var cr = confdResource.startsWith( "/" ) ? confdResource : "/" + confdResource;
        cr = clazz.getSimpleName() + cr;

        return withConfdResources( clazz, cr );
    }

    @SuppressWarnings( "unchecked" )
    public Self withConfResource( Class<?> clazz, String confdResource ) throws UncheckedIOException {
        initConfd();

        conf.add( __( clazz, confdResource ) );

        return ( Self ) this;
    }

    public Self withLocalConfResource( Class<?> clazz, String confdResource ) throws UncheckedIOException {
        var cr = confdResource.startsWith( "/" ) ? confdResource : "/" + confdResource;
        cr = clazz.getSimpleName() + cr;

        return withConfResource( clazz, cr );
    }

    public Self withAllowActiveByDefault( boolean allowActiveByDefault ) {
        return define( "main.allowActiveByDefault", allowActiveByDefault );
    }

    @SuppressWarnings( "unchecked" )
    public Self withBootMain( String... modules ) {
        this.bootMain.addAll( List.of( modules ) );

        return ( Self ) this;
    }

    private void initConfd() {
        if( this.confdPath == null )
            this.confdPath = testDirectoryFixture.testPath( "/application.test.confd" );
    }

    @Nonnull
    public <T> T service( @Nonnull String moduleName, @Nonnull Class<T> clazz ) throws IllegalArgumentException {
        return kernel.serviceOfClass( moduleName, clazz )
            .orElseThrow( () -> new IllegalArgumentException( "unknown service " + moduleName + ":" + clazz ) );
    }

    @Nonnull
    public <T> T service( @Nonnull String moduleName, @Nonnull String serviceName ) throws IllegalArgumentException {
        return kernel.<T>service( moduleName, serviceName )
            .orElseThrow( () -> new IllegalArgumentException( "unknown service " + moduleName + ":" + serviceName ) );
    }

    @Nonnull
    public <T> T service( @Nonnull String reference ) throws IllegalArgumentException {
        return kernel.<T>service( reference )
            .orElseThrow( () -> new IllegalArgumentException( "unknown service " + reference ) );
    }

    public <T> List<T> ofClass( Class<T> clazz ) {
        return kernel.ofClass( clazz );
    }

    public <T> List<T> ofClass( String moduleName, Class<T> clazz ) {
        return kernel.ofClass( moduleName, clazz );
    }

    @SneakyThrows
    @Override
    protected void before() {
        Preconditions.checkArgument( this.kernel == null );
        super.before();

        for( var cd : confd ) {
            Resources.filePaths( cd._1, cd._2 )
                .forEach( path -> {
                    if( path.toFile().exists() && path.toFile().isDirectory() ) {
                        log.info( "Copy directory " + path + " -> " + confdPath );
                        oap.io.Files.copyDirectory( path, confdPath );
                    } else {
                        if( !path.toFile().exists() ) log.warn( "Configuration directory " + path + " is not found" );
                        else log.warn( "Configuration directory " + path + " is not a directory" );
                    }
                } );
        }

        for( var cd : conf ) {
            var url = Resources.url( cd._1, cd._2 ).orElse( null );
            if( url == null ) {
                throw new FileNotFoundException( "Configuration file " + cd + " is not found" );
            }
            Path destPath = confdPath.resolve( FilenameUtils.getName( url.toString() ) );
            log.info( "Copying file " + url + " -> " + destPath );

            try( var is = IoStreams.in( url ) ) {
                IoStreams.write( destPath, PLAIN, is );
            } catch( IOException e ) {
                throw new UncheckedIOException( e );
            }
        }

        var moduleConfigurations = Module.CONFIGURATION.urlsFromClassPath();
        moduleConfigurations.addAll( additionalModules );
        this.kernel = new Kernel( "FixtureKernel#" + getClass().getCanonicalName(), moduleConfigurations );

        var confds = ApplicationConfiguration.getConfdUrls( confdPath );

        var kernelProperties = new LinkedHashMap<>( properties );

        dependencies.forEach( ( name, fixture ) -> {
            kernelProperties.put( name, fixture.getProperties() );
        } );

        var applicationConfiguration = ApplicationConfiguration.load( applicationConf, confds, kernelProperties );

        this.kernel.start( applicationConfiguration );
    }

    @Override
    protected void after() {
        if( this.kernel != null ) {
            this.kernel.stop();
            this.kernel = null;
        }
        super.after();
    }

    public String httpUrl( String url ) {
        return HttpAsserts.httpUrl( testHttpPort, url );
    }

    public void addDependency( String name, AbstractFixture<?> fixture ) {
        dependencies.put( name, fixture );
    }

    public Path testPath( String name ) {
        return testDirectoryFixture.testPath( name );
    }

    public Path testDirectory() {
        return testDirectoryFixture.testDirectory();
    }

    public URI testUri( String name ) {
        return testDirectoryFixture.testUri( name );
    }

    public URL testUrl( String name ) {
        return testDirectoryFixture.testUrl( name );
    }
}
