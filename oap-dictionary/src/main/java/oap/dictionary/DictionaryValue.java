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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

/**
 * Created by Igor Petrenko on 29.04.2016.
 */
@EqualsAndHashCode( callSuper = true )
@ToString( callSuper = true )
@JsonPropertyOrder( { "id", "externalId", "enabled", "properties", "values" } )
public class DictionaryValue extends DictionaryLeaf implements Dictionary {
    public final List<? extends Dictionary> values;

    public DictionaryValue( String id, boolean enabled, int externalId ) {
        this( id, enabled, externalId, emptyList(), emptyMap() );
    }

    public DictionaryValue( String id, boolean enabled, int externalId, List<? extends Dictionary> values ) {
        this( id, enabled, externalId, values, emptyMap() );
    }

    public DictionaryValue(
        String id,
        boolean enabled,
        int externalId,
        Map<String, Object> properties
    ) {
        this( id, enabled, externalId, emptyList(), properties );
    }

    public DictionaryValue(
        String id,
        boolean enabled,
        int externalId,
        List<? extends Dictionary> values,
        Map<String, Object> properties
    ) {
        super( id, enabled, externalId, properties );
        this.values = values;
    }

    @Override
    public int getOrDefault( String id, int defaultValue ) {
        return getValueOpt( id ).map( Dictionary::getExternalId ).orElse( defaultValue );
    }

    @Override
    public Integer get( String id ) {
        return getValueOpt( id ).map( Dictionary::getExternalId ).orElse( null );
    }

    @Override
    public String getOrDefault( int externlId, String defaultValue ) {
        return values
            .stream()
            .filter( v -> v.getExternalId() == externlId )
            .findAny()
            .map( Dictionary::getId )
            .orElse( defaultValue );
    }

    @Override
    public boolean containsValueWithId( String id ) {
        return getValueOpt( id ).isPresent();
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
    public List<? extends Dictionary> getValues() {
        return values;
    }

    @Override
    public Optional<? extends Dictionary> getValueOpt( String name ) {
        return values.stream().filter( l -> l.getId().equals( name ) ).findAny();
    }

    @Override
    public Dictionary getValue( String name ) {
        return getValueOpt( name ).orElse( null );
    }

    @Override
    public Dictionary getValue( int externalId ) {
        return values.stream().filter( l -> l.getExternalId() == externalId ).findAny().orElse( null );
    }

    @Override
    public DictionaryLeaf cloneDictionary() {
        return new DictionaryValue(
            id,
            enabled,
            externalId,
            values.stream().map( Dictionary::cloneDictionary ).collect( toList() ),
            new HashMap<>( properties )
        );
    }
}
