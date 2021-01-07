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

package oap.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.id.Id;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeIdAccessorFactoryTest {
    @Test
    public void classMapping() {
        var b = new TestBean( "1" );
        var marshal = Binder.json.marshal( new TestContainer( b ) );
        assertThat( marshal ).isEqualTo( "{\"ref\":{\"@object:type\":\"b\",\"id\":\"1\"}}" );

        var unmarshal = Binder.json.unmarshal( TestContainer.class, marshal );
        assertThat( unmarshal.ref ).isEqualTo( b );
    }

    @Test
    public void anySetterWithCustomValueWithTypeId() {
        var json = "{\"b\":{\"id\":\"val\"}}";

        var vm = Binder.json.unmarshal( TestCustomValueMap.class, json );
        assertThat( vm.properties )
            .isNotNull()
            .containsKey( "b" )
            .containsValue( new TestBean( "val" ) );
    }

    @Test
    public void anySetterWithCustomValueWithoutTypeId() {
        var json = "{\"unknown-typeid1\":\"10\", \"unknown-typeid2\":{\"a\":\"10\"}}";

        var vm = Binder.json.unmarshal( TestCustomValueMap.class, json );
        assertThat( vm.properties )
            .isNotNull()
            .containsEntry( "unknown-typeid1", "10" )
            .containsEntry( "unknown-typeid2", Map.of( "a", "10" ) );
    }

    @ToString
    @EqualsAndHashCode
    public static class TestContainer {
        @JsonTypeIdResolver( TypeIdFactory.class )
        @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, property = "@object:type" )
        public final Object ref;

        public TestContainer( @JsonProperty( "ref" ) Object ref ) {
            this.ref = ref;
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class TestBean {
        @Id
        public final String id;

        public TestBean( @JsonProperty( "id" ) String id ) {
            this.id = id;
        }
    }

    @ToString
    public static class TestCustomValueMap {
        @JsonIgnore
        public final HashMap<String, Object> properties = new HashMap<>();

        @JsonAnySetter
        public void putProperty( String name, CustomValue<?> customValue ) {
            properties.put( name, customValue.getValue() );
        }

        @JsonAnyGetter
        public Map<String, Object> getProperties() {
            return properties;
        }
    }
}
