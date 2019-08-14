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
import oap.util.Maps;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static oap.json.testng.JsonAsserts.assertJson;
import static oap.json.testng.JsonAsserts.objectOfTestJsonResource;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonPatchTest {

    @Test
    public void patchSimpleObject() {
        String test = "{\n"
            + "\t\"id\": \"i1\",\n"
            + "\t\"count\": 10,\n"
            + "\t\"unknown\": 0.0\n"
            + "}";

        TestObj obj = new TestObj( "i1", 0L, "descr" );
        Map<String, Object> immutableMap = JsonPatch.patch( obj, test );
        obj = Binder.json.unmarshal( TestObj.class, immutableMap );
        assertThat( obj.count ).isEqualTo( 10L );
        assertThat( obj.id ).isEqualTo( "i1" );
        assertThat( obj.description ).isEqualTo( "descr" );
    }

    @Test( expectedExceptions = JsonException.class )
    public void patchObjectFailIncorrectJson() {
        String test = "{\n"
            + "\t\"id\": \"i1\",\n"
            + "\t\"count\": 10`\n"
            + "\t\"unknown\": 0.0\n"
            + "}";

        TestObj obj = objectOfTestJsonResource( getClass(), TestObj.class, "source.json" );

        JsonPatch.patch( obj, test );
    }

    @Test
    public void patchUpdateInner() {
        TestObj obj = objectOfTestJsonResource( getClass(), TestObj.class, "source.json" );

        String patch = "{\"id\": \"i2\", \"description\":\"newdesc\"}";

        Map<String, Object> patched = JsonPatch.patch( obj, "list", o -> Lists.find( ( List<Map<String, Object>> ) o.getOrDefault( "list", Lists.empty() ), p -> p.get( "id" ).equals( "i2" ) ).orElseGet( Maps::empty ), patch );
        assertJson( patched )
            .isStructurallyEqualToResource( getClass(), "patched.json" );
    }

    @Test
    public void patchAddInnerToExistingList() {
        TestObj obj = objectOfTestJsonResource( getClass(), TestObj.class, "source.json" );

        String patch = "{\"id\": \"i3\", \"description\":\"newdesc\", \"count\": 1 }";

        Map<String, Object> patched = JsonPatch.patch( obj, "list", o -> Lists.find( ( List<Map<String, Object>> ) o.get( "list" ), p -> Objects.equals( p.get( "id" ), "i3" ) ).orElseGet( Maps::empty ), patch );
        assertJson( patched )
            .isStructurallyEqualToResource( getClass(), "added.json" );
    }

    @Test
    public void patchAddInner() {
        TestObj obj = objectOfTestJsonResource( getClass(), TestObj.class, "source_no_list.json" );

        String patch = "{\"id\": \"i2\", \"description\":\"newdesc\", \"count\": 0 }";

        Map<String, Object> patched = JsonPatch.patch( obj, "list", o -> Lists.find( ( List<Map<String, Object>> ) o.getOrDefault( "list", Lists.empty() ), p -> Objects.equals( p.get( "id" ), "i2" ) ).orElseGet( Maps::empty ), patch );
        assertJson( patched )
            .isStructurallyEqualToResource( getClass(), "added_no_list.json" );
    }
}
