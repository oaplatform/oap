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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Igor Petrenko on 14.03.2016.
 */
public class TypeIdFactoryTest extends AbstractTest {
    @Test
    public void testClassMapping() {
        final TestBean b = new TestBean( "1" );
        final String marshal = Binder.json.marshal( new TestContainer( b ) );
        assertThat( marshal ).isEqualTo( "{\"ref\":{\"@object:type\":\"b\",\"id\":\"1\"}}" );

        final TestContainer unmarshal = Binder.json.unmarshal( TestContainer.class, marshal );
        assertThat( unmarshal.ref ).isEqualTo( b );
    }

    @ToString
    @EqualsAndHashCode
    public static class TestContainer {
        @JsonTypeIdResolver( TypeIdFactory.class )
        @JsonTypeInfo( use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "@object:type" )
        public final Object ref;

        public TestContainer( @JsonProperty( "ref" ) Object ref ) {
            this.ref = ref;
        }
    }

    @ToString
    @EqualsAndHashCode
    public static class TestBean {
        public final String id;

        public TestBean( @JsonProperty( "id" ) String id ) {
            this.id = id;
        }
    }
}
