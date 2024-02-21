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

package oap.json.schema;

import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonPathTest {
    @Test
    public void traverse() {
        final List<Object> traverse = new JsonPath( "flights.items.rules.items.region.items", Optional.of( "flights/0/rules/0/country/0" ) )
            .traverse( Maps.of( __( "flights", Lists.of( Maps.of( __( "rules", Lists.of( Maps.of( __( "region", Lists.of( "reg" ) ) ) ) ) ) ) ) ) );

        assertThat( traverse ).containsExactly( "reg" );
    }

    @Test
    public void traverseNotFound() {
        final List<Object> traverse = new JsonPath( "a.items.field", Optional.of( "a/0/unknown" ) )
            .traverse( Maps.of( __( "a", Lists.of( Maps.of( __( "unknown", 10 ) ) ) ) ) );

        assertThat( traverse ).isEmpty();
    }

    @Test
    public void getFixedPath() {
        final String path = new JsonPath( "flights.items.rules.items.region.items", Optional.of( "flights/0/rules/5/country/0" ) )
            .getFixedPath();

        assertThat( path ).isEqualTo( "flights.0.rules.5.region.0" );
    }
}
