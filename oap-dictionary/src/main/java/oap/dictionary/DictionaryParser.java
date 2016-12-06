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

package oap.dictionary;

import com.fasterxml.jackson.core.JsonGenerator;
import lombok.val;
import oap.json.Binder;
import oap.util.Try;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Created by Igor Petrenko on 15.04.2016.
 */
public class DictionaryParser {
   public static final String NAME = "name";
   public static final String ID = "id";
   public static final String ENABLED = "enabled";
   public static final String EXTERNAL_ID = "eid";
   public static final String VALUES = "values";

   private static final Set<String> defaultFields = new HashSet<>();
   private static final BiFunction<Boolean, Object, Optional<Integer>> intFunc =
      ( convert, str ) -> {
         if( !convert ) return Optional.empty();
         else if( str instanceof Long ) return Optional.of( ( ( Long ) str ).intValue() );
         else if( str instanceof Double ) return Optional.of( ( ( Double ) str ).intValue() );
         else if( str instanceof String && ( ( String ) str ).length() == 1 )
            return Optional.of( ( int ) ( ( String ) str ).charAt( 0 ) );
         else return Optional.empty();
      };

   static {
      defaultFields.add( ID );
      defaultFields.add( ENABLED );
      defaultFields.add( EXTERNAL_ID );
   }

   @SuppressWarnings( "unchecked" )
   private static Dictionary parseAsDictionaryValue( Object value, String path, boolean valueAsRoot ) {
      if( value instanceof Map ) {
         final Map<Object, Object> valueMap = ( Map<Object, Object> ) value;
         List<Dictionary> values = emptyList();

         final HashMap<String, Object> properties = new HashMap<>();
         for( Map.Entry e : valueMap.entrySet() ) {
            final String propertyName = e.getKey().toString();
            if( !defaultFields.contains( propertyName ) && ( !valueAsRoot || !NAME.equals( propertyName ) ) ) {
               final Object propertyValue = e.getValue();

               if( VALUES.equals( propertyName ) )
                  values = parseValues( ( List ) propertyValue, path );
               else
                  properties.put( propertyName, propertyValue );
            }
         }


         final Map<String, Object> p = properties.isEmpty() ? emptyMap() : properties;

         if( valueAsRoot ) {
            final String name = getString( valueMap, NAME );

            final ExternalIdType externalIdAs = getStringOpt( valueMap, "externalIdAs" )
               .map( ExternalIdType::valueOf )
               .orElse( ExternalIdType.integer );

            return new DictionaryRoot( name, externalIdAs, values, p );
         }

         final String id = getString( valueMap, ID );
         final boolean enabled = getBooleanOpt( valueMap, ENABLED ).orElse( true );
         final int externalId = getInt( valueMap, EXTERNAL_ID, true );

         return values.isEmpty() ?
            new DictionaryLeaf( id, enabled, externalId, p ) :
            new DictionaryValue( id, enabled, externalId, values, p );
      } else {
         throw new DictionaryFormatError(
            "value " + path + " type " +
               ( value == null ? "<NULL>" : value.getClass().toString() ) + " != " + Map.class
         );
      }
   }

   public static DictionaryRoot parse( Path resource ) {
      final Map map = Binder.hoconWithoutSystemProperties.unmarshal( Map.class, resource );
      return parse( map );
   }

   public static DictionaryRoot parse( URL resource ) {
      final Map map = Binder.hoconWithoutSystemProperties.unmarshal( Map.class, resource );
      return parse( map );
   }

   public static DictionaryRoot parse( String resource ) {
      final Map map = Binder.hoconWithoutSystemProperties.unmarshalResource( DictionaryParser.class, Map.class, resource );

      return parse( map );
   }

   public static DictionaryRoot parseFromString( String dictionary ) {
      final Map map = Binder.hoconWithoutSystemProperties.unmarshal( Map.class, dictionary );

      return parse( map );
   }

   private static DictionaryRoot parse( Map map ) {
      final DictionaryRoot dictionaryRoot = ( DictionaryRoot ) parseAsDictionaryValue( map, "", true );
      final ArrayList<InvalidEntry> invalid = new ArrayList<>();
      validate( "", invalid, dictionaryRoot );

      if( !invalid.isEmpty() ) {
         invalid.sort( Comparator.comparing( l -> l.path ) );
         final String msg = invalid
            .stream()
            .map( e -> "path: " + e.path + "; eid: " + e.one.getExternalId() + "; one: " + e.one.getId() + "; two: " + e.two.getId() )
            .collect( Collectors.joining( ", " ) );

         throw new DictionaryError( "duplicate eid: " + msg );

      }

      return dictionaryRoot;
   }

   private static void validate( String path, ArrayList<InvalidEntry> invalid, Dictionary dictionary ) {
      validate( path, invalid, dictionary.getValues() );
   }

   private static void validate( String path, ArrayList<InvalidEntry> invalid, List<? extends Dictionary> values ) {
      final HashMap<Integer, Dictionary> eid = new HashMap<>();

      for( Dictionary dictionary : values ) {
         final Dictionary found = eid.get( dictionary.getExternalId() );
         if( found != null ) {
            invalid.add( new InvalidEntry( found, dictionary, path ) );
         } else {
            eid.put( dictionary.getExternalId(), dictionary );
         }

         validate( path + "/" + dictionary.getId(), invalid, dictionary );
      }
   }

   private static ArrayList<Dictionary> parseValues( List values, String path ) {
      final ArrayList<Dictionary> dv = new ArrayList<>();

      for( int i = 0; i < values.size(); i++ ) {
         final Object value = values.get( i );
         dv.add( parseAsDictionaryValue( value, path + "[" + i + "]", false ) );
      }

      return dv;
   }

   private static String getString( Map map, String field ) {
      return getValue( String.class, map, field, str -> Optional.empty() );
   }

   private static Optional<String> getStringOpt( Map map, String field ) {
      return getValueOpt( String.class, map, field, str -> Optional.empty() );
   }

   private static int getInt( Map map, String field, boolean convert ) {
      return getValue( Integer.class, map, field, str -> intFunc.apply( convert, str ) );
   }

   private static Optional<Boolean> getBooleanOpt( Map map, String field ) {
      return getValueOpt( Boolean.class, map, field, str -> Optional.empty() );
   }

   private static List getList( Map map, String field ) {
      return getValue( List.class, map, field, ( str ) -> Optional.empty() );
   }

   private static <T> T getValue( Class<T> clazz, Map map, String field, Function<Object, Optional<T>> func ) {
      return getValueOpt( clazz, map, field, func ).orElseThrow( () -> new DictionaryFormatError( "field '" + field + "' not found" ) );
   }

   @SuppressWarnings( "unchecked" )
   private static <T> Optional<T> getValueOpt( Class<T> clazz, Map map, String field, Function<Object, Optional<T>> func ) {
      final Object f = map.get( field );

      if( f == null ) return Optional.empty();

      if( clazz.isInstance( f ) ) {
         return Optional.of( ( T ) f );
      }

      final Optional<T> apply = func.apply( f );
      if( apply.isPresent() ) return Optional.ofNullable( apply.get() );

      throw new DictionaryFormatError( "field '" + field + "' type " + f.getClass() + " != " + clazz );
   }

   public static void serialize( DictionaryRoot dictionary, Path path ) {
      serialize( dictionary, path, false );
   }

   public static void serialize( DictionaryRoot dictionary, Path path, boolean format ) {
      try( JsonGenerator jsonGenerator = format ?
         Binder.json.getJsonGenerator( path ).useDefaultPrettyPrinter() :
         Binder.json.getJsonGenerator( path ) ) {

         jsonGenerator.writeStartObject();

         jsonGenerator.writeStringField( NAME, dictionary.name );

         writeProperties( jsonGenerator, dictionary );

         writeValues( jsonGenerator, dictionary.getValues() );

         jsonGenerator.writeEndObject();
      } catch( IOException e ) {
         throw new UncheckedIOException( e );
      }
   }

   private static void writeValues( JsonGenerator jsonGenerator, List<? extends Dictionary> values ) throws IOException {
      if( values.isEmpty() ) return;

      jsonGenerator.writeFieldName( VALUES );
      jsonGenerator.writeStartArray();

      for( val value : values ) {
         jsonGenerator.writeStartObject();
         jsonGenerator.writeStringField( ID, value.getId() );
         if( !value.isEnabled() ) jsonGenerator.writeBooleanField( ENABLED, false );
         jsonGenerator.writeNumberField( EXTERNAL_ID, value.getExternalId() );
         if( value instanceof DictionaryValue ) {
            writeValues( jsonGenerator, value.getValues() );
         }

         writeProperties( jsonGenerator, value );

         jsonGenerator.writeEndObject();
      }

      jsonGenerator.writeEndArray();
   }

   private static void writeProperties( JsonGenerator jsonGenerator, Dictionary value ) {
      value.getProperties().forEach( Try.consume( jsonGenerator::writeObjectField ) );
   }

   private static class InvalidEntry {
      public final Dictionary one;
      public final Dictionary two;
      public final String path;

      public InvalidEntry( Dictionary one, Dictionary two, String path ) {
         this.one = one;
         this.two = two;
         this.path = path;
      }
   }
}
