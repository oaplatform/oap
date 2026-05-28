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

package oap.template.render;

import lombok.ToString;
import oap.template.runtime.RuntimeContext;

/**
 * Evaluates a condition expression and emits its boolean result directly to the accumulator.
 * Used when a standalone comparison or negation appears as a top-level template expression,
 * e.g. {@code {{ field == 'value' }}} or {@code {{ not booleanField }}}.
 */
@ToString( callSuper = true )
public class AstRenderConditionResult extends AstRender {
    private final AstRender conditionAst;

    public AstRenderConditionResult( TemplateType type, AstRender conditionAst ) {
        super( type );
        this.conditionAst = conditionAst;
    }

    @Override
    public void render( Render render ) {
        String condVar = render.newVariable();
        render.ntab().append( "boolean %s = false;", condVar );
        conditionAst.render( render.withBooleanIfVar( condVar ) );
        render.ntab().append( "%s.accept( %s );", render.templateAccumulatorName, condVar );
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        boolean[] capture = { false };
        conditionAst.interpret( ctx.withBooleanCapture( capture ) );
        ctx.acc.accept( capture[0] );
    }
}
