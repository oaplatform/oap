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

package oap.json.schema._object;

import oap.json.schema.DefaultSchemaASTWrapper;
import oap.json.schema.SchemaASTWrapper;
import oap.json.schema.SchemaId;
import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectSchemaASTWrapperTest extends AbstractTest {
   @Test
   public void testGetChildren() throws Exception {
      final ObjectSchemaASTWrapper ow = new ObjectSchemaASTWrapper( new SchemaId( "", "id" ) ) {{
         extendsSchema = Optional.of(
            new ObjectSchemaASTWrapper( new SchemaId( "", "id" ) ) {{
               extendsSchema = Optional.empty();
               declaredProperties = new LinkedHashMap<String, SchemaASTWrapper>() {{
                  put( "b", new DefaultSchemaASTWrapper( new SchemaId( "", "id.b" ) ) );
               }};
            }}
         );
         declaredProperties = new LinkedHashMap<String, SchemaASTWrapper>() {{
            put( "a", new DefaultSchemaASTWrapper( new SchemaId( "", "id.a" ) ) );
            put( "b", new DefaultSchemaASTWrapper( new SchemaId( "", "id.b" ) ) );
         }};
      }};

      assertThat( ow.getChildren() ).containsKeys( "a", "b" );
      assertThat( ow.getChildren().get( "b" ) ).hasSize( 2 );
   }

   @Test
   public void testGetChildrenWithoutParent() throws Exception {
      final ObjectSchemaASTWrapper ow = new ObjectSchemaASTWrapper( new SchemaId( "", "id" ) ) {{
         extendsSchema = Optional.empty();
         declaredProperties = new LinkedHashMap<String, SchemaASTWrapper>() {{
            put( "a", new DefaultSchemaASTWrapper( new SchemaId( "", "id.a" ) ) );
         }};
      }};

      assertThat( ow.getChildren() ).hasSize( 1 );
   }

}
