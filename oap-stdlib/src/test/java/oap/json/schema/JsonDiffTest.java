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

import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonDiffTest extends AbstractSchemaTest {
    @Test
    public void newString() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"string\""
            + "}"
            + "}}";

        assertThat( diff( schema, "{}", "{\"test\":\"new value\"}" ) ).containsOnly( newF( "test", "\"new value\"" ) );
    }

    @Test
    public void delString() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"string\""
            + "}"
            + "}}";

        assertThat( diff( schema, "{\"test\":\"old value\"}", "{}" ) ).containsOnly( delF( "test", "\"old value\"" ) );
    }

    @Test
    public void updString() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"string\""
            + "}"
            + "}}";

        assertThat( diff( schema, "{\"test\":\"old value\"}", "{\"test\":\"new value\"}" ) )
            .containsOnly( updF( "test", "\"old value\"", "\"new value\"" ) );
    }

    @Test
    public void updStringNested() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"object\","
            + "    \"properties\":{"
            + "      \"testin\":{"
            + "        \"type\":\"string\""
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":{\"testin\":\"old value\"}}", "{\"test\":{\"testin\":\"new value\"}}" ) )
            .containsOnly( updF( "test.testin", "\"old value\"", "\"new value\"" ) );
    }

    @Test
    public void newNested() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"object\","
            + "    \"properties\":{"
            + "      \"testin\":{"
            + "        \"type\":\"string\""
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{}", "{\"test\":{\"testin\":\"new value\"}}" ) )
            .containsOnly( newO( "test", "{\"testin\":\"new value\"}" ) );
    }

    @Test
    public void delNested() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"object\","
            + "    \"properties\":{"
            + "      \"testin\":{"
            + "        \"type\":\"string\""
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":{\"testin\":\"new value\"}}", "{}" ) )
            .containsOnly( delO( "test", "{\"testin\":\"new value\"}" ) );
    }

    @Test
    public void newArray() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"items\":{"
            + "      \"type\":\"string\""
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{}", "{\"test\":[\"new value\"]}" ) ).containsOnly( newA( "test", "[\"new value\"]" ) );
    }

    @Test
    public void newArrayItem() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"items\":{"
            + "      \"type\":\"string\""
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[\"old value\"]}", "{\"test\":[\"old value\",\"new value\"]}" ) )
            .containsOnly( newA( "test", "[\"new value\"]" ) );
    }

    @Test
    public void emptyArrays() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"items\":{"
            + "      \"type\":\"string\""
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{}", "{}" ) ).isEmpty();
    }

    @Test
    public void delArrayItem() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"items\":{"
            + "      \"type\":\"string\""
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[\"old value\", \"old value 2\"]}", "{\"test\":[\"old value 2\"]}" ) )
            .containsOnly( delA( "test", "[\"old value\"]" ) );
    }

    @Test
    public void updateArrayItem() {
        final String schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"items\":{"
            + "      \"type\":\"string\""
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[\"old value\", \"old value 2\"]}", "{\"test\":[\"old value 2\", \"new value\"]}" ) )
            .containsOnly( updA( "test", "[\"old value\"]", "[\"new value\"]" ) );
    }

    @Test
    public void updStringIntoArrayObject() {
        var schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"id\":\"test\","
            + "    \"items\":{"
            + "      \"type\":\"object\","
            + "      \"properties\":{"
            + "        \"test\":{"
            + "          \"type\":\"string\""
            + "        },"
            + "        \"testin\":{"
            + "          \"type\":\"string\""
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[{\"test\":\"id\",\"testin\":\"old value\"}]}",
            "{\"test\":[{\"test\":\"id\",\"testin\":\"new value\"}]}" ) )
            .containsOnly( updF( "test[id].testin", "\"old value\"", "\"new value\"" ) );
    }

    @Test
    public void addArrayObject() {
        var schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"id\":\"test\","
            + "    \"items\":{"
            + "      \"type\":\"object\","
            + "      \"properties\":{"
            + "        \"test\":{"
            + "          \"type\":\"string\""
            + "        },"
            + "        \"testin\":{"
            + "          \"type\":\"string\""
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[]}",
            "{\"test\":[{\"test\":\"id\",\"testin\":\"new value\"}]}" ) )
            .containsOnly( newO( "test[id]", "{\"test\":\"id\",\"testin\":\"new value\"}" ) );
    }

    @Test
    public void removeArrayObject() {
        var schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"id\":\"test\","
            + "    \"items\":{"
            + "      \"type\":\"object\","
            + "      \"properties\":{"
            + "        \"test\":{"
            + "          \"type\":\"string\""
            + "        },"
            + "        \"testin\":{"
            + "          \"type\":\"string\""
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[{\"test\":\"id\",\"testin\":\"new value\"}]}",
            "{\"test\":[]}" ) )
            .containsOnly( delO( "test[id]", "{\"test\":\"id\",\"testin\":\"new value\"}" ) );
    }

    @Test
    public void updateArrayObjectWithoutId() {
        var schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"id\":\"{index}\","
            + "    \"items\":{"
            + "      \"type\":\"object\","
            + "      \"properties\":{"
            + "        \"test\":{"
            + "          \"type\":\"string\""
            + "        },"
            + "        \"testin\":{"
            + "          \"type\":\"string\""
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[{\"test\":\"id\",\"testin\":\"old value\"}]}",
            "{\"test\":[{\"test\":\"id\",\"testin\":\"new value\"}]}" ) )
            .containsOnly( updF( "test[0].testin", "\"old value\"", "\"new value\"" ) );
    }

    @Test
    public void addArrayObjectWithoutId() {
        var schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"id\":\"{index}\","
            + "    \"items\":{"
            + "      \"type\":\"object\","
            + "      \"properties\":{"
            + "        \"test\":{"
            + "          \"type\":\"string\""
            + "        },"
            + "        \"testin\":{"
            + "          \"type\":\"string\""
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[{}]}",
            "{\"test\":[{}, {\"test\":\"id\",\"testin\":\"new value\"}]}" ) )
            .containsOnly( newO( "test[1]", "{\"test\":\"id\",\"testin\":\"new value\"}" ) );
    }

    @Test
    public void removeArrayObjectWithoutId() {
        var schema = "{"
            + "\"type\":\"object\","
            + "\"properties\":{"
            + "  \"test\": {"
            + "    \"type\":\"array\","
            + "    \"id\":\"{index}\","
            + "    \"items\":{"
            + "      \"type\":\"object\","
            + "      \"properties\":{"
            + "        \"test\":{"
            + "          \"type\":\"string\""
            + "        },"
            + "        \"testin\":{"
            + "          \"type\":\"string\""
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}}";

        assertThat( diff( schema, "{\"test\":[{\"test\":\"id\",\"testin\":\"new value\"}]}",
            "{\"test\":[]}" ) )
            .containsOnly( delO( "test[0]", "{\"test\":\"id\",\"testin\":\"new value\"}" ) );
    }

    private List<JsonDiff.Line> diff( String schema, String from, String to ) {
        return JsonDiff.diff( from, to, schema( schema ) ).getDiff();
    }

    private JsonDiff.Line newF( String path, String value ) {
        return newX( path, value, JsonDiff.Line.LineType.FIELD );
    }

    private JsonDiff.Line newO( String path, String value ) {
        return newX( path, value, JsonDiff.Line.LineType.OBJECT );
    }

    private JsonDiff.Line newA( String path, String value ) {
        return newX( path, value, JsonDiff.Line.LineType.ARRAY );
    }

    private JsonDiff.Line newX( String path, String value, JsonDiff.Line.LineType lineType ) {
        return new JsonDiff.Line( path, lineType, empty(), of( value ) );
    }

    private JsonDiff.Line delF( String path, String value ) {
        return del( path, JsonDiff.Line.LineType.FIELD, of( value ), empty() );
    }

    private JsonDiff.Line delO( String path, String value ) {
        return del( path, JsonDiff.Line.LineType.OBJECT, of( value ), empty() );
    }

    private JsonDiff.Line delA( String path, String value ) {
        return del( path, JsonDiff.Line.LineType.ARRAY, of( value ), empty() );
    }

    private JsonDiff.Line del( String path, JsonDiff.Line.LineType object, Optional<String> value2, Optional<String> empty ) {
        return new JsonDiff.Line( path, object, value2, empty );
    }

    private JsonDiff.Line updF( String path, String oldValue, String newValue ) {
        return upd( path, oldValue, newValue, JsonDiff.Line.LineType.FIELD );
    }

    private JsonDiff.Line updA( String path, String oldValue, String newValue ) {
        return upd( path, oldValue, newValue, JsonDiff.Line.LineType.ARRAY );
    }

    private JsonDiff.Line upd( String path, String oldValue, String newValue, JsonDiff.Line.LineType lineType ) {
        return new JsonDiff.Line( path, lineType, of( oldValue ), of( newValue ) );
    }
}
