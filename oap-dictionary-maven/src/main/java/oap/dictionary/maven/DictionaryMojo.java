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
import oap.io.Files;
import oap.io.IoStreams;
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
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * Created by Admin on 19.05.2016.
 */
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
            .append( "public enum " + enumClass + " {\n" );

//         final Set<String> properties = dictionary.getValues().stream().flatMap( v -> v.getProperties().keySet().stream() ).collect( toSet() );

         out.append(
            dictionary
               .getValues()
               .stream()
               .map( d -> "  " + d.getId() + "(" + d.getExternalId() + ", " + d.isEnabled() + ")" )
               .collect( joining( ",\n" ) )
         );

         out
            .append( ";\n\n" )
            .append( "  private final int externalId;\n" )
            .append( "  private final boolean enabled;\n\n" )
            .append( "  public final boolean enabled() {return enabled;}\n" )
            .append( "  public final int externalId() {return externalId;}\n\n" )
            .append( "  " + enumClass + "( int externalId, boolean enabled ) {\n" )
            .append( "    this.externalId = externalId;\n" )
            .append( "    this.enabled = enabled;\n" )
            .append( "  }\n" )
            .append( "}\n" );


         final Path outPath = Paths.get( outputDirectory, dictionaryPackage.replace( ".", "/" ), enumClass + ".java" );

         if( !java.nio.file.Files.exists( outPath ) || !Files.readString( outPath ).equals( out.toString() ) ) {
            Files.writeString( outPath, IoStreams.Encoding.PLAIN, out.toString() );
         } else {
            getLog().debug( outPath + " is not modified." );
         }

      } );
   }

   private String toEnumName( String name ) {
      return String.join( "", asList( split( name, '-' ) ).stream().map( StringUtils::capitalize ).collect( toList() ) );
   }
}
