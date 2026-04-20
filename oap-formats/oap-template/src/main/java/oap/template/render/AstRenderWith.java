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

/**
 * Inline with: {{ with (field1) field2 | default field3 end }}
 * Resolves the scope path once, then evaluates body expressions against the scope type.
 */
@ToString( callSuper = true )
public class AstRenderWith extends AstRender {
    private final AstRender scopeAst;
    private final TemplateType scopeType;
    private final AstRender bodyAst;

    public AstRenderWith( TemplateType type, AstRender scopeAst, TemplateType scopeType, AstRender bodyAst ) {
        super( type );
        this.scopeAst = scopeAst;
        this.scopeType = scopeType;
        this.bodyAst = bodyAst;
    }

    @Override
    public void render( Render render ) {
        String sv = render.newVariable();
        render.ntab().append( "%s %s = null;", scopeType.getTypeName(), sv );
        scopeAst.render( render.withScopeVar( sv ) );
        bodyAst.render( render.withField( sv ).withParentType( scopeType ) );
    }
}
