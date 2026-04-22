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
 * Redirects render.field to the Java variable bound to a named range variable ($varName).
 * Used when an expression inside a range body starts with $varName (e.g., {{ $item.field }}).
 */
@ToString( callSuper = true )
class AstRenderVarRef extends AstRender {
    private final String varName;

    AstRenderVarRef( String varName, TemplateType varType ) {
        super( varType );
        this.varName = varName;
    }

    @Override
    public void render( Render render ) {
        String javaVar = render.rangeVarMap.get( varName );
        Render varRender = render.withField( javaVar ).withParentType( type );
        children.forEach( c -> c.render( varRender ) );
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        Object value = ctx.rangeVars.get( varName );
        RuntimeContext next = ctx.withCurrentObject( value );
        children.forEach( c -> c.interpret( next ) );
    }
}
