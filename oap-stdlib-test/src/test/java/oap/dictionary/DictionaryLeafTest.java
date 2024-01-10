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

import oap.json.Binder;
import org.testng.annotations.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class DictionaryLeafTest {
    @Test
    public void serializeProperties() {
        var dictionaryLeaf = new DictionaryLeaf( "id", true, 1, Map.of( "p1", "v1" ) );
        assertThat( Binder.json.marshal( dictionaryLeaf ) )
            .isEqualTo( "{\"id\":\"id\",\"externalId\":1,\"properties\":{\"p1\":\"v1\"}}" );
    }

    @Test
    public void serializeEnabled() {
        var dictionaryLeaf = new DictionaryLeaf( "id", false, 1, emptyMap() );
        assertThat( Binder.json.marshal( dictionaryLeaf ) )
            .isEqualTo( "{\"id\":\"id\",\"externalId\":1,\"enabled\":false}" );
    }

}
