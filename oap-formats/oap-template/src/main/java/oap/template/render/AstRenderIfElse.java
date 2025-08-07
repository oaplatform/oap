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

import java.util.function.Supplier;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@ToString( callSuper = true )
public abstract class AstRenderIfElse extends AstRender {
    public AstRender elseAstRender = null;

    public AstRenderIfElse( TemplateType type ) {
        super( type );
    }

    @Override
    public void render( Render render ) {
        render
            .ntab().append( "if ( %s%s ) {", render.field, getTrue() );

        String iv = getInnerVariable( render::newVariable );
        if( iv != null ) {
            render.tabInc().ntab().append( getInnerVariableSetter( iv, render ) );
        }

        Render newRender = render.withParentType( type ).tabInc();
        if( iv != null ) {
            newRender = newRender.withField( iv );
        }
        for( AstRender c : children ) {
            c.render( newRender.newBlock() );
        }

        render.ntab().append( "}" );

        if( elseAstRender != null ) {
            Render nRender = render.append( " else {" )
                .tabInc();
            if( nRender.tryVariable != null ) nRender.ntab().append( "%s = true;", nRender.tryVariable );
            elseAstRender.render( nRender.newBlock() );
            render.ntab().append( "}" );
        }
    }

    @Override
    public void print( StringBuilder buffer, String prefix, String childrenPrefix ) {
        if( elseAstRender != null ) {
            printTop( buffer, prefix );
            buffer.append( childrenPrefix ).append( "│" ).append( getFalseToString() );
            buffer.append( '\n' );

            elseAstRender.print( buffer, childrenPrefix + "│" + "└── ", childrenPrefix + "│" + "    " );

            printChildren( buffer, childrenPrefix, children );
        } else {
            super.print( buffer, prefix, childrenPrefix );
        }
    }

    protected abstract String getTrue();

    protected abstract String getFalseToString();

    protected abstract String getInnerVariable( Supplier<String> newVariable );

    protected abstract String getInnerVariableSetter( String variableName, Render render );
}
