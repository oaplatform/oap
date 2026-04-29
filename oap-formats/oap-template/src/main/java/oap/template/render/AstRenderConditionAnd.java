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
public class AstRenderConditionAnd extends AstRender {
    final AstRender left;
    final AstRender right;

    public AstRenderConditionAnd( TemplateType type, AstRender left, AstRender right ) {
        super( type );
        this.left = left;
        this.right = right;
    }

    @Override
    public void render( Render render ) {
        String sharedField = sharedTopLevelField();
        if( sharedField != null )
            renderMerged( render, sharedField );
        else
            renderDefault( render );
    }

    private String sharedTopLevelField() {
        String lf = topLevelFieldName( left );
        String rf = topLevelFieldName( right );
        return lf != null && lf.equals( rf ) ? lf : null;
    }

    private static String topLevelFieldName( AstRender node ) {
        if( !( node instanceof AstRenderField f ) ) return null;
        if( f.children.size() != 1 ) return null;
        if( !( f.children.getFirst() instanceof AstRenderNullable ) ) return null;
        return f.fieldName;
    }

    private void renderMerged( Render render, String fieldName ) {
        String leftVar = render.newVariable();
        String rightVar = render.newVariable();
        render.ntab().append( "boolean %s = false;", leftVar );
        render.ntab().append( "boolean %s = false;", rightVar );

        AstRenderField leftField = ( AstRenderField ) left;
        AstRenderNullable leftNullable = ( AstRenderNullable ) leftField.children.getFirst();
        AstRenderField rightField = ( AstRenderField ) right;
        AstRenderNullable rightNullable = ( AstRenderNullable ) rightField.children.getFirst();

        Render.NewVariable sv = render.newVariable( fieldName );
        if( sv.isNew )
            render.ntab().append( "%s %s = %s.%s;", leftField.type.getTypeName(), sv.name, render.field, fieldName );

        render.ntab().append( "if ( %s != null ) {", sv.name );
        Render inner = render.withField( sv.name ).withParentType( leftField.type ).tabInc();
        leftNullable.renderBodyOnly( inner.withBooleanIfVar( leftVar ).newBlock() );
        rightNullable.renderBodyOnly( inner.withBooleanIfVar( rightVar ).newBlock() );
        render.ntab().append( "}" );

        render.ntab().append( "%s = %s && %s;", render.booleanIfVar, leftVar, rightVar );
    }

    private void renderDefault( Render render ) {
        String leftVar = render.newVariable();
        String rightVar = render.newVariable();
        render.ntab().append( "boolean %s = false;", leftVar );
        left.render( render.withBooleanIfVar( leftVar ) );
        render.ntab().append( "boolean %s = false;", rightVar );
        right.render( render.withBooleanIfVar( rightVar ) );
        render.ntab().append( "%s = %s && %s;", render.booleanIfVar, leftVar, rightVar );
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        boolean[] leftCapture = { false };
        boolean[] rightCapture = { false };
        left.interpret( ctx.withBooleanCapture( leftCapture ) );
        right.interpret( ctx.withBooleanCapture( rightCapture ) );
        if( ctx.booleanCapture != null ) ctx.booleanCapture[0] = leftCapture[0] && rightCapture[0];
    }
}
