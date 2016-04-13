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

import oap.testng.Env;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Igor Petrenko on 13.04.2016.
 */
public class DictionaryFromEnumGeneratorTest {
   @Test
   public void testMain() throws Exception {
      DictionaryFromEnumGenerator.main( new String[]{
         TestDictEnum.class.getName() + "," + TestDictEnum2.class.getName(),
         "bbb,ccc",
         Env.tmp( "tmp" )
      } );

      assertThat( Env.tmpPath( "tmp" ).resolve( "bbb.json" ) ).hasContent( "{\"values\":[\"A\",\"B\",\"TEST\"]}" );
      assertThat( Env.tmpPath( "tmp" ).resolve( "ccc.json" ) ).hasContent( "{\"values\":[\"TEST1\",\"TEST2\"]}" );
   }

   public enum TestDictEnum {
      A, B, UNKNOWN, TEST
   }

   public enum TestDictEnum2 {
      TEST1, TEST2
   }

}