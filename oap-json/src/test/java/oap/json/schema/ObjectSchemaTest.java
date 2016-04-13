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

import oap.util.Maps;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

import static oap.util.Pair.__;

public class ObjectSchemaTest extends AbstractSchemaTest {
   @Test
   public void testObject() {
      String schema = "{type: object, properties: {}}";

      vOk( schema, "{}" );
      vOk( schema, "null" );
      vFail( schema, "[]",
         "instance is of type array, which is none of the allowed primitive types ([object])" );
   }

   @Test
   public void testObjectWithField() {
      String schema = "{type: object, properties: {a: {type: string}}}";

      vOk( schema, "{}" );
      vOk( schema, "{'a': 'test'}" );
      vFail( schema, "{'a': 10}",
         "/a: instance is of type number, which is none of the allowed primitive types ([string])" );
   }

   @Test
   public void testObjectObjectWithField() {
      String schema = "{" +
         "type: object, " +
         "properties: {" +
         "  a: {" +
         "    type: object, " +
         "    properties: {" +
         "      a: {" +
         "        type: string" +
         "      }" +
         "    }" +
         "  }" +
         "}" +
         "}";

      vOk( schema, "{}" );
      vOk( schema, "{'a': {'a': 'test'}}" );
      vFail( schema, "{'a': {'a': true}}",
         "/a/a: instance is of type boolean, which is none of the allowed primitive types ([string])" );
   }

   @Test
   public void testAdditionalProperties_true() {
      String schema = "{type: object, properties: {a: {type: string}}}";

      vOk( schema, "{}" );
      Assert.assertEquals( vOk( schema, "{'b': 'test'}" ), Maps.addAll( new HashMap<>(), __( "b", "test" ) ) );
   }

   @Test
   public void testAdditionalProperties_false() {
      String schema = "{additionalProperties: false, type: object, properties: {a: {type: string}}}";

      vOk( schema, "{}" );
      vFail( schema, "{'b': 'test', 'c': 10}", "additional properties are not permitted [b,c]" );
   }

   @Test
   public void testAdditionalProperties_false_inheritance() {
      String schema = "{" +
         "additionalProperties: false, " +
         "type: object, " +
         "properties: {" +
         " a: {" +
         "  type: object," +
         "  properties: {" +
         "   b: {" +
         "    type: string" +
         "   }" +
         "  }" +
         " }" +
         "}" +
         "}";

      vOk( schema, "{}" );
      vFail( schema, "{'a': {'b': 'test', 'c': 10}}", "/a: additional properties are not permitted [c]" );
   }
}
