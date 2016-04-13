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

/**
 * Created by Igor Petrenko on 13.04.2016.
 */
public class ExtendsTest extends AbstractSchemaTest {
   @Test
   public void testExtends() {
      String schema = "{" +
         "type: object," +
         "extends: \"schema/test2.json\"," +
         "properties: {}" +
         "}";

      String schema2 = "{" +
         "type:object," +
         "properties: {" +
         "  a: {" +
         "    type: string" +
         "  }" +
         "}" +
         "}";

      vOk( schema, "{'a': 'test'}", ( url ) -> schema2, false );
      vFail( schema, "{'a': 1}",
         "/a: instance is of type number, which is none of the allowed primitive types ([string])",
         ( url ) -> schema2
      );
   }

   @Test
   public void testMergeInnerObject() {
      String schema = "{" +
         "type: object," +
         "additionalProperties: false," +
         "extends: test2," +
         "properties: {" +
         "  o: {" +
         "    type:object," +
         "    properties: {" +
         "      a1: {type:string}" +
         "    }" +
         "}" +
         "}" +
         "}";

      String schema2 = "{" +
         "type:object," +
         "properties: {" +
         "  o: {" +
         "    type:object," +
         "    properties: {" +
         "      a2: {type:string}" +
         "    }" +
         "}" +
         "}" +
         "}";

      vOk( schema, "{'o': {'a1':'test'}}", ( url ) -> schema2, false );
      vOk( schema, "{'o': {'a2':'test'}}", ( url ) -> schema2, false );
   }

   @Test
   public void testMergeInnerArray() {
      String schema = "{" +
         "type: object," +
         "additionalProperties: false," +
         "extends: test2," +
         "properties: {" +
         "  o: {" +
         "    type:array," +
         "    items {" +
         "      type: object," +
         "      properties {" +
         "        a1: {type:string}" +
         "      }" +
         "    }" +
         "}" +
         "}" +
         "}";

      String schema2 = "{" +
         "type:object," +
         "properties: {" +
         "  o: {" +
         "    type:array," +
         "    items {" +
         "      type: object," +
         "      properties {" +
         "        a2: {type:string}" +
         "      }" +
         "    }" +
         "}" +
         "}" +
         "}";

      vOk( schema, "{'o': [{'a1':'test'}]}", ( url ) -> schema2, false );
      vOk( schema, "{'o': [{'a2':'test'}]}", ( url ) -> schema2, false );
   }
}
