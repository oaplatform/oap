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

package oap.dictionary.maven;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import oap.dictionary.Dictionary;
import oap.dictionary.DictionaryParser;
import oap.dictionary.DictionaryRoot;
import oap.dictionary.ExternalIdType;
import oap.io.Files;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.util.Collections;
import oap.util.Lists;
import oap.util.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static oap.util.Pair.__;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.apache.commons.lang3.StringUtils.split;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class DictionaryMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project.build.directory}/generated-sources/dictionary" )
    public String outputDirectory;

    @Parameter( defaultValue = "${project.basedir}/src/main/resources/dictionary" )
    public String sourceDirectory;

    @Parameter( defaultValue = "dictionary" )
    public String dictionaryPackage;

    @Parameter
    public String[] exclude = new String[0];

    @Override
    public void execute() {
        var paths =
            Lists.filter( Files.fastWildcard( Paths.get( sourceDirectory ), "*.json" ), p -> {
                var b = Arrays
                    .stream( exclude )
                    .noneMatch( e -> FilenameUtils.wildcardMatchOnSystem( separatorsToUnix( p.toString() ), e ) );
                if( !b ) getLog().debug( "exclude " + p );
                return b;
            } );

        getLog().debug( "found " + paths );

        for( var path : paths ) {
            getLog().info( "dictionary " + path + "..." );

            var dictionary = DictionaryParser.parse( path );

            var gc = dictionary.getProperty( "$generator" )
                .map( p -> Binder.json.<Generator>unmarshal( Generator.class, Binder.json.marshal( p ) ) )
                .orElse( new Generator() );

            for( var dict : gc.getDictionaries( dictionary ) ) {

                var out = new StringBuilder();
                out
                    .append( "package " + dictionaryPackage + ";\n\n" )
                    .append( "import oap.dictionary.Dictionary;\n\n" )
                    .append( "import java.util.Map;\n" )
                    .append( "import java.util.Optional;\n" )
                    .append( "import java.util.List;\n\n" )
                    .append( "import static java.util.Collections.emptyList;\n" )
                    .append( "import static java.util.Collections.emptyMap;\n\n" )
                    .append( "public enum " + dict.name + " implements Dictionary {\n" );

                var properties = dict.values
                    .stream()
                    .flatMap( v -> v.getProperties().keySet().stream() )
                    .collect( toSet() );

                var optional = properties
                    .stream()
                    .collect( toMap(
                        k -> k,
                        k -> dictionary.getValues().stream().anyMatch( v -> !v.containsProperty( k ) )
                    ) );

                final Map<String, Class<?>> types = properties
                    .stream()
                    .collect( toMap(
                        k -> k,
                        k -> dictionary
                            .getValues()
                            .stream()
                            .filter( v -> v.containsProperty( k ) )
                            .findAny()
                            .get().getProperty( k ).get().getClass()
                    ) );

                out.append(
                    dict.values
                        .stream()
                        .map( d ->
                            "  " + d.getId() + "(" + convert( d.getExternalId(), dict.externalIdAs ) + ", "
                                + d.isEnabled() + properties( d.getProperties(), properties, optional, types ) + ")"
                        )
                        .collect( joining( ",\n" ) )
                );

                var externalIdType = dict.externalIdAs.javaType.getSimpleName();
                out
                    .append( ";\n\n" )
                    .append( "  private final " + externalIdType + " externalId;\n" )
                    .append( "  private final boolean enabled;\n\n" );

                for( var property : properties ) {
                    out
                        .append( "  private final " + propertyType( property, optional, types ) + " " + property + ";\n" );
                }

                out.append( "\n" );

                for( var property : properties ) {
                    out
                        .append( "  public final " + propertyType( property, optional, types ) + " " + property + "() { return " + property + "; }\n" );
                }

                out
                    .append( "\n  " + dict.name + "( " + externalIdType + " externalId, boolean enabled" );

                var cParameters = properties.stream().map( p -> propertyType( p, optional, types ) + " " + p ).collect( joining( ", " ) );

                out.append( cParameters.length() > 0 ? ", " : "" )
                    .append( cParameters + " ) {\n" )
                    .append( "    this.externalId = externalId;\n" )
                    .append( "    this.enabled = enabled;\n" );

                for( var property : properties ) {
                    out.append( "    this." + property + " = " + property + ";\n" );
                }

                out
                    .append( "  }\n\n"
                        + "  public static " + dict.name + " valueOf( int externalId ) {\n"
                        + "    switch( externalId ) {\n" );

                dict.values.forEach( d -> {
                    out.append( "      case " ).append( d.getExternalId() ).append( ": return " ).append( d.getId() ).append( ";\n" );
                } );

                out.append( "      default: " );

                if( dict.containsValueWithId( "UNKNOWN" ) ) {
                    out.append( "return UNKNOWN" );
                } else {
                    out.append( "throw new java.lang.IllegalArgumentException( \"Unknown id \" + externalId )" );
                }

                out.append(
                    ";\n"
                        + "    }\n"
                        + "  }\n"
                        + "\n"
                        + """
                              @Override
                                public int getOrDefault( String id, int defaultValue ) {
                                  return defaultValue;
                                }

                                @Override
                                public Integer get( String id ) {
                                  return null;
                                }

                                @Override
                                public String getOrDefault( int externlId, String defaultValue ) {
                                  return defaultValue;
                                }

                                @Override
                                public boolean containsValueWithId( String id ) {
                                  return false;
                                }

                                @Override
                                public List<String> ids() {
                                  return emptyList();
                                }

                                @Override
                                public int[] externalIds() {
                                  return new int[0];
                                }

                                @Override
                                public Map<String, Object> getProperties() {
                                  return emptyMap();
                                }

                                @Override
                                public Optional<? extends Dictionary> getValueOpt( String name ) {
                                  return Optional.empty();
                                }

                                @Override
                                public Dictionary getValue( String name ) {
                                  return null;
                                }

                                @Override
                                public Dictionary getValue( int externalId ) {
                                  return null;
                                }

                                @Override
                                public List<? extends Dictionary> getValues() {
                                  return emptyList();
                                }

                                @Override
                                public String getId() {
                                  return name();
                                }

                                @Override
                                public Optional<Object> getProperty( String name ) {
                                  return Optional.empty();
                                }

                                @Override
                                public boolean isEnabled() {
                                  return enabled;
                                }

                                @Override
                                public int getExternalId() {
                                  return externalId;
                                }

                                @Override
                                public boolean containsProperty( String name ) {
                                  return false;
                                }

                            """
                        + "  @Override\n"
                        + "  public " + dict.name + " cloneDictionary() {\n"
                        + "    return this;\n"
                        + "  }\n\n"
                        + "  public " + externalIdType + " externalId() {\n"
                        + "    return externalId;\n"
                        + "  }\n\n" )
                    .append( "}\n" );


                var outPath = Paths.get( outputDirectory, dictionaryPackage.replace( ".", "/" ), dict.name + ".java" );

                if( !java.nio.file.Files.exists( outPath ) || !Files.readString( outPath ).equals( out.toString() ) ) {
                    Files.writeString( outPath, IoStreams.Encoding.PLAIN, out.toString() );
                } else {
                    getLog().debug( outPath + " is not modified." );
                }
            }
        }
    }

    private String properties( Map<String, Object> properties, Set<String> names, Map<String, Boolean> optional, Map<String, Class<?>> types ) {
        final String res = names.stream().map( n -> {
            final Object value = properties.get( n );
            final Boolean opt = optional.get( n );
            if( opt ) {
                return value == null ? "Optional.empty()" : "Optional.of(" + print( value ) + ")";
            }

            return print( value );
        } ).collect( joining( ", " ) );
        return res.length() > 0 ? ", " + res : res;
    }

    @SuppressWarnings( "unchecked" )
    private String print( Object value ) {
        if( value == null ) return "null";
        if( String.class.isAssignableFrom( value.getClass() ) ) return "\"" + value + "\"";
        if( Long.class.isAssignableFrom( value.getClass() ) ) return value + "L";
        if( List.class.isAssignableFrom( value.getClass() ) ) {
            return "asList(" + ( ( List ) value ).stream().map( this::print ).collect( joining( ", " ) ) + ")";
        } else return value.toString();
    }

    private String propertyType( String property, Map<String, Boolean> optional, Map<String, Class<?>> types ) {
        final Boolean opt = optional.get( property );
        Class<?> clazz = types.get( property );
        if( clazz.equals( ArrayList.class ) ) clazz = List.class;

        if( opt ) return "Optional<" + clazz.getSimpleName() + ">";
        else {
            final Class<?> aClass = ClassUtils.wrapperToPrimitive( clazz );
            if( aClass == null ) return clazz.getSimpleName();
            return aClass.getSimpleName();
        }
    }

    private Object convert( int value, ExternalIdType externalIdType ) {
        switch( externalIdType ) {
            case character:
                return "'" + ( char ) value + "'";
            case integer:
                return value;
            default:
                throw new IllegalArgumentException( "Unknown ExternalIdType " + externalIdType );
        }
    }

    public static class Generator {
        @JsonProperty( "$children" )
        public final ArrayList<Child> children = new ArrayList<>();
        @JsonProperty( "$externalIdAs" )
        public ExternalIdType externalIdAs = ExternalIdType.integer;
        @JsonProperty( "$name" )
        private String name;

        public Generator() {
        }

        private static String toEnumName( String name ) {
            return Stream.of( split( name, '-' ) )
                .map( StringUtils::capitalize )
                .collect( joining() );
        }

        private static java.util.stream.Stream<? extends Dictionary> getValues( java.util.stream.Stream<? extends Dictionary> values, int level ) {
            if( level == 0 ) return values;

            return getValues( values.flatMap( v -> v.getValues().stream() ), level - 1 );
        }

        private static List<? extends Dictionary> getValues( List<? extends Dictionary> values, int level ) {
            if( level == 0 ) return values;

            return Stream.of( getValues( values.stream().flatMap( v -> v.getValues().stream() ), level - 1 ) )
                .distinctByProperty( d -> __( d.getId(), d.getExternalId() ) )
                .collect( toList() );

        }

        public String getName( String name ) {
            return toEnumName( this.name != null ? this.name : name );
        }

        public List<DictionaryConf> getDictionaries( DictionaryRoot dictionary ) {
            var ret = new ArrayList<DictionaryConf>();
            ret.add( new DictionaryConf( toEnumName( this.name != null ? this.name : dictionary.name ), 0,
                externalIdAs,
                getValues( dictionary.getValues(), 0 ) ) );
            for( var child : children ) {
                ret.add( new DictionaryConf( toEnumName( child.name ), child.level,
                    child.externalIdAs,
                    getValues( dictionary.getValues(), child.level ) ) );
            }

            return ret;
        }

        public static class Child {
            @JsonProperty( "$level" )
            public final int level;
            @JsonProperty( "$name" )
            public final String name;

            @JsonProperty( "$externalIdAs" )
            public final ExternalIdType externalIdAs;

            @JsonCreator
            public Child( int level, String name, ExternalIdType externalIdAs ) {
                this.level = level;
                this.name = name;
                this.externalIdAs = externalIdAs != null ? externalIdAs : ExternalIdType.integer;
            }
        }

        public static class DictionaryConf {
            public final String name;
            public final int level;
            public final ExternalIdType externalIdAs;
            public final List<? extends Dictionary> values;

            public DictionaryConf( String name, int level, ExternalIdType externalIdAs, List<? extends Dictionary> values ) {
                this.name = name;
                this.level = level;
                this.externalIdAs = externalIdAs;
                this.values = values;
            }

            public boolean containsValueWithId( String id ) {
                return Collections.find2( values, v -> v.getId().equals( id ) ) != null;
            }
        }
    }
}
