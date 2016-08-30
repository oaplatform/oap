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

package oap.tools;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * Created by igor.petrenko on 30.08.2016.
 */
public class MemoryClassLoader extends ClassLoader {
   private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
   private final MemoryFileManager manager = new MemoryFileManager( this.compiler );

   public MemoryClassLoader( String classname, String filecontent ) {
      this( singletonMap( classname, filecontent ) );
   }

   private MemoryClassLoader( Map<String, String> map ) {
      List<Source> list = new ArrayList<>();
      for( Map.Entry<String, String> entry : map.entrySet() ) {
         list.add( new Source( entry.getKey(), JavaFileObject.Kind.SOURCE, entry.getValue() ) );
      }
      this.compiler.getTask( null, this.manager, null, null, null, list ).call();
   }

   @Override
   protected Class<?> findClass( String name ) throws ClassNotFoundException {
      synchronized( this.manager ) {
         Output mc = this.manager.map.remove( name );
         if( mc != null ) {
            byte[] array = mc.toByteArray();
            return defineClass( name, array, 0, array.length );
         }
      }
      return super.findClass( name );
   }

   private static class Source extends SimpleJavaFileObject {
      private final String content;

      Source( String name, Kind kind, String content ) {
         super( URI.create( "memo:///" + name.replace( '.', '/' ) + kind.extension ), kind );
         this.content = content;
      }

      @Override
      public CharSequence getCharContent( boolean ignore ) {
         return this.content;
      }
   }

   private static class Output extends SimpleJavaFileObject {
      private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

      Output( String name, Kind kind ) {
         super( URI.create( "memo:///" + name.replace( '.', '/' ) + kind.extension ), kind );
      }

      byte[] toByteArray() {
         return this.baos.toByteArray();
      }

      @Override
      public ByteArrayOutputStream openOutputStream() {
         return this.baos;
      }
   }

   private static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
      private final Map<String, Output> map = new HashMap<>();

      MemoryFileManager( JavaCompiler compiler ) {
         super( compiler.getStandardFileManager( null, null, null ) );
      }

      @Override
      public JavaFileObject getJavaFileForOutput
         ( Location location, String name, JavaFileObject.Kind kind, FileObject source ) {
         Output mc = new Output( name, kind );
         this.map.put( name, mc );
         return mc;
      }
   }
}
