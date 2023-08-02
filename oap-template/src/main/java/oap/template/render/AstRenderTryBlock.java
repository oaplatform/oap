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

@ToString( callSuper = true )
public class AstRenderTryBlock extends AstRender {
    public AstRenderTryBlock( TemplateType type ) {
        super( type );
    }

    @Override
    public void render( Render render ) {
        var newFunctionId = render.newVariable();
        var templateAccumulatorName = "acc_" + newFunctionId;

        render( newFunctionId, templateAccumulatorName, render );
    }

    public void render( String newFunctionId, String templateAccumulatorName, Render render ) {
        String emptyVariable = "empty_" + newFunctionId;

        render
            .ntab().append( "var %s = acc.newInstance();", templateAccumulatorName )
            .ntab().append( "BooleanSupplier %s = () -> {", newFunctionId )
            .tabInc()
            .ntab().append( "boolean %s = false;", emptyVariable );

        var newRender = render.withParentType( type )
            .withTryVariable( emptyVariable )
            .withTemplateAccumulatorName( templateAccumulatorName )
            .tabInc();
        children.forEach( ast -> ast.render( newRender ) );

        render
            .tabInc()
            .ntab().append( " return %s;", emptyVariable )
            .tabDec()
            .ntab().append( "};" );
    }
}
