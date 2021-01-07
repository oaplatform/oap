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
package oap.application;

import com.typesafe.config.impl.ConfigImpl;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.json.Binder;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Stream;
import oap.util.Strings;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static oap.io.Files.wildcard;
import static oap.util.Lists.concat;

@Slf4j
@ToString
public final class ApplicationConfiguration {
    public static final String PREFIX = "CONFIG.";
    List<String> profiles = Lists.empty();
    Map<String, Map<String, Object>> services = Maps.of();

    private ApplicationConfiguration() {
    }

    @SneakyThrows
    public static ApplicationConfiguration loadWithProperties( Path appConfigPath, List<String> confdContents ) {
        return loadWithProperties( appConfigPath.toUri().toURL(), confdContents );
    }

    public static ApplicationConfiguration loadWithProperties( URL appConfigPath, List<String> confdContents ) {
        log.trace( "application configurations: {}, configs = {}", appConfigPath, confdContents );
        ConfigImpl.reloadSystemPropertiesConfig();
        ConfigImpl.reloadEnvVariablesConfig();
        return Binder.hoconWithConfig( concat( confdContents, List.of( getEnvConfig() ) ) )
            .unmarshal( ApplicationConfiguration.class, appConfigPath );
    }

    public static ApplicationConfiguration load() {
        return Binder.hocon.unmarshal( ApplicationConfiguration.class, getEnvConfig() );
    }

    public static ApplicationConfiguration load( Path appConfigPath ) {
        return loadWithProperties( appConfigPath, List.of() );
    }

    @SneakyThrows
    public static ApplicationConfiguration load( Path appConfigPath, Path confd ) {
        return load( appConfigPath.toUri().toURL(), confd.toString() );
    }

    public static ApplicationConfiguration load( URL appConfigPath, String confd ) {
        List<URL> confdUrls = getConfdUrls( Path.of( confd ) );
        log.info( "global configurations: {}", confdUrls );
        return load( appConfigPath, confdUrls );
    }

    public static ApplicationConfiguration load( URL appConfigPath, List<URL> confdUrls ) {
        return loadWithProperties( appConfigPath, Lists.map( confdUrls, p ->
            p.getPath().endsWith( ".yaml" )
                ? Binder.json.marshal( Binder.yaml.unmarshal( Map.class, p ) )
                : Strings.readString( p )
        ) );
    }

    public static List<URL> getConfdUrls( Path confd ) {
        return confd != null
            ? Stream.of( wildcard( confd, "*.conf", "*.yaml" ) )
            .map( Files::toUrl )
            .toList() : List.of();
    }

    private static String getEnvConfig() {
        var res = new StringBuilder( "{\n" );

        System.getenv().forEach( ( key, value ) -> {
            if( key.startsWith( PREFIX ) ) {
                res.append( key.substring( PREFIX.length() ) ).append( " = " ).append( value ).append( '\n' );
            }
        } );

        res.append( "}" );

        log.trace( "env config = {}", res );

        return res.toString();
    }
}
