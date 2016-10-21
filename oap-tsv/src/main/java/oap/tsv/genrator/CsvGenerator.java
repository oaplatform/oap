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

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import oap.tools.MemoryClassLoader;
import oap.util.Pair;
import oap.util.Try;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static oap.reflect.Types.getOptionalArgumentType;
import static oap.reflect.Types.isPrimitive;
import static oap.reflect.Types.toJavaType;
import static oap.util.Pair.__;

/**
 * Created by igor.petrenko on 01.09.2016.
 */
@Slf4j
public class CsvGenerator<T, TLine extends CsvGenerator.Line> {
   public static final String UNIQUE_ID = "{unique_id}";
   private static final String math = "/*+-%";
   private static final AtomicInteger counter = new AtomicInteger();
   private static final HashMap<String, Function<?, String>> cache = new HashMap<>();
   private static final CsvGenerator<Object, Line> EMPTY = new CsvGenerator<>(
      Object.class, emptyList(), ' ', CsvGeneratorStrategy.DEFAULT );
   private final Function<T, String> func;
   private CsvGeneratorStrategy<TLine> map;

   public CsvGenerator( Class<T> clazz, List<TLine> pathAndDefault, char delimiter, CsvGeneratorStrategy<TLine> map ) {
      this.map = map;
      final StringBuilder c = new StringBuilder();

      try {
         final AtomicInteger num = new AtomicInteger();
         final FieldStack fields = new FieldStack();

         final String className = clazz.getName().replace( '$', '.' );

         c.append( "package " ).append( getClass().getPackage().getName() ).append( ";\n" +
            "\n" +
            "import oap.util.Strings;\n" +
            "\n" +
            "import java.util.function.Function;\n" +
            "import com.google.common.base.CharMatcher;\n" +
            "\n" +
            "public final class " ).append( getClass().getSimpleName() ).append( UNIQUE_ID ).append( " implements Function<" ).append( className ).append( ", String> {\n" +
            "   @Override\n" +
            "   public String apply( " ).append( className ).append( " s ) {\n" +
            "     final StringBuilder sb = new StringBuilder();\n" +
            "\n" );

         final int size = pathAndDefault.size();
         for( int x = 0; x < size; x++ ) {
            addPath( clazz, pathAndDefault.get( x ), delimiter, c, num, fields, x + 1 >= size );
         }


         c.append( "\n" +
            "     return sb.toString();\n" +
            "   }\n" +
            "}" );

         final AtomicInteger line = new AtomicInteger( 0 );
         log.trace( "\n{}", new BufferedReader( new StringReader( c.toString() ) )
            .lines()
            .map( l -> String.format( "%3d", line.incrementAndGet() ) + " " + l )
            .collect( joining( "\n" ) )
         );

         synchronized( cache ) {
            final Function<?, String> s = cache.get( c.toString() );
            if( s != null ) func = ( Function<T, String> ) s;
            else {
               int i = counter.incrementAndGet();
               MemoryClassLoader mcl = new MemoryClassLoader( getClass().getName() + i, c.toString().replace( UNIQUE_ID, String.valueOf( i ) ) );
               func = ( Function<T, String> ) mcl.loadClass( getClass().getName() + i ).newInstance();
               cache.put( c.toString(), func );
            }
         }

      } catch( Exception e ) {
         log.error( c.toString() );
         throw Throwables.propagate( e );
      }
   }

   @SuppressWarnings( "unchecked" )
   public static <T> CsvGenerator<T, Line> empty() {
      return ( CsvGenerator<T, Line> ) EMPTY;
   }

   private void addPath( Class<T> clazz, TLine line, char delimiter, StringBuilder c,
                         AtomicInteger num, FieldStack fields,
                         boolean last ) throws NoSuchMethodException, NoSuchFieldException {
      final AtomicInteger tab = new AtomicInteger( 5 );

      c.append( "\n" );
      tab( c, tab ).append( "// " ).append( line.path ).append( "\n" );

      final String[] orPath = StringUtils.split( line.path, '|' );
      final int orIndex = 0;

      map.beforeLine( c, line, delimiter );
      addPathOr( clazz, delimiter, c, num, fields, last, tab, orPath, orIndex, line );
      map.afterLine( c, line, delimiter );
   }

   private void printDelimiter( char delimiter, StringBuilder c, boolean last, AtomicInteger tab ) {
      if( map.printDelimiter() && !last ) tab( c, tab ).append( "sb.append('" ).append( delimiter ).append( "');\n" );
   }

   private void addPathOr( Class<T> clazz, char delimiter, StringBuilder c, AtomicInteger num,
                           FieldStack fields, boolean last, AtomicInteger tab,
                           String[] orPath, int orIndex, TLine line ) {
      int sp = 0;
      String newPath = "s.";
      final MutableObject<Type> lc = new MutableObject<>( clazz );
      final AtomicInteger psp = new AtomicInteger( 0 );
      final AtomicInteger opts = new AtomicInteger( 0 );
      while( sp >= 0 ) {
         sp = orPath[orIndex].indexOf( '.', sp + 1 );

         if( sp > 0 ) {
            final Pair<Type, String> pnp = fields.computeIfAbsent( orPath[orIndex].substring( 0, sp ), Try.map( ( key ) -> {
               final String prefix = psp.get() > 1 ? key.substring( 0, psp.get() - 1 ) : "";
               final String suffix = key.substring( psp.get() );

               final boolean optional = isOptional( lc.getValue() );
               final Type declaredFieldType = getDeclaredFieldOrFunctionType( optional ? getOptionalArgumentType( lc.getValue() ) : lc.getValue(), suffix );

               final String classType = toJavaType( declaredFieldType );

               final String field = "field" + num.incrementAndGet();

               final Optional<Pair<Type, String>> rfP = Optional.ofNullable( fields.get( prefix ) );
               final String rf = rfP.map( p -> p._2 + ( optional ? ".get()" : "" ) + "." + suffix ).orElse( "s." + key );

               if( optional ) {
                  tab( c, tab ).append( "if( " ).append( rfP.map( p -> p._2 ).orElse( "s" ) ).append( ".isPresent() ) {\n" );
                  opts.incrementAndGet();
                  tabInc( tab );
                  fields.up();
               }

               tab( c, tab ).append( "final " ).append( classType ).append( " " ).append( field ).append( " = " ).append( rf ).append( ";\n" );

               lc.setValue( declaredFieldType );
               return __( lc.getValue(), field );
            } ) );
            newPath = pnp._2 + ".";
            lc.setValue( pnp._1 );
            psp.set( sp + 1 );
         } else {
            newPath += orPath[orIndex].substring( psp.get() );
         }
      }

      final int in = newPath.lastIndexOf( '.' );
      String pField = in > 0 ? newPath.substring( 0, in ) : newPath;
      String cField = newPath.substring( in + 1 );

      final boolean isJoin = cField.startsWith( "{" );
      final String[] cFields = isJoin ?
         StringUtils.split( cField.substring( 1, cField.length() - 1 ), ',' ) : new String[]{ cField };

      Type parentClass = lc.getValue();
      final boolean isOptionalParent = isOptional( parentClass ) && !cField.startsWith( "isPresent" );
      String optField = null;
      if( isOptionalParent ) {
         opts.incrementAndGet();
         tab( c, tab ).append( "if( " ).append( pField ).append( ".isPresent() ) {\n" );
         fields.up();
         tabInc( tab );

         parentClass = getOptionalArgumentType( parentClass );
         optField = "opt" + num.incrementAndGet();
         tab( c, tab ).append( "final " ).append( toJavaType( parentClass ) ).append( " " ).append( optField ).append( " = " ).append( pField ).append( ".get();\n" );
      }

      for( int i = 0; i < cFields.length; i++ ) {
         cField = cFields[i];

         if( cField.startsWith( "\"" ) )
            tab( c.append( ";\n" ), tab ).append( "sb.append( " ).append( cField ).append( " );\n" );
         else {
            if( isOptionalParent ) {
               newPath = in > 0 ? optField + "." + cField : cField;
            } else {
               newPath = in > 0 ? pField + "." + cField : "s." + cField;
            }

            final Type cc = in > 0 ? getDeclaredFieldOrFunctionType( parentClass, cField ) : parentClass;

            final Optional<Join> join = isJoin ? Optional.of( new Join( i, cFields.length ) ) : Optional.empty();

            add( c, num, newPath, cc, parentClass, true, tab, orPath, orIndex,
               clazz, delimiter, fields, last || ( i < cFields.length - 1 ), line, join );
         }
      }


      c.append( ";\n" );

      for( int i = 0; i < opts.get(); i++ ) {
         fields.down();
         tabDec( tab );
         tab( c, tab ).append( "} else {\n" );
         fields.up();
         tabInc( tab );

         if( orIndex + 1 < orPath.length ) {
            addPathOr( clazz, delimiter, c, num, fields, last, new AtomicInteger( tab.get() + 2 ), orPath, orIndex + 1, line );
         } else {
            printDefaultValue( c, line.defaultValue );
            if( !map.ignoreDefaultValue() ) printDelimiter( delimiter, c, last, tab );
         }
         tabDec( tab );
         fields.down();
         tab( c, tab ).append( "}\n" );
      }
   }

   private void tabInc( AtomicInteger tab ) {
      tab.getAndUpdate( v -> v + 2 );
   }

   private void tabDec( AtomicInteger tab ) {
      tab.getAndUpdate( v -> v - 2 );
   }

   private StringBuilder tab( StringBuilder sb, AtomicInteger tab ) {
      for( int i = 0; i < tab.get(); i++ ) sb.append( ' ' );

      return sb;
   }

   private Type getDeclaredFieldOrFunctionType( Type type, String field ) {
      if( type instanceof ParameterizedType )
         return getDeclaredFieldOrFunctionType( ( ( ParameterizedType ) type ).getRawType(), field );

      final int mi = StringUtils.indexOfAny( field, math );
      if( mi > 0 ) return getDeclaredFieldOrFunctionType( type, field.substring( 0, mi ) );

      final Class type1 = ( Class ) type;
      try {
         final int i = field.indexOf( '(' );
         if( i < 0 ) {
            if( type1.isAssignableFrom( Map.class ) ) {
               return Object.class;
            }
            return type1.getDeclaredField( field ).getGenericType();
         } else
            return type1.getDeclaredMethod( field.substring( 0, i ) ).getGenericReturnType();
      } catch( NoSuchMethodException | NoSuchFieldException e ) {
         if( !type1.getSuperclass().equals( Object.class ) )
            return getDeclaredFieldOrFunctionType( type1.getGenericSuperclass(), field );
         else throw Throwables.propagate( e );
      }
   }

   private void add( StringBuilder c, AtomicInteger num, String newPath, Type cc, Type parentType,
                     boolean nullable, AtomicInteger tab,
                     String[] orPath, int orIndex, Class<T> clazz, char delimiter,
                     FieldStack fields, boolean last, TLine line, Optional<Join> join ) {
      String pfield = newPath;
      final boolean primitive = isPrimitive( cc );
      if( !primitive ) {
         pfield = "field" + num.incrementAndGet();
         if( parentType.getTypeName().startsWith( "java.util.Map<" ) ) {

            tab( c, tab )
               .append( "final " )
               .append( toJavaType( cc ) ).append( " " ).append( pfield ).append( " = " )
               .append( newPath.replace( ".", ".get(\"" ) ).append( "\");\n" );
         } else {
            tab( c, tab ).append( "final " ).append( toJavaType( cc ) ).append( " " ).append( pfield ).append( " = " ).append( newPath ).append( ";\n" );
         }
      }
      if( !primitive ) {
         if( isOptional( cc ) ) {
            final Type cc1 = getOptionalArgumentType( cc );
            tab( c, tab ).append( "if( " ).append( pfield ).append( ".isPresent() ) {\n" );
            fields.up();
            add( c, num, pfield + ".get()", cc1, parentType, false, new AtomicInteger( tab.get() + 2 ),
               orPath, orIndex, clazz, delimiter, fields, last, line, join );
            fields.down();
            tab( c, tab ).append( "} else {\n" );

            if( orIndex + 1 < orPath.length ) {
               fields.up();
               addPathOr( clazz, delimiter, c, num, fields, last, new AtomicInteger( tab.get() + 2 ), orPath, orIndex + 1, line );
               fields.down();
            } else {
               printDefaultValue( tab( c, tab ), line.defaultValue );
               if( !map.ignoreDefaultValue() ) printDelimiter( delimiter, c, last, tab );
            }
            tab( c, tab ).append( "}\n" );
         } else {
            tab( c, tab );

            if( nullable ) c.append( "if( " ).append( pfield ).append( " != null ) { " );

            map.map( c, cc, line, pfield, delimiter, join );
            printDelimiter( delimiter, c, last, tab );

            if( nullable ) {
               c.append( "} else {" );
               printDefaultValue( c, line.defaultValue );
               if( !map.ignoreDefaultValue() ) printDelimiter( delimiter, c, last, tab );
               tab( c, tab ).append( "}\n" );
            }
         }
      } else {
         tab( c, tab );
         map.map( c, cc, line, pfield, delimiter, join );
         printDelimiter( delimiter, c, last, tab );
      }
   }

   private void printDefaultValue( StringBuilder c, Object pfield ) {
      if( !map.ignoreDefaultValue() ) {
         c.append( "sb.append( " );
         if( ClassUtils.isPrimitiveOrWrapper( pfield.getClass() ) ) c.append( pfield );
         else c.append( "\"" ).append( pfield ).append( "\"" );
         c.append( " );\n" );
      } else c.append( "{}\n" );
   }

   private boolean isOptional( Type type ) {
      return type instanceof ParameterizedType &&
         ( ( ParameterizedType ) type ).getRawType().equals( Optional.class );
   }

   public String process( T source ) {
      return func.apply( source );
   }

   public static class Line {
      public final String name;
      public final String path;
      public final Object defaultValue;

      public Line( String name, String path, Object defaultValue ) {
         this.name = name;
         this.path = path;
         this.defaultValue = defaultValue;
      }

      public static Line line( String name, String path, Object defaultValue ) {
         return new Line( name, path, defaultValue );
      }
   }

   private static class FieldStack {
      private final Stack<HashMap<String, Pair<Type, String>>> stack = new Stack<>();

      public FieldStack() {
         stack.push( new HashMap<>() );
      }

      public FieldStack up() {
         stack.push( new HashMap<>( stack.peek() ) );

         return this;
      }

      public FieldStack down() {
         stack.pop();

         return this;
      }

      public Pair<Type, String> computeIfAbsent( String key, Function<String, Pair<Type, String>> func ) {
         Pair<Type, String> v = stack.peek().get( key );
         if( v == null ) {
            v = func.apply( key );
            stack.peek().put( key, v );
         }
         return v;
      }

      public Pair<Type, String> get( String key ) {
         return stack.peek().get( key );
      }
   }
}
