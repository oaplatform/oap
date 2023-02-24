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

package oap.template;

import lombok.ToString;

@ToString( callSuper = true )
public class AstRunnable extends Ast {
    AstRunnable( TemplateType type ) {
        super( type );
    }

    @Override
    void render( Render render ) {
        var newFunctionId = render.newVariable();
        var templateAccumulatorName = "acc_" + newFunctionId;

        render( newFunctionId, templateAccumulatorName, render );
    }

    void render( String newFunctionId, String templateAccumulatorName, Render render ) {
        render
            .ntab().append( "var %s = acc.newInstance();", templateAccumulatorName )
            .ntab().append( "Runnable %s = () -> {", newFunctionId );

        var newRender = render.withParentType( type ).withTemplateAccumulatorName( templateAccumulatorName ).tabInc();
        children.forEach( ast -> ast.render( newRender ) );

        render.ntab().append( "};" );
    }
}