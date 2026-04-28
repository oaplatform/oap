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

import java.util.List;

/**
 * Block with: {{% with field1 }} body {{% end %}}
 * Resolves the scope path once; body is rendered with the scope variable as the current field.
 */
@ToString( callSuper = true )
public class AstRenderBlockWith extends AstRender {
    private final String scopePath;
    private final AstRender scopeAst;
    private final TemplateType scopeType;
    private final List<AstRender> bodyChildren;

    public AstRenderBlockWith( String scopePath, TemplateType type, AstRender scopeAst, TemplateType scopeType, List<AstRender> bodyChildren ) {
        super( type );
        this.scopePath = scopePath;
        this.scopeAst = scopeAst;
        this.scopeType = scopeType;
        this.bodyChildren = bodyChildren;
    }

    @Override
    public void render( Render render ) {
        render.ntab().append( "// --- with ( %s )", scopePath );

        String sv;
        if( isSingleLevelField( scopeAst ) ) {
            AstRenderField sf = ( AstRenderField ) scopeAst;
            new AstRenderField( sf.fieldName, sf.type, sf.forceCast, sf.castType ).render( render );
            sv = render.newVariable( sf.fieldName ).name;
        } else {
            sv = render.newVariableWithCustomPrefix( "with_" );
            render.ntab().append( "%s %s = null;", scopeType.getTypeName(), sv );
            scopeAst.render( render.withScopeVar( sv ) );
        }

        render.ntab().append( "// --- with ( %s ) START BODY", scopePath );

        Render bodyRender = render.newBlock().withFieldDirect( sv ).withParentType( scopeType );
        boolean hasNullable = bodyChildren.stream().anyMatch( c -> extractNullable( c ) != null );

        if( hasNullable ) {
            render.ntab().append( "if ( %s != null ) {", sv );
            Render ifBody = bodyRender.tabInc();
            for( AstRender child : bodyChildren ) {
                AstRenderNullable nullable = extractNullable( child );
                if( nullable != null ) {
                    if( child instanceof AstRenderComment ac ) ifBody.ntab().append( ac.comment );
                    nullable.renderBodyOnly( ifBody );
                } else {
                    child.render( ifBody );
                }
            }
            render.ntab().append( "} else {" );
            Render elseBody = bodyRender.tabInc();
            for( AstRender child : bodyChildren ) {
                AstRenderNullable nullable = extractNullable( child );
                if( nullable != null ) {
                    nullable.renderElseOnly( elseBody );
                } else {
                    child.render( elseBody );
                }
            }
            render.ntab().append( "}" );
        } else {
            for( AstRender child : bodyChildren ) {
                child.render( bodyRender );
            }
        }

        render.ntab().append( "// --- with ( %s ) END body ", scopePath ).n();
    }

    private static boolean isSingleLevelField( AstRender scopeAst ) {
        if( !( scopeAst instanceof AstRenderField sf ) ) return false;
        if( sf.children.size() != 1 ) return false;
        AstRender child = sf.children.getFirst();
        if( !( child instanceof AstRenderNullable ) ) return false;
        return child.children.size() == 1 && child.children.getFirst() instanceof AstRenderCaptureScope;
    }

    private static AstRenderNullable extractNullable( AstRender child ) {
        AstRender current = child;
        while( current instanceof AstRenderComment ic ) {
            if( ic.children.size() != 1 ) return null;
            current = ic.children.getFirst();
        }
        if( current instanceof AstRenderNullable n ) return n;
        return null;
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        Object[] capture = { null };
        scopeAst.interpret( ctx.withScopeCapture( capture ) );
        RuntimeContext bodyCtx = ctx.withCurrentObject( capture[0] );
        bodyChildren.forEach( c -> c.interpret( bodyCtx ) );
    }
}
