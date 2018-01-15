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

package oap.dictionary;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.Resources;
import oap.util.Pair;
import oap.util.Stream;
import oap.util.Try;
import org.apache.commons.io.FilenameUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static oap.util.Maps.Collectors.toMap;
import static oap.util.Pair.__;

@Slf4j
public abstract class Configuration {
    public static Integer force_version = null;
    protected final Map<Integer, DictionaryRoot> mappings;
    private int latestVersion = -1;

    public Configuration( Path mappingLocation, String defaultPath, String resourceLocation, int maxVersionsToLoad ) {

        List<URL> logConfigs = Collections.emptyList();
        if( mappingLocation != null ) {
            log.info( "mappingLocation = {}", mappingLocation );
            logConfigs = Stream.of( Files.fastWildcard( mappingLocation, "*.json" ).stream() )
                .concat( Files.fastWildcard( mappingLocation, "*.conf" ).stream() )
                .map( Try.map( p -> p.toUri().toURL() ) )
                .collect( toList() );
        }

        if( logConfigs.isEmpty() && defaultPath != null ) {
            log.info( "defaultLocation = {}", defaultPath );
            logConfigs = Stream.of( Files.fastWildcard( defaultPath, "*.json" ).stream() )
                .concat( Files.fastWildcard( defaultPath, "*.conf" ).stream() )
                .map( Try.map( p -> p.toUri().toURL() ) )
                .collect( toList() );
        }

        if( logConfigs.isEmpty() && resourceLocation != null ) {
            log.info( "resourceLocation = {}", resourceLocation );

            logConfigs.addAll( Resources.urls( resourceLocation, "json" ) );
        }

        Preconditions.checkState( !logConfigs.isEmpty(), "couldn't load configs from default path " + defaultPath +
            " or resource location " + resourceLocation );

        List<Pair<Integer, URL>> versionedDics = logConfigs
            .stream()
            .sorted( Comparator.comparing( URL::toString ) )
            .map( path -> __( versionOf( path.getPath() ), path ) )
            .collect( Collectors.toList() );

        long minimumVersion = versionedDics.stream().mapToLong( p -> p._1 ).max().orElse( maxVersionsToLoad ) - maxVersionsToLoad;

        mappings = versionedDics.stream().filter( lc -> lc._1 > minimumVersion )
            .map( this::parseDictionary )
            .collect( toMap() );

        //mappings.values().stream().forEach(this::validateSupportedTypes);

        log.debug( "loaded versions: {}", mappings.keySet() );
    }

    public static int versionOf( String path ) {
        return Integer.parseInt( FilenameUtils.getBaseName( path ).split( "\\." )[1].substring( 1 ) );
    }

    public void resetLatestVersion() {
        latestVersion = -1;
    }

    private Pair<Integer, DictionaryRoot> parseDictionary( Pair<Integer, URL> versionAndPath ) {
        log.debug( "loading {}", versionAndPath );
        final DictionaryRoot dictionaryRoot = DictionaryParser.parse( versionAndPath._2 );
        if( !Objects.equals( dictionaryRoot.name, FilenameUtils.getBaseName( versionAndPath._2.getPath() ) ) ) {
            throw new IllegalArgumentException( versionAndPath._2 + " name is wrong" );
        }
        Integer version = dictionaryRoot.getProperty( "version" ).map( v -> ( ( Number ) v ).intValue() )
            .orElseThrow( () -> new IllegalArgumentException( versionAndPath._2 + " is not versioned " ) );

        if( !Objects.equals( version, versionAndPath._1 ) ) {
            throw new IllegalArgumentException( versionAndPath._2 + " version is wrong" );
        }
        return __( version, dictionaryRoot );
    }

    /**
     * private final List<String> supportedTypes = Arrays.asList("STRING", "STRING_ARRAY", "INTEGER", "ENUM");
     * private void validateSupportedTypes(DictionaryRoot dictionaryRoot) {
     * Stream.of(dictionaryRoot.getValues())
     * .flatMap(Optional d -> d.getProperty("type"))
     * .filter(type -> !supportedTypes.contains(type))
     * .collect(Collectors.toList()) ;
     * }
     */

    public Dictionary getLatestDictionary() {
        return getDictionary( getLatestVersion() );
    }

    public Dictionary getDictionary( int version ) {
        final DictionaryRoot d = mappings.get( version );
        if( d == null ) throw new RuntimeException( "No log-config found!!!" );
        return d;
    }

    public int getLatestVersion() {
        if( force_version != null ) return force_version;

        if( latestVersion != -1 ) return latestVersion;

        synchronized( this ) {
            if( latestVersion != -1 ) return latestVersion;

            latestVersion = mappings
                .entrySet()
                .stream()
                .mapToInt( Map.Entry::getKey )
                .max()
                .orElseThrow( () -> new RuntimeException( "No log-config found!!!" ) );
        }

        return latestVersion;
    }

    public Optional<Object> getDefaultValue( int version, String table, int externalId ) {
        return Optional.ofNullable( mappings.get( version ) )
            .map( m -> m.getValue( table ).getValue( externalId ) )
            .flatMap( p -> p.getProperty( "default" ) );
    }

    public Optional<Object> getDefaultValue( int version, String table, String id ) {
        return Optional.ofNullable( mappings.get( version ) )
            .map( m -> m.getValue( table ).getValue( id ) )
            .flatMap( p -> p.getProperty( "default" ) );
    }

    public Optional<Integer> transformEid( String model, String id, int toVersion ) {
        return Optional.ofNullable( mappings.get( toVersion ) )
            .map( m -> m.getValue( model ).getValue( id ) )
            .map( Dictionary::getExternalId );
    }

}
