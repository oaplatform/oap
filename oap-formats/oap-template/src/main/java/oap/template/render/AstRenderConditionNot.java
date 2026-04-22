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

@ToString( callSuper = true )
public class AstRenderConditionNot extends AstRender {
    private final AstRender inner;

    public AstRenderConditionNot( TemplateType type, AstRender inner ) {
        super( type );
        this.inner = inner;
    }

    @Override
    public void render( Render render ) {
        String innerVar = render.newVariable();
        render.ntab().append( "boolean %s = false;", innerVar );
        inner.render( render.withBooleanIfVar( innerVar ) );
        render.ntab().append( "%s = !%s;", render.booleanIfVar, innerVar );
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        boolean[] innerCapture = { false };
        inner.interpret( ctx.withBooleanCapture( innerCapture ) );
        if( ctx.booleanCapture != null ) ctx.booleanCapture[0] = !innerCapture[0];
    }
}
