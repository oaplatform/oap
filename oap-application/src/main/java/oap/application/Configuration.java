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

import oap.io.Resources;
import oap.json.Binder;
import oap.util.Stream;
import oap.util.Strings;
import org.apache.commons.collections4.ListUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Configuration<T> {
   private Class<T> clazz;
   private String name;

   public Configuration( Class<T> clazz, String name ) {
      this.clazz = clazz;
      this.name = name;
   }

   public List<T> fromClassPath() {
      return Stream.of( urlsFromClassPath() )
         .map( this::fromUrl )
         .toList();
   }

   public List<URL> urlsFromClassPath() {
      return Stream.of( ListUtils.union(
         Resources.urls( "META-INF/" + name + ".json" ),
         Resources.urls( "META-INF/" + name + ".conf" ) ) )
         .toList();
   }

   public T fromUrl( URL url ) {
      return fromHocon( Strings.readString( url ) );
   }

   public T fromHocon( String hocon ) {
      Objects.nonNull( hocon );
      return Binder.hocon.unmarshal( clazz, hocon );
   }

   public T fromResource( Class<?> contextClass, String name ) {
      return Resources.url( contextClass, name )
         .map( this::fromUrl )
         .orElseThrow( () -> {
            String path = Optional.ofNullable( contextClass.getPackage() )
               .map( Package::getName )
               .orElse( "" )
               .replace( ".", "/" ) + "/" + name;
            return new UncheckedIOException( new IOException( "not found " + path ) );
         } );
   }

}
