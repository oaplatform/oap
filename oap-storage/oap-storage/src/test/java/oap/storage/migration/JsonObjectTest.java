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

package oap.storage.migration;

import oap.json.Binder;
import org.testng.annotations.Test;

import java.util.Map;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonObjectTest {

    @SuppressWarnings( "unchecked" )
    private static Map<String, Object> map( String map ) {
        return Binder.hoconWithoutSystemProperties.unmarshal( Map.class, map );
    }

    @Test
    public void rename() {
        JsonObject obj = new JsonObject( empty(), empty(), map( "{field1 = v1}" ) );
        assertThat( obj.rename( "field1", "field2" ).underlying ).isEqualTo( map( "{field2 = v1}" ) );
    }

    @Test
    public void renameInner() {
        JsonObject obj = new JsonObject( empty(), empty(), map( "{obj.field1 = v1}" ) );
        assertThat( obj.rename( "obj.field1", "obj.field2" ).underlying )
            .isEqualTo( map( "{obj.field2 = v1}" ) );
    }

    @Test
    public void renameIntoArray() {
        JsonObject obj = new JsonObject( empty(), empty(), map( "{obj = [{field1 = v1}, {field1 = v2}]}" ) );
        assertThat( obj.rename( "obj.field1", "obj.field2" ).underlying )
            .isEqualTo( map( "{obj = [{field2 = v1}, {field2 = v2}]}" ) );
    }

    @Test
    public void renameIntoArray2() {
        JsonObject obj = new JsonObject( empty(), empty(), map( "{obj = [{field1 = v1}, {field1 = v2}]}" ) );
        assertThat( obj.rename( "obj.field1", "obj.newObj.field2" ).underlying )
            .isEqualTo( map( "{obj = [{newObj.field2 = v1}, {newObj.field2 = v2}]}" ) );
    }

    @Test
    public void multipleRenameIntoArray() {
        JsonObject obj = new JsonObject( empty(), empty(), map( "{obj = [{field1 = v1, field2 = v1}, {field1 = v2, field2 = v2}]}" ) );
        assertThat( obj
            .rename( "obj.field1", "obj.newObj.newfield1" )
            .rename( "obj.field2", "obj.newObj.newfield2" )
            .underlying )
            .isEqualTo( map( "{obj = [{newObj {newfield1 = v1, newfield2 = v1}}, {newObj {newfield1 = v2, newfield2 = v2}}]}" ) );
    }
}
