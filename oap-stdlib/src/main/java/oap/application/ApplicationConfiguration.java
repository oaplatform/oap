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

import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.json.Binder;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Stream;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Slf4j
@ToString
public class ApplicationConfiguration {
    public static final String PREFIX = "CONFIG.";
    List<String> profiles = Lists.empty();
    Map<String, Map<String, Object>> services = Maps.empty();

    private ApplicationConfiguration() {
    }

    public static ApplicationConfiguration load() {
        return Binder.hocon.unmarshal( ApplicationConfiguration.class, getEnvConfig() );
    }

    public static ApplicationConfiguration load( Path appConfigPath ) {
        return load( appConfigPath, emptyList() );
    }

    @SneakyThrows
    public static ApplicationConfiguration load( Path appConfigPath, List<String> configs ) {
        return load( appConfigPath.toUri().toURL(), configs );
    }

    public static ApplicationConfiguration load( URL appConfigPath, List<String> configs ) {
        log.trace( "application configurations: {}, configs = {}", appConfigPath, asList( configs ) );

        return Binder.hoconWithConfig( configs )
            .unmarshal( ApplicationConfiguration.class, appConfigPath );
    }

    @SneakyThrows
    public static ApplicationConfiguration load( Path appConfigPath, Path confd ) {
        return load( appConfigPath.toUri().toURL(), confd.toString() );
    }

    public static ApplicationConfiguration load( URL appConfigPath, String confd ) {
        List<Path> paths = confd != null ? Files.wildcard( confd, "*.conf" ) : emptyList();
        log.info( "global configurations: {}", paths );

        var confs = Stream.of( paths ).map( Files::readString ).concat( getEnvConfig() ).collect( toList() );

        return load( appConfigPath, confs );
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
