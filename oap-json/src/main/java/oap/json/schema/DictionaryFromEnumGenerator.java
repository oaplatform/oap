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

package oap.json.schema;

import oap.json.Binder;
import oap.json.schema._dictionary.Dictionary;
import oap.util.Strings;

import java.nio.file.Paths;
import java.util.LinkedHashSet;

/**
 * Created by Igor Petrenko on 13.04.2016.
 */
public class DictionaryFromEnumGenerator {
   public static void main( String[] args ) throws ClassNotFoundException {
      final String clazz = args[0];
      final String name = args[1];
      final String path = args[2];

      System.out.println( "Class = " + clazz );
      System.out.println( "name = " + name );
      System.out.println( "path = " + path );

      final String[] classes = clazz.split( "," );
      final String[] names = name.split( "," );

      assert ( classes.length == names.length );

      for( int x = 0; x < classes.length; x++ ) {
         final String c = classes[x];
         final String n = names[x];
         final Object[] enumConstants = Class.forName( c ).getEnumConstants();

         final LinkedHashSet<String> values = new LinkedHashSet<>( enumConstants.length );

         for( int i = 0; i < enumConstants.length; i++ ) {
            final String e = enumConstants[i].toString();
            if( Strings.UNKNOWN.equals( e ) ) continue;

            values.add( e );
         }

         Binder.json.marshal( Paths.get( path, n + ".json" ), new Dictionary( values ) );
      }
   }
}
