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

package oap.template.ast;

import lombok.ToString;

import java.util.function.Supplier;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@ToString( callSuper = true )
public abstract class AstIfElse extends Ast {
    public Ast elseAst = null;

    public AstIfElse( TemplateType type ) {
        super( type );
    }

    @Override
    public boolean equalsAst( Ast ast ) {
        return getClass().equals( ast.getClass() );
    }

    @Override
    public void render( Render render ) {
        render
            .ntab().append( "if ( %s%s ) {", render.field, getTrue() );

        var iv = getInnerVariable( render::newVariable );
        if( iv != null ) {
            render.tabInc().ntab().append( getInnerVariableSetter( iv, render ) );
        }

        var newRender = render.withParentType( type ).tabInc();
        if( iv != null ) newRender = newRender.withField( iv );
        for( var c : children ) {
            c.render( newRender );
        }

        render.ntab().append( "}" );

        if( elseAst != null ) {
            var nRender = render.append( " else {" )
                .tabInc();
            if( nRender.tryVariable != null ) nRender.ntab().append( "%s = true;", nRender.tryVariable );
            elseAst.render( nRender );
            render.ntab().append( "}" );
        }
    }

    @Override
    public void print( StringBuilder buffer, String prefix, String childrenPrefix ) {
        if( elseAst != null ) {
            printTop( buffer, prefix );
            buffer.append( childrenPrefix ).append( "│" ).append( getFalseToString() );
            buffer.append( '\n' );

            elseAst.print( buffer, childrenPrefix + "│" + "└── ", childrenPrefix + "│" + "    " );

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
