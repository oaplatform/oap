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

import oap.util.Lists;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static oap.io.content.ContentReader.ofJson;
import static oap.json.testng.JsonAsserts.assertJson;
import static oap.testng.Asserts.contentOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings( "unchecked" )
public class JsonPatchTest {

    @Test
    public void patchSimpleObject() {
        var test = """
            {
                "id": "i1",
                "count": 10,
                "unknown": 0.0
            }
            """;

        var obj = new TestObj( "i1", 0L, "descr" );
        var immutableMap = JsonPatch.patch( obj, test );
        obj = Binder.json.unmarshal( TestObj.class, immutableMap );
        assertThat( obj.count ).isEqualTo( 10L );
        assertThat( obj.id ).isEqualTo( "i1" );
        assertThat( obj.description ).isEqualTo( "descr" );
    }

    @Test( expectedExceptions = JsonException.class )
    public void patchObjectFailIncorrectJson() {
        var test = """
            {
                "id": "i1",
                "count": 10`
                "unknown": 0.0
            }
            """;

        var obj = contentOfTestResource( getClass(), "source.json", ofJson( TestObj.class ) );

        JsonPatch.patch( obj, test );
    }

    @Test
    public void patchUpdateInner() {
        var obj = contentOfTestResource( getClass(), "source.json", ofJson( TestObj.class ) );

        var patch = """
            {"id": "i2", "description":"newdesc"}
            """;

        var patched = JsonPatch.patch( obj, "list", o -> Lists.find( ( List<Map<String, Object>> ) o.getOrDefault( "list", Lists.empty() ), p -> p.get( "id" ).equals( "i2" ) ).orElseGet( Map::of ), patch );
        assertJson( patched )
            .isEqualTo( getClass(), "patched.json" );
    }

    @Test
    public void patchAddInnerToExistingList() {
        var obj = contentOfTestResource( getClass(), "source.json", ofJson( TestObj.class ) );

        var patch = """
            {"id": "i3", "description":"newdesc", "count": 1 }
            """;

        var patched = JsonPatch.patch( obj, "list", o -> Lists.find( ( List<Map<String, Object>> ) o.get( "list" ), p -> Objects.equals( p.get( "id" ), "i3" ) ).orElseGet( Map::of ), patch );
        assertJson( patched )
            .isEqualTo( getClass(), "added.json" );
    }

    @Test
    public void patchAddInner() {
        var obj = contentOfTestResource( getClass(), "source_no_list.json", ofJson( TestObj.class ) );

        var patch = """
                {"id": "i2", "description":"newdesc", "count": 0 }
            """;

        var patched = JsonPatch.patch( obj, "list", o -> Lists.find( ( List<Map<String, Object>> ) o.getOrDefault( "list", Lists.empty() ), p -> Objects.equals( p.get( "id" ), "i2" ) ).orElseGet( Map::of ), patch );
        assertJson( patched )
            .isEqualTo( getClass(), "added_no_list.json" );
    }
}
