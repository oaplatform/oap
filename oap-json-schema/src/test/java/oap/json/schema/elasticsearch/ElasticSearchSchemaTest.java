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

package oap.json.schema.elasticsearch;

import oap.json.schema.AbstractSchemaTest;
import org.testng.annotations.Test;

import static oap.json.schema.elasticsearch.ElasticSearchSchema.convert;
import static oap.testng.Asserts.assertString;

public class ElasticSearchSchemaTest extends AbstractSchemaTest {
    @Test
    public void testConvert_string() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:string}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"keyword\"}}}" );
    }

    @Test
    public void testConvert__id() throws Exception {
        assertString( convert( schema( "{type:object,properties:{_id: {type:string}}}" ) ) )
            .isEqualTo( "{\"properties\":{}}" );
    }

    @Test
    public void testConvert_dictionary() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type: dictionary, name: dict}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"keyword\"}}}" );
    }

    @Test
    public void testConvert_date() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:date}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"date\"}}}" );
    }

    @Test
    public void testConvert_text() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:text}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"text\"}}}" );
    }

    @Test
    public void testConvert_string_include_in_all() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:string,include_in_all:false}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"keyword\",\"include_in_all\":false}}}" );
        assertString( convert( schema( "{type:object,properties:{a: {type:string,include_in_all:true}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"keyword\"}}}" );
    }

    @Test
    public void testConvert_string_index() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:string,index:false}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"keyword\",\"index\":false}}}" );
        assertString( convert( schema( "{type:object,properties:{a: {type:string,index:true}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"keyword\"}}}" );
    }

    @Test
    public void testConvert_string_analyzer() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:string,analyzer:english}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"keyword\",\"analyzer\":\"english\"}}}" );
    }

    @Test
    public void testConvert_boolean() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:boolean}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"boolean\"}}}" );
    }

    @Test
    public void testConvert_integer() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:integer}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"long\"}}}" );
    }

    @Test
    public void testConvert_long() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:long}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"long\"}}}" );
    }

    @Test
    public void testConvert_double() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:double}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"double\"}}}" );
    }

    @Test
    public void testConvert_object() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:object,properties:{a:{type:date}}}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"date\"}}}}}" );
    }

    @Test
    public void testConvert_object_nested() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:object,nested:true,properties:{a:{type:date}}}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"nested\",\"properties\":{\"a\":{\"type\":\"date\"}}}}}" );
    }

    @Test
    public void testConvert_array() throws Exception {
        assertString( convert( schema( "{type:object,properties:{a: {type:array,items:{type:date}}}}" ) ) )
            .isEqualTo( "{\"properties\":{\"a\":{\"type\":\"date\"}}}" );
    }

    @Test
    public void testConvert_dynamic() throws Exception {
        assertString( convert( schema( "{type:object,dynamic:true,properties:{a: {type:object,properties:{a:{type:date}}}}}" ) ) )
            .isEqualTo( "{\"dynamic\":\"true\",\"properties\":{\"a\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"date\"}}}}}" );
        assertString( convert( schema( "{type:object,dynamic:false,properties:{a: {type:object,properties:{a:{type:date}}}}}" ) ) )
            .isEqualTo( "{\"dynamic\":\"false\",\"properties\":{\"a\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"date\"}}}}}" );
        assertString( convert( schema( "{type:object,dynamic:strict,properties:{a: {type:object,properties:{a:{type:date}}}}}" ) ) )
            .isEqualTo( "{\"dynamic\":\"strict\",\"properties\":{\"a\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"date\"}}}}}" );
    }
}
