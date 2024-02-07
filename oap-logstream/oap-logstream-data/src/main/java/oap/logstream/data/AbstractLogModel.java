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

package oap.logstream.data;

import lombok.extern.slf4j.Slf4j;
import oap.dictionary.DictionaryRoot;
import oap.reflect.TypeRef;
import oap.template.TemplateAccumulator;

import javax.annotation.Nonnull;

@Slf4j
public abstract class AbstractLogModel<TOut, TAccumulator, TA extends TemplateAccumulator<TOut, TAccumulator, TA>> extends DataModel {

    private final TA accumulator;

    public AbstractLogModel( @Nonnull DictionaryRoot model, TA accumulator ) {
        super( model );
        this.accumulator = accumulator;
    }

    public abstract <D, LD extends LogRenderer<D, TOut, TAccumulator, TA>> LD renderer( TypeRef<D> typeRef, TA accumulator, String id, String tag );

    public <D, LD extends LogRenderer<D, TOut, TAccumulator, TA>> LD renderer( TypeRef<D> typeRef, String id, String tag ) {
        return renderer( typeRef, accumulator.newInstance(), id, tag );
    }
}
