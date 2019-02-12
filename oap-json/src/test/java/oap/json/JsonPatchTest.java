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

import static oap.json.testng.JsonAsserts.assertJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class JsonPatchTest {

    @Test
    public void patchObjectSuccess() throws Exception {
        String test = "{\n"
            + "\t\"id\": \"i1\",\n"
            + "\t\"count\": 10,\n"
            + "\t\"unknown\": 0.0\n"
            + "}";

        TestObj testObj = new TestObj( "i1", 0L, "descr" );
        Map<String, Object> immutableMap = JsonPatch.patch( testObj, test );
        testObj = Binder.json.unmarshal( TestObj.class, immutableMap );
        assertThat( testObj.count ).isEqualTo( 10L );
        assertThat( testObj.id ).isEqualTo( "i1" );
        assertThat( testObj.description ).isEqualTo( "descr" );
    }

    @Test
    public void patchObjectFailIncorrectJson() throws Exception {
        String test = "{\n"
            + "\t\"id\": \"i1\",\n"
            + "\t\"count\": 10\n"
            + "\t\"unknown\": 0.0\n"
            + "}";

        TestObj testObj = new TestObj( "i1", 0L, "descr" );

        try {
            JsonPatch.patch( testObj, test );
            fail( "Test fail" );
        } catch( JsonException e ) {
            assertThat( e ).hasMessageContaining( "json error: Unexpected character ('\"' (code 34)): was expecting comma to separate Object entries" );
        }
    }

    @Test
    public void patch() {
        TestObj obj = new TestObj( "1", 0L, "desc" )
            .add( new TestObj( "i2", 0L, "i2" ) );

        String patch = "{\"id\": \"i2\", \"description\":\"newdesc\"}";
        assertJson( Binder.json.marshal( JsonPatch.patch( obj, o -> Lists.find( ( List<Map<String, Object>> ) o.get( "list" ), p -> p.get( "id" ).equals( "i2" ) ).get(), patch ) ) )
            .isStructurallyEqualTo( Binder.json.marshal( new TestObj( "1", 0L, "desc" )
                .add( new TestObj( "i2", 0L, "newdesc" ) ) ) );
    }

    @Test
    public void patchAddNew() {
        TestObj obj = new TestObj( "1", 0L, "desc" );

        String patch = "{\"id\": \"i2\", \"description\":\"newdesc\", \"count\": 0 }";

        String marshal = Binder.json.marshal( JsonPatch.patchAddNew( obj, "list", o -> ( List<Map<String, Object>> ) o.getOrDefault( "list", Lists.empty() ), patch ) );
        assertJson( marshal )
            .isStructurallyEqualTo( Binder.json.marshal( new TestObj( "1", 0L, "desc" )
                .add( new TestObj( "i2", 0L, "newdesc" ) ) ) );
    }

    @Test
    public void patchAddNewToExistList() {
        TestObj obj = new TestObj( "1", 0L, "desc" )
            .add( new TestObj( "i3", 0L, "desc2" ) );

        String patch = "{\"id\": \"i2\", \"description\":\"newdesc\", \"count\": 0 }";

        String marshal = Binder.json.marshal( JsonPatch.patchAddNew( obj, "list", o -> ( List<Map<String, Object>> ) o.getOrDefault( "list", Lists.empty() ), patch ) );
        assertJson( marshal )
            .isStructurallyEqualTo( Binder.json.marshal( new TestObj( "1", 0L, "desc" )
                .add( new TestObj( "i3", 0L, "desc2" ) )
                .add( new TestObj( "i2", 0L, "newdesc" ) ) ) );
    }


}
