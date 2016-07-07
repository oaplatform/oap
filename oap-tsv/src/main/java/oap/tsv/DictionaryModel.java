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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Created by Admin on 17.06.2016.
 */
public class DictionaryModel {
   private final Dictionary dictionary;

   public DictionaryModel( Dictionary dictionary ) {
      this.dictionary = dictionary;
   }

   public int getVersion() {
      return dictionary.getProperty( "version" ).map( v -> ( ( Number ) v ).intValue() ).orElse( 1 );
   }

   public Model toModel( String table ) {
      final Model model = new Model( false );

      final List<? extends Dictionary> values = dictionary.getValue( table ).getValues();
      for( Dictionary field : values ) {
         final String id = field.getId();
         final int offset = field.getExternalId();
         final String type = ( String ) field.getProperty( "type" ).get();

         switch( type ) {
            case "STRING":
               model.s( id, offset );
               break;
            case "INTEGER":
               model.i( id, offset );
               break;
            case "LONG":
               model.l( id, offset );
               break;
            case "DOUBLE":
               model.d( id, offset );
               break;
            case "BOOLEAN":
               model.b( id, offset );
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
         defaults.put( value.getId(), value.getProperty( "default" ).get() );
      }

      return defaults;
   }

   public Set<String> getTables() {
      return dictionary.ids().stream().collect( toSet() );
   }
}
