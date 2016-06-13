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

public class SchemaEnabledTest extends AbstractSchemaTest {
   @Test
   public void testObjectPropertiesEnabled() {
      String schema = "{" +
         "  additionalProperties: false, " +
         "  type: object, " +
         "  properties {v1.type = string,v2 {type = string,enabled = false}}" +
         "}";

      assertOk( schema, "{}" );
      assertOk( schema, "{'v1':'10'}" );
      assertFailure( schema, "{'v2':'20'}", "additional properties are not permitted [v2]" );
   }

   @Test
   public void testObjectPropertiesEnabledDynamic() {
      String schema = "{" +
         "  additionalProperties: false, " +
         "  type: object, " +
         "  properties {v1.type = string,v2 {type = string,enabled = {json-path: v1,eq: test}}}" +
         "}";

      assertOk( schema, "{}" );
      assertOk( schema, "{'v1':'10'}" );
      assertOk( schema, "{'v1':'test','v2':'ok'}" );
      assertFailure( schema, "{'v2':'20'}", "additional properties are not permitted [v2]" );
      assertFailure( schema, "{'v1':'?', 'v2':'20'}", "additional properties are not permitted [v2]" );
   }
}
