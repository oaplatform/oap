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
import oap.template.TemplateConditionHelper;
import oap.template.runtime.RuntimeContext;

import java.util.Collection;
import java.util.Map;

/**
 * Leaf node for the condition path in AstRenderBooleanIf / AstRenderBlockIf.
 * Evaluates the current field value as a boolean condition and writes the result
 * into render.booleanIfVar (codegen) or ctx.booleanCapture (interpreter).
 * <p>
 * Truthiness rules:
 * <ul>
 *   <li>{@code boolean} / {@code Boolean} — the value itself</li>
 *   <li>{@code String} — not empty</li>
 *   <li>{@code Collection} / {@code Map} — not empty</li>
 *   <li>array — length &gt; 0</li>
 *   <li>any other non-null object — true</li>
 * </ul>
 */
@ToString( callSuper = true )
class AstRenderCaptureBoolean extends AstRender {
    AstRenderCaptureBoolean( TemplateType type ) {
        super( type );
    }

    @Override
    public void render( Render render ) {
        Class<?> tc = type.getTypeClass();
        if( boolean.class.equals( tc ) ) {
            render.ntab().append( "%s = %s;", render.booleanIfVar, render.field );
        } else if( Boolean.class.equals( tc ) ) {
            render.ntab().append( "%s = Boolean.TRUE.equals( %s );", render.booleanIfVar, render.field );
        } else if( String.class.equals( tc ) ) {
            render.ntab().append( "%s = !%s.isEmpty();", render.booleanIfVar, render.field );
        } else if( Collection.class.isAssignableFrom( tc ) || Map.class.isAssignableFrom( tc ) ) {
            render.ntab().append( "%s = !%s.isEmpty();", render.booleanIfVar, render.field );
        } else if( tc.isArray() ) {
            render.ntab().append( "%s = %s.length > 0;", render.booleanIfVar, render.field );
        } else {
            render.ntab().append( "%s = oap.template.TemplateConditionHelper.isTruthy( %s );",
                render.booleanIfVar, render.field );
        }
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        if( ctx.booleanCapture != null ) {
            ctx.booleanCapture[0] = TemplateConditionHelper.isTruthy( ctx.currentObject );
        }
    }
}
