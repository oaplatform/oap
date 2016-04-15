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
package oap.json.schema._number;

import oap.json.schema.SchemaAST;

import java.util.Optional;

public class NumberSchemaAST extends SchemaAST<NumberSchemaAST> {
   public Optional<Boolean> exclusiveMinimum;

   public Optional<Boolean> exclusiveMaximum;

   public Optional<Double> minimum;

   public Optional<Double> maximum;

   public NumberSchemaAST( CommonSchemaAST common, Optional<Boolean> exclusiveMinimum,
                           Optional<Boolean> exclusiveMaximum, Optional<Double> minimum,
                           Optional<Double> maximum ) {
      super( common );
      this.exclusiveMinimum = exclusiveMinimum;
      this.exclusiveMaximum = exclusiveMaximum;
      this.minimum = minimum;
      this.maximum = maximum;
   }

   @Override
   public NumberSchemaAST merge( NumberSchemaAST cs ) {
      return new NumberSchemaAST(
         common.merge( cs.common ),
         exclusiveMinimum.isPresent() ? exclusiveMinimum : cs.exclusiveMinimum,
         exclusiveMaximum.isPresent() ? exclusiveMaximum : cs.exclusiveMaximum,
         minimum.isPresent() ? minimum : cs.minimum,
         maximum.isPresent() ? maximum : cs.maximum
      );
   }
}
