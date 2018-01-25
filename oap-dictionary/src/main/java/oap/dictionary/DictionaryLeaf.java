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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Igor Petrenko on 29.04.2016.
 */
@EqualsAndHashCode
@ToString
@JsonPropertyOrder( { "id", "externalId", "enabled", "properties" } )
public class DictionaryLeaf implements Dictionary {
    @JsonIgnore
    private final boolean enabled;
    private final int externalId;
    @JsonInclude( JsonInclude.Include.NON_DEFAULT )
    @JsonProperty
    private final Map<String, Object> properties;
    private final String id;

    public DictionaryLeaf( String id, boolean enabled, int externalId, Map<String, Object> properties ) {
        this.id = id;
        this.enabled = enabled;
        this.externalId = externalId;
        this.properties = properties;
    }

    @Override
    public int getOrDefault( String id, int defaultValue ) {
        return defaultValue;
    }

    @Override
    public Integer get( String id ) {
        return null;
    }

    @Override
    public String getOrDefault( int externlId, String defaultValue ) {
        return defaultValue;
    }

    @Override
    public boolean containsValueWithId( String id ) {
        return false;
    }

    @Override
    public List<String> ids() {
        return Collections.emptyList();
    }

    @Override
    public int[] externalIds() {
        return new int[0];
    }

    @JsonIgnore
    public Map<String, Object> getProperties() {
        return properties != null ? Collections.unmodifiableMap( properties ) : Collections.emptyMap();
    }

    @Override
    public Optional<? extends Dictionary> getValueOpt( String name ) {
        return Optional.empty();
    }

    @Override
    public Dictionary getValue( String name ) {
        return null;
    }

    @Override
    public Dictionary getValue( int externalId ) {
        return null;
    }

    @Override
    public List<? extends Dictionary> getValues() {
        return Collections.emptyList();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T> Optional<T> getProperty( String name ) {
        return Optional.ofNullable( ( T ) getProperties().get( name ) );
    }

    @JsonIgnore
    public boolean isEnabled() {
        return enabled;
    }

    @JsonInclude( JsonInclude.Include.NON_NULL )
    @JsonProperty( value = "enabled" )
    private Boolean isEnabledJackson() {
        return enabled ? null : false;
    }

    public int getExternalId() {
        return externalId;
    }

    @Override
    public boolean containsProperty( String name ) {
        return properties != null && properties.containsKey( name );
    }
}
