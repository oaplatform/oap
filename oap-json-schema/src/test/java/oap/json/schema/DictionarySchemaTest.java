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

import oap.io.Files;
import oap.json.schema._dictionary.DictionaryJsonValidator;
import oap.testng.Env;
import org.testng.annotations.Test;

import java.nio.file.Path;

public class DictionarySchemaTest extends AbstractSchemaTest {
   @Test
   public void testDictionary() {
      final Path test = Env.tmpPath( "test" );
      Files.writeString( test.resolve( "dict.json" ), "{values = [test1, test2, test3]}" );

      new DictionaryJsonValidator( test );

      String schema = "{type: dictionary, name: dict}";

      vOk( schema, "null" );
      vOk( schema, "'test1'" );
      vOk( schema, "'test2'" );

      vFail( schema, "'test4'", "instance does not match any member of the enumeration [test1,test2,test3]" );
   }

   @Test
   public void testUnknownDictionary() {
      final Path test = Env.tmpPath( "test" );
      Files.writeString( test.resolve( "dict.json" ), "{values = [test1, test2, test3]}" );

      new DictionaryJsonValidator( test );

      String schema = "{type: dictionary, name: unknown}";

      vFail( schema, "'test4'", "dictionary not found" );
   }
}
