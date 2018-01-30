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

package oap.json.schema.validator.object;

import oap.json.schema.AbstractSchemaTest;
import org.testng.annotations.Test;

public class ObjectSchemaTest extends AbstractSchemaTest {
   @Test
   public void testObject() {
      String schema = "{type: object, properties: {}}";

      assertOk( schema, "{}" );
      assertOk( schema, "null" );
      assertFailure( schema, "[]",
         "instance is resolve type array, which is none resolve the allowed primitive types ([object])" );
   }

   @Test
   public void testObjectWithField() {
      String schema = "{type: object, properties: {a: {type: string}}}";

      assertOk( schema, "{}" );
      assertOk( schema, "{'a': 'test'}" );
      assertFailure( schema, "{'a': 10}",
         "/a: instance is resolve type number, which is none resolve the allowed primitive types ([string])" );
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

      assertOk( schema, "{}" );
      assertOk( schema, "{'a': {'a': 'test'}}" );
      assertFailure( schema, "{'a': {'a': true}}",
         "/a/a: instance is resolve type boolean, which is none resolve the allowed primitive types ([string])" );
   }

   @Test
   public void testAdditionalPropertiesTrue() {
      String schema = "{type: object, properties: {a: {type: string}}}";

      assertOk( schema, "{}" );
      assertOk( schema, "{'b': 'test'}" );
   }

   @Test
   public void testAdditionalPropertiesFalse() {
      String schema = "{additionalProperties: false, type: object, properties: {a: {type: string}}}";

      assertOk( schema, "{}" );
      assertFailure( schema, "{'b': 'test', 'c': 10}", "additional properties are not permitted [b, c]" );
   }

   @Test
   public void testAdditionalPropertiesFalseInheritance() {
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

      assertOk( schema, "{}" );
      assertFailure( schema, "{'a': {'b': 'test', 'c': 10}}", "/a: additional properties are not permitted [c]" );
   }
}
