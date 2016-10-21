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

package oap.tsv.genrator;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

import static oap.reflect.Types.isInstance;
import static oap.reflect.Types.isPrimitive;

/**
 * Created by igor.petrenko on 01.09.2016.
 */
public interface CsvGeneratorStrategy<TLine extends CsvGenerator.Line> {
   CsvGeneratorStrategy<CsvGenerator.Line> DEFAULT = new CsvGeneratorStrategy<CsvGenerator.Line>() {};

   default void map( StringBuilder c, Type cc, TLine line, String field, char delimiter, Optional<Join> join ) {
      if( isInstance( Boolean.class, cc ) || isInstance( boolean.class, cc ) ) {
         mapBoolean( c, cc, line, field );
      } else if( isPrimitive( cc ) ) {
         mapPrimitive( c, cc, line, field );
      } else if( isInstance( Enum.class, cc ) )
         mapEnum( c, cc, line, field );
      else if( isInstance( Collection.class, cc ) ) {
         mapCollection( c, cc, line, field );
      } else if( !cc.equals( String.class ) ) {
         c.append( "sb.append( " );
         escape( c, () -> c.append( " String.valueOf( " ).append( field ).append( " )" ) );
         c.append( " );" );
      } else {
         mapString( c, cc, line, field );
      }
   }

   default void mapPrimitive( StringBuilder c, Type cc, TLine line, String field ) {
      c.append( "sb.append( " ).append( field ).append( " );" );
   }

   default void mapString( StringBuilder c, Type cc, TLine line, String field ) {
      c.append( "sb.append( " );
      escape( c, () -> c.append( field ) );
      c.append( " );" );
   }

   default void mapCollection( StringBuilder c, Type cc, TLine line, String field ) {
      c.append( "{sb.append( '[' ).append( " );
      escape( c, () -> c.append( " Strings.join( " ).append( field ).append( " )" ) );
      c.append( ").append( ']' );}" );
   }

   default void mapEnum( StringBuilder c, Type cc, TLine line, String field ) {
      c.append( "sb.append( " ).append( field ).append( " );" );
   }

   default boolean ignoreDefaultValue() {
      return false;
   }

   default void escape( StringBuilder c, Runnable run ) {
      c.append( "CharMatcher.JAVA_ISO_CONTROL.removeFrom( " );
      run.run();
      c.append( " )" );
   }

   default StringBuilder mapBoolean( StringBuilder c, Type cc, TLine line, String field ) {
      c.append( "sb.append( " ).append( field ).append( " );" );
      return c;
   }

   default boolean printDelimiter() {
      return true;
   }

   default void beforeLine( TLine line ) {
   }

   default void afterLine( TLine line ) {
   }
}
