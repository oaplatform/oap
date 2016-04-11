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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.json.Binder;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
public class ApplicationConfiguration {

   //   @todo get this logic tested and sorted out
   public static Map<String, Map<String, Object>> load( Path configPath, Path confd ) {
      log.info( "application configurations: {}", configPath );
      log.info( "global configuration directory: {}", confd );

      ArrayList<Path> paths = Files.wildcard( confd, "*.conf" );
      log.info( "global configurations = {}", paths );
      final String[] configs = Stream.of( paths )
         .map( Files::readString )
         .concat( Stream.of( Files.readString( configPath ) ) )
         .toArray( String[]::new );
      return toMap( configPath, configs );
   }

   @SuppressWarnings( "unchecked" )
   private static Map<String, Map<String, Object>> toMap( Path configPath, String[] configs ) {
      Map<String, Object> map = Binder.hoconWithConfig( configs ).unmarshal(
         new TypeReference<Map<String, Object>>() {
         }, configPath );

      return ( Map<String, Map<String, Object>> ) ( Object ) com.google.common.collect.Maps.filterValues( map, v -> v instanceof Map );
   }
}
