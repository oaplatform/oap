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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

/**
 * Created by Igor Petrenko on 14.04.2016.
 */
@EqualsAndHashCode
@ToString
public final class DictionaryRoot implements Dictionary {
   public final String name;
   public final ExternalIdType externalIdAs;
   private final List<? extends Dictionary> values;
   @JsonIgnore
   private final HashMap<Integer, String> indexByExternalId = new HashMap<>();
   @JsonIgnore
   private final HashMap<String, Dictionary> indexById = new HashMap<>();
   private final Map<String, Object> properties;

   public DictionaryRoot( String name, List<? extends Dictionary> values ) {
      this( name, ExternalIdType.integer, values, emptyMap() );
   }

   public DictionaryRoot( String name, List<? extends Dictionary> values, Map<String, Object> properties ) {
      this( name, ExternalIdType.integer, values, properties );
   }

   public DictionaryRoot( String name, ExternalIdType externalIdAs, List<? extends Dictionary> values, Map<String, Object> properties ) {
      this.name = name;
      this.externalIdAs = externalIdAs;
      this.values = values;
      this.properties = properties;

      for( Dictionary dv : values ) {
         indexById.put( dv.getId(), dv );
         indexByExternalId.put( dv.getExternalId(), dv.getId() );
      }
   }

   @Override
   public final String getOrDefault( int externlId, String defaultValue ) {
      final String id = indexByExternalId.get( externlId );
      if( id == null ) return defaultValue;
      return id;
   }

   @Override
   public final int getOrDefault( String id, int defaultValue ) {
      final Dictionary rtb = indexById.get( id );
      if( rtb == null ) return defaultValue;
      return rtb.getExternalId();
   }

   @Override
   public final Integer get( String id ) {
      final Dictionary rtb = indexById.get( id );
      if( rtb == null ) return null;
      return rtb.getExternalId();
   }

   @Override
   public boolean containsValueWithId( String id ) {
      return indexById.containsKey( id );
   }

   @Override
   public List<String> ids() {
      return values.stream().map( Dictionary::getId ).collect( toList() );
   }

   @Override
   public int[] externalIds() {
      return values.stream().mapToInt( Dictionary::getExternalId ).toArray();
   }

   @Override
   public Map<String, Object> getProperties() {
      return properties;
   }

   @Override
   public List<? extends Dictionary> getValues() {
      return values;
   }

   @Override
   public String getId() {
      return name;
   }

   @Override
   public Optional<Object> getProperty( String name ) {
      return Optional.ofNullable( properties.get( name ) );
   }

   @Override
   public Optional<? extends Dictionary> getValueOpt( String name ) {
      return Optional.ofNullable( indexById.get( name ) );
   }

   @Override
   public Dictionary getValue( String name ) {
      return indexById.get( name );
   }

   @Override
   public Dictionary getValue( int externalId ) {
      final String name = indexByExternalId.get( externalId );
      if( name == null ) return null;
      return indexById.get( name );
   }

   @Override
   public boolean isEnabled() {
      return true;
   }

   @Override
   public int getExternalId() {
      return -1;
   }

   @Override
   public boolean containsProperty( String name ) {
      return properties.containsKey( name );
   }
}
