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

import oap.dictionary.DictionaryParser;
import oap.dictionary.DictionaryRoot;
import oap.dictionary.ExternalIdType;
import oap.io.Files;
import oap.io.IoStreams;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.split;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class DictionaryMojo extends AbstractMojo {

   @Parameter( defaultValue = "${project.build.directory}/generated-sources/dictionary" )
   public String outputDirectory;

   @Parameter( defaultValue = "${project.basedir}/src/main/resources/dictionary" )
   public String sourceDirectory;

   @Parameter( defaultValue = "dictionary" )
   public String dictionaryPackage;

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {

      final ArrayList<Path> paths = Files.fastWildcard( Paths.get( sourceDirectory ), "*.json" );

      getLog().debug( "found " + paths );

      paths.forEach( path -> {
         getLog().info( "dictionary " + path + "..." );

         final DictionaryRoot dictionary = DictionaryParser.parse( path );

         final StringBuilder out = new StringBuilder();
         final String enumClass = toEnumName( dictionary.name );
         out
            .append( "package " + dictionaryPackage + ";\n\n" )
            .append( "import oap.dictionary.Dictionary;\n\n" )
            .append( "import java.util.Map;\n" )
            .append( "import java.util.Optional;\n" )
            .append( "import java.util.List;\n\n" )
            .append( "import static java.util.Collections.emptyList;\n" )
            .append( "import static java.util.Collections.emptyMap;\n" )
            .append( "import static java.util.Arrays.asList;\n\n" )
            .append( "public enum " + enumClass + " implements Dictionary {\n" );

         final Set<String> properties = dictionary
            .getValues()
            .stream()
            .flatMap( v -> v.getProperties().keySet().stream() )
            .collect( toSet() );

         final Map<String, Boolean> optional = properties
            .stream()
            .collect( toMap(
               k -> k,
               k -> dictionary.getValues().stream().filter( v -> !v.containsProperty( k ) ).findAny().isPresent()
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
            dictionary
               .getValues()
               .stream()
               .map( d ->
                  "  " + d.getId() + "(" + convert( d.getExternalId(), dictionary.externalIdAs ) + ", "
                     + d.isEnabled() + properties( d.getProperties(), properties, optional, types ) + ")"
               )
               .collect( joining( ",\n" ) )
         );

         final String externalIdType = dictionary.externalIdAs.javaType.getSimpleName();
         out
            .append( ";\n\n" )
            .append( "  private final " + externalIdType + " externalId;\n" )
            .append( "  private final boolean enabled;\n\n" );

         for( String property : properties ) {
            out
               .append( "  private final " + propertyType( property, optional, types ) + " " + property + ";\n" );
         }

         out.append( "\n" );

         for( String property : properties ) {
            out
               .append( "  public final " + propertyType( property, optional, types ) + " " + property + "(){return " + property + ";}\n" );
         }

         out
            .append( "\n  " + enumClass + "( " + externalIdType + " externalId, boolean enabled" );

         final String cParameters = properties.stream().map( p -> propertyType( p, optional, types ) + " " + p ).collect( joining( ", " ) );

         out.append( cParameters.length() > 0 ? ", " : "" )
            .append( cParameters + " ) {\n" )
            .append( "    this.externalId = externalId;\n" )
            .append( "    this.enabled = enabled;\n" );

         for( String property : properties ) {
            out.append( "    this." + property + " = " + property + ";\n" );
         }

         out
            .append( "  }\n" )
            .append( "\n" +
               "  @Override\n" +
               "  public int getOrDefault( String id, int defaultValue ) {\n" +
               "    return defaultValue;\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public Integer get( String id ) {\n" +
               "    return null;\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public String getOrDefault( int externlId, String defaultValue ) {\n" +
               "    return defaultValue;\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public boolean containsValueWithId( String id ) {\n" +
               "    return false;\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public List<String> ids() {\n" +
               "    return emptyList();\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public int[] externalIds() {\n" +
               "    return new int[0];\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public Map<String, Object> getProperties() {\n" +
               "    return emptyMap();\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public Optional<? extends Dictionary> getValueOpt( String name ) {\n" +
               "    return Optional.empty();\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public Dictionary getValue( String name ) {\n" +
               "    return null;\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public Dictionary getValue( int externalId ) {\n" +
               "    return null;\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public List<? extends Dictionary> getValues() {\n" +
               "    return emptyList();\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public String getId() {\n" +
               "    return name();\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public Optional<Object> getProperty( String name ) {\n" +
               "    return Optional.empty();\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public boolean isEnabled() {\n" +
               "    return enabled;\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public int getExternalId() {\n" +
               "    return externalId;\n" +
               "  }\n" +
               "\n" +
               "  @Override\n" +
               "  public boolean containsProperty( String name ) {\n" +
               "    return false;\n" +
               "  }\n" )
            .append( "}\n" );


         final Path outPath = Paths.get( outputDirectory, dictionaryPackage.replace( ".", "/" ), enumClass + ".java" );

         if( !java.nio.file.Files.exists( outPath ) || !Files.readString( outPath ).equals( out.toString() ) ) {
            Files.writeString( outPath, IoStreams.Encoding.PLAIN, out.toString() );
         } else {
            getLog().debug( outPath + " is not modified." );
         }

      } );
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

   private String print( Object value ) {
      if( String.class.isAssignableFrom( value.getClass() ) ) return "\"" + value + "\"";
      if( Long.class.isAssignableFrom( value.getClass() ) ) return value + "L";
      if( List.class.isAssignableFrom( value.getClass() ) ) {
         return "asList(" + ( ( List ) value ).stream().map( v -> print( v ) ).collect( joining( ", " ) ) + ")";
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

   private String toEnumName( String name ) {
      return String.join( "", asList( split( name, '-' ) ).stream().map( StringUtils::capitalize ).collect( toList() ) );
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
}
