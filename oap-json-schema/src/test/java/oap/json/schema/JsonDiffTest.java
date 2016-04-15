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

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static oap.json.schema.JsonDiff.diff;

public class JsonDiffTest extends AbstractSchemaTest {
    @Test
    public void testNewString() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"string\"" +
            "}" +
            "}}";

        Assertions.assertThat( __diff( schema, "{}", "{\"test\":\"new value\"}" ) ).containsOnly( __newF( "test", "\"new value\"" ) );
    }

    @Test
    public void testDelString() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"string\"" +
            "}" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":\"old value\"}", "{}" ) ).containsOnly( __delF( "test", "\"old value\"" ) );
    }

    @Test
    public void testUpdString() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"string\"" +
            "}" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":\"old value\"}", "{\"test\":\"new value\"}" ) )
            .containsOnly( __updF( "test", "\"old value\"", "\"new value\"" ) );
    }

    @Test
    public void testUpdStringNested() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"object\"," +
            "    \"properties\":{" +
            "      \"testin\":{" +
            "        \"type\":\"string\"" +
            "      }" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":{\"testin\":\"old value\"}}", "{\"test\":{\"testin\":\"new value\"}}" ) )
            .containsOnly( __updF( "test.testin", "\"old value\"", "\"new value\"" ) );
    }

    @Test
    public void testNewNested() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"object\"," +
            "    \"properties\":{" +
            "      \"testin\":{" +
            "        \"type\":\"string\"" +
            "      }" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{}", "{\"test\":{\"testin\":\"new value\"}}" ) )
            .containsOnly( __newO( "test", "{\"testin\":\"new value\"}" ) );
    }

    @Test
    public void testDelNested() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"object\"," +
            "    \"properties\":{" +
            "      \"testin\":{" +
            "        \"type\":\"string\"" +
            "      }" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":{\"testin\":\"new value\"}}", "{}" ) )
            .containsOnly( __delO( "test", "{\"testin\":\"new value\"}" ) );
    }

    @Test
    public void testNewArray() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"array\"," +
            "    \"items\":{" +
            "      \"type\":\"string\"" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{}", "{\"test\":[\"new value\"]}" ) ).containsOnly( __newA( "test", "[\"new value\"]" ) );
    }

    @Test
    public void testNewArrayItem() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"array\"," +
            "    \"items\":{" +
            "      \"type\":\"string\"" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":[\"old value\"]}", "{\"test\":[\"old value\",\"new value\"]}" ) )
            .containsOnly( __newA( "test", "[\"new value\"]" ) );
    }

    @Test
    public void testEmptyArrays() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"array\"," +
            "    \"items\":{" +
            "      \"type\":\"string\"" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{}", "{}" ) ).isEmpty();
    }

    @Test
    public void testDelArrayItem() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"array\"," +
            "    \"items\":{" +
            "      \"type\":\"string\"" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":[\"old value\", \"old value 2\"]}", "{\"test\":[\"old value 2\"]}" ) )
            .containsOnly( __delA( "test", "[\"old value\"]" ) );
    }

    @Test
    public void testUpdateArrayItem() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"array\"," +
            "    \"items\":{" +
            "      \"type\":\"string\"" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":[\"old value\", \"old value 2\"]}", "{\"test\":[\"old value 2\", \"new value\"]}" ) )
            .containsOnly( __updA( "test", "[\"old value\"]", "[\"new value\"]" ) );
    }

    @Test
    public void testUpdStringIntoArrayObject() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"array\"," +
            "    \"id\":\"test\"," +
            "    \"items\":{" +
            "      \"type\":\"object\"," +
            "      \"properties\":{" +
            "        \"test\":{" +
            "          \"type\":\"string\"" +
            "        }," +
            "        \"testin\":{" +
            "          \"type\":\"string\"" +
            "        }" +
            "      }" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":[{\"test\":\"id\",\"testin\":\"old value\"}]}",
            "{\"test\":[{\"test\":\"id\",\"testin\":\"new value\"}]}" ) )
            .containsOnly( __updF( "test[id].testin", "\"old value\"", "\"new value\"" ) );
    }

    @Test
    public void testAddArrayObject() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"array\"," +
            "    \"id\":\"test\"," +
            "    \"items\":{" +
            "      \"type\":\"object\"," +
            "      \"properties\":{" +
            "        \"test\":{" +
            "          \"type\":\"string\"" +
            "        }," +
            "        \"testin\":{" +
            "          \"type\":\"string\"" +
            "        }" +
            "      }" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":[]}",
            "{\"test\":[{\"test\":\"id\",\"testin\":\"new value\"}]}" ) )
            .containsOnly( __newO( "test[id]", "{\"test\":\"id\",\"testin\":\"new value\"}" ) );
    }

    @Test
    public void testRemoveArrayObject() throws Exception {
        final String schema = "{" +
            "\"type\":\"object\"," +
            "\"properties\":{" +
            "  \"test\": {" +
            "    \"type\":\"array\"," +
            "    \"id\":\"test\"," +
            "    \"items\":{" +
            "      \"type\":\"object\"," +
            "      \"properties\":{" +
            "        \"test\":{" +
            "          \"type\":\"string\"" +
            "        }," +
            "        \"testin\":{" +
            "          \"type\":\"string\"" +
            "        }" +
            "      }" +
            "    }" +
            "  }" +
            "}}";

        Assertions.assertThat( __diff( schema, "{\"test\":[{\"test\":\"id\",\"testin\":\"new value\"}]}",
            "{\"test\":[]}" ) )
            .containsOnly( __delO( "test[id]", "{\"test\":\"id\",\"testin\":\"new value\"}" ) );
    }

    private List<JsonDiff.Line> __diff( String schema, String from, String to ) {
        final SchemaAST ast = JsonValidatorFactory.schemaFromString( schema, NO_STORAGE ).schema;

        return diff( from, to, ast ).getDiff();
    }

    private JsonDiff.Line __newF( String path, String value ) {
        return ___new( path, value, JsonDiff.Line.LineType.FIELD );
    }

    private JsonDiff.Line __newO( String path, String value ) {
        return ___new( path, value, JsonDiff.Line.LineType.OBJECT );
    }

    private JsonDiff.Line __newA( String path, String value ) {
        return ___new( path, value, JsonDiff.Line.LineType.ARRAY );
    }

    private JsonDiff.Line ___new( String path, String value, JsonDiff.Line.LineType lineType ) {
        return new JsonDiff.Line( path, lineType, empty(), of( value ) );
    }

    private JsonDiff.Line __delF( String path, String value ) {
        return __del( path, JsonDiff.Line.LineType.FIELD, of( value ), empty() );
    }

    private JsonDiff.Line __delO( String path, String value ) {
        return __del( path, JsonDiff.Line.LineType.OBJECT, of( value ), empty() );
    }

    private JsonDiff.Line __delA( String path, String value ) {
        return __del( path, JsonDiff.Line.LineType.ARRAY, of( value ), empty() );
    }

    private JsonDiff.Line __del( String path, JsonDiff.Line.LineType object, Optional<String> value2, Optional<String> empty ) {
        return new JsonDiff.Line( path, object, value2, empty );
    }

    private JsonDiff.Line __updF( String path, String oldValue, String newValue ) {
        return __upd( path, oldValue, newValue, JsonDiff.Line.LineType.FIELD );
    }

    private JsonDiff.Line __updA( String path, String oldValue, String newValue ) {
        return __upd( path, oldValue, newValue, JsonDiff.Line.LineType.ARRAY );
    }

    private JsonDiff.Line __upd( String path, String oldValue, String newValue, JsonDiff.Line.LineType lineType ) {
        return new JsonDiff.Line( path, lineType, of( oldValue ), of( newValue ) );
    }
}