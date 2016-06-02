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

package oap.etl.accumulator;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.tsv.Model;

import java.util.List;

@ToString
@EqualsAndHashCode( exclude = { "money", "events" } )
public class CostAccumulator implements Accumulator<CostAccumulator> {
   private int moneyField;
   private int eventField;
   private long money;
   private int events;

   public CostAccumulator( int moneyField, int eventField ) {
      this.moneyField = moneyField;
      this.eventField = eventField;
   }

   @Override
   public void accumulate( List<Object> values ) {
      this.money += ( ( Number ) values.get( this.moneyField ) ).longValue();
      this.events += ( ( Number ) values.get( this.eventField ) ).longValue();
   }

   @Override
   public void reset() {
      this.money = 0;
      this.events = 0;
   }

   @Override
   public Double result() {
      return this.events > 0 ? this.money / this.events : 0.0;
   }

   @Override
   public CostAccumulator clone() {
      return new CostAccumulator( moneyField, eventField );
   }

   @Override
   public Model.ColumnType getModelType() {
      return Model.ColumnType.DOUBLE;
   }
}
