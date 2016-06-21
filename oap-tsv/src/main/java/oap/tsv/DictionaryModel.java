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

package oap.tsv;

import oap.dictionary.Dictionary;
import org.apache.commons.collections4.keyvalue.AbstractMapEntry;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.primitives.Ints.asList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by Admin on 17.06.2016.
 */
public class DictionaryModel {
   private final Dictionary dictionary;

   public DictionaryModel( Dictionary dictionary ) {
      this.dictionary = dictionary;
   }

   public String getVersion() {
      return ( String ) dictionary.getProperty( "version" ).orElse( "v1" );
   }

   public Model toModel( String table ) {
      final Model model = new Model( false );

      final List<? extends Dictionary> values = dictionary.getValueOpt( table ).get().getValues();
      for( Dictionary field : values ) {
         final int offset = field.getExternalId();
         final String type = ( String ) field.getProperty( "type" ).get();

         switch( type ) {
            case "STRING":
               model.s( offset );
               break;
            case "INTEGER":
               model.i( offset );
               break;
            case "LONG":
               model.l( offset );
               break;
            case "DOUBLE":
               model.d( offset );
               break;
            case "BOOLEAN":
               model.b( offset );
               break;
            default:
               throw new IllegalArgumentException( "Unknown field type " + type );
         }
      }

      return model;
   }

   public Map<String, Object> getDefaults( String table ) {
      final Dictionary tableDictionary = DictionaryModel.this.dictionary.getValueOpt( table ).get();

      final HashMap<String, Object> defaults = new HashMap<>();

      for( Dictionary value : tableDictionary.getValues() ) {
         defaults.put(value.getId(),  value.getProperty( "default" ).get());
      }

      return defaults;
   }

   public Map<String, Integer> toMap( String table ) {
      final Dictionary tableDictionary = DictionaryModel.this.dictionary.getValueOpt( table ).get();

      return new NameToIdMap( tableDictionary );
   }

   public Set<String> getTables() {
      return dictionary.ids().stream().collect( toSet() );
   }

   private static class NameToIdMap implements Map<String, Integer> {
      private final Dictionary tableDictionary;

      public NameToIdMap( Dictionary tableDictionary ) {
         this.tableDictionary = tableDictionary;
      }

      @Override
      public int size() {
         return tableDictionary.getValues().size();
      }

      @Override
      public boolean isEmpty() {
         return tableDictionary.getValues().isEmpty();
      }

      @Override
      public boolean containsKey( Object key ) {
         return tableDictionary.containsValueWithId( key.toString() );
      }

      @Override
      public boolean containsValue( Object value ) {
         throw new NotImplementedException( "" );
      }

      @Override
      public Integer get( Object key ) {
         return tableDictionary.get( key.toString() );
      }

      @Override
      public Integer put( String key, Integer value ) {
         throw new NotImplementedException( "" );
      }

      @Override
      public Integer remove( Object key ) {
         throw new NotImplementedException( "" );
      }

      @Override
      public void putAll( Map<? extends String, ? extends Integer> m ) {
         throw new NotImplementedException( "" );
      }

      @Override
      public void clear() {
         throw new NotImplementedException( "" );
      }

      @Override
      public Set<String> keySet() {
         return tableDictionary.ids().stream().collect( toSet() );
      }

      @Override
      public Collection<Integer> values() {
         return asList( tableDictionary.externalIds() );
      }

      @Override
      public Set<Entry<String, Integer>> entrySet() {
         return tableDictionary.getValues().stream().map( v -> new AbstractMapEntry<String, Integer>( v.getId(), v.getExternalId() ) {
         } ).collect( toSet() );
      }
   }
}
