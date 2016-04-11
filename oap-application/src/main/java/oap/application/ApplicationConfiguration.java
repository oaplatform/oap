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

import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.json.Binder;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Stream;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ApplicationConfiguration {
   List<String> profiles = Lists.empty();
   Map<String, Map<String, Object>> services = Maps.empty();

   public static ApplicationConfiguration load( Path appConfigPath ) {
      return load( appConfigPath, new String[0] );
   }

   public static ApplicationConfiguration load( Path appConfigPath, String[] configs ) {
      log.debug( "application configurations: {}", appConfigPath );
      return Binder.hoconWithConfig( configs )
         .unmarshal( ApplicationConfiguration.class, appConfigPath );
   }

   public static ApplicationConfiguration load( Path appConfigPath, Path confd ) {
      ArrayList<Path> paths = Files.wildcard( confd, "*.conf" );
      log.info( "global configurations: {}", paths );
      return load( appConfigPath, Stream.of( paths )
         .map( Files::readString )
         .toArray( String[]::new ) );
   }
}
