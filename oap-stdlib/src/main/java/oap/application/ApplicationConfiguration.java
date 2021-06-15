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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.typesafe.config.impl.ConfigImpl;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.content.ContentReader;
import oap.json.Binder;
import oap.util.Lists;
import oap.util.Stream;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static oap.io.Files.wildcard;
import static oap.util.Lists.concat;

@Slf4j
@ToString
public final class ApplicationConfiguration {
    public static final String PREFIX = "CONFIG.";
    public static final String OAP_PROFILE_PREFIX = "OAP_PROFILE_";
    public final LinkedHashMap<String, ApplicationConfigurationModule> services = new LinkedHashMap<>();
    public final ArrayList<Object> profiles = new ArrayList<>();
    public final ModuleBoot boot = new ModuleBoot();
    private LinkedHashSet<String> profilesCache = null;

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
        return load( appConfigPath, confdUrls, Map.of() );
    }

    public static ApplicationConfiguration load( URL appConfigPath, List<URL> confdUrls, Map<String, Object> map ) {
        return loadWithProperties( appConfigPath, Lists.concat( Lists.map( confdUrls, p -> {
                var content = ContentReader.read( p, ContentReader.ofString() );

                log.trace( "config: {}\n{}", p, content );

                return p.getPath().endsWith( ".yaml" )
                    ? Binder.json.marshal( Binder.yaml.unmarshal( Map.class, content ) )
                    : content;
            }
        ), List.of( Binder.json.marshal( map ) ) ) );
    }

    static ApplicationConfiguration load( Map<String, Object> properties ) {
        var sj = new StringJoiner( "\n" );
        properties.forEach( ( k, v ) -> sj.add( k + " = " + v ) );
        return Binder.hocon.unmarshal( ApplicationConfiguration.class, sj.toString() + "\n" + getEnvConfig() );
    }

    public static List<URL> getConfdUrls( Path confd ) {
        return confd != null
            ? Stream.of( wildcard( confd, "*.conf", "*.yaml" ) )
            .map( Files::toUrl )
            .toList() : List.of();
    }

    private static String getEnvConfig() {
        var res = new StringBuilder( "" );

        System.getenv().forEach( ( key, value ) -> {
            if( key.startsWith( PREFIX ) ) {
                res.append( key.substring( PREFIX.length() ) ).append( " = " ).append( value ).append( '\n' );
            }
        } );

        log.trace( "env config = {}", res );

        return res.toString();
    }

    private static void optimizeProfiles( ArrayList<String> profiles ) {
        var cache = new HashSet<String>();

        var it = profiles.listIterator( profiles.size() );
        while( it.hasPrevious() ) {
            var profile = it.previous();

            if( profile.startsWith( "-" ) ) profile = profile.substring( 1 );
            if( cache.contains( profile ) ) it.remove();
            else cache.add( profile );
        }
    }

    @SuppressWarnings( "unchecked" )
    public LinkedHashSet<String> getProfiles() {
        if( profilesCache == null ) {
            synchronized( this ) {
                if( profilesCache == null ) {
                    var p = new ArrayList<String>();

                    for( var profile : this.profiles ) {
                        if( profile instanceof String ) p.add( ( String ) profile );
                        else if( profile instanceof Map<?, ?> ) {

                            var conf = Binder.json.unmarshal( ProfileMap.class, Binder.json.marshal( profile ) );

                            if( conf.enabled )
                                p.add( conf.name );
                        }
                    }

                    addProfiles( p, System.getenv() );
                    addProfiles( p, System.getProperties() );

                    optimizeProfiles( p );

                    profilesCache = new LinkedHashSet<>( p );

                    log.info( "application profiles = {}", profilesCache );
                }
            }
        }

        return profilesCache;
    }

    public synchronized void reset() {
        profilesCache = null;
    }

    private void addProfiles( ArrayList<String> ret, Map<? extends Object, ? extends Object> env ) {
        env.forEach( ( nameObj, valueObj ) -> {
            var name = ( String ) nameObj;
            var value = ( String ) valueObj;
            if( name.startsWith( OAP_PROFILE_PREFIX ) ) {
                var profileName = name.substring( OAP_PROFILE_PREFIX.length() );
                var profileValue = value.trim().toUpperCase();
                profileName = profileEscape( profileName );
                var enabled = "1".equals( profileValue ) || "ON".equals( profileValue ) || "TRUE".equals( profileValue );

                ret.remove( ( enabled ? "-" : "" ) + profileName );
                ret.add( ( enabled ? "" : "-" ) + profileName );
            }
        } );
    }

    @SuppressWarnings( "checkstyle:ModifiedControlVariable" )
    private String profileEscape( String profileName ) {
        var ret = new StringBuilder();
        var escapeMode = false;

        for( var i = 0; i < profileName.length(); i++ ) {
            var ch = profileName.charAt( i );
            if( ch == '_' ) {
                if( !escapeMode ) escapeMode = true;
                else {
                    escapeMode = false;
                    ret.append( '_' );
                }

            } else if( escapeMode ) {
                if( i < profileName.length() - 2 ) {
                    var b = Byte.parseByte( profileName.substring( i, i + 2 ), 16 );
                    ret.append( ( char ) b );
                    escapeMode = false;
                    i += 1;
                } else throw new IllegalArgumentException( "invalid escape: " + i );
            } else {
                ret.append( ch );
            }
        }

        return ret.toString();
    }

    public static class ApplicationConfigurationModule extends LinkedHashMap<String, Object> {
        public boolean isEnabled() {
            return !Boolean.FALSE.equals( get( "enabled" ) );
        }
    }

    @ToString
    public static class ModuleBoot {
        public final LinkedHashSet<String> main = new LinkedHashSet<>();
    }

    public static class ProfileMap {
        public final String name;
        public boolean enabled = true;

        @JsonCreator
        public ProfileMap( String name ) {
            this.name = name;
        }
    }
}
