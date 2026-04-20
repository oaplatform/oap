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

import javax.annotation.Nullable;
import java.util.List;

@ToString( callSuper = true )
public class AstRenderBlockIf extends AstRender {
    private final AstRender conditionAst;
    private final List<AstRender> thenChildren;
    @Nullable
    private final List<AstRender> elseChildren;

    public AstRenderBlockIf( TemplateType type, AstRender conditionAst,
                             List<AstRender> thenChildren, @Nullable List<AstRender> elseChildren ) {
        super( type );
        this.conditionAst = conditionAst;
        this.thenChildren = thenChildren;
        this.elseChildren = elseChildren;
    }

    @Override
    public void render( Render render ) {
        String condVar = render.newVariable();
        render.ntab().append( "boolean %s = false;", condVar );

        conditionAst.render( render.withBooleanIfVar( condVar ) );

        render.ntab().append( "if ( %s ) {", condVar );

        Render thenBodyRender = render.tabInc().newBlock();
        for( AstRender child : thenChildren ) {
            child.render( thenBodyRender );
        }

        render.ntab().append( "}" );

        if( elseChildren != null ) {
            render.append( " else {" );

            Render elseBodyRender = render.tabInc().newBlock();
            for( AstRender child : elseChildren ) {
                child.render( elseBodyRender );
            }

            render.ntab().append( "}" );
        }
    }
}
