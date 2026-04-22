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
import oap.template.runtime.ReflectionCache;
import oap.template.runtime.RuntimeContext;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Block range over an integer interval.
 * {{% range $k := 1 .. 10 }} body {{% end %}}
 * {{% range $k := 1 .. 10 step 2 }} body {{% end %}}
 * {{% range $k := start .. end step step }} body {{% end %}}
 */
@ToString( callSuper = true )
public class AstRenderBlockRangeInterval extends AstRender {

    public sealed interface IntRangeValue permits IntRangeValue.Literal, IntRangeValue.Field {
        record Literal( int value ) implements IntRangeValue {
            @Override
            public String toCode( String obj ) {
                return String.valueOf( value );
            }
        }
        record Field( String fieldName ) implements IntRangeValue {
            @Override
            public String toCode( String obj ) {
                return obj + "." + fieldName;
            }
        }
        String toCode( String currentObj );
    }

    private final String varName;
    private final IntRangeValue from;
    private final IntRangeValue to;
    private final IntRangeValue step;
    private final List<AstRender> bodyChildren;
    @Nullable private final List<AstRender> elseChildren;

    public AstRenderBlockRangeInterval( TemplateType type, String varName,
                                        IntRangeValue from, IntRangeValue to, IntRangeValue step,
                                        List<AstRender> bodyChildren, @Nullable List<AstRender> elseChildren ) {
        super( type );
        this.varName = varName;
        this.from = from;
        this.to = to;
        this.step = step;
        this.bodyChildren = bodyChildren;
        this.elseChildren = elseChildren;
    }

    @Override
    public void render( Render render ) {
        String loopVar = render.newVariable();
        String fromCode = from.toCode( render.field );
        String toCode = to.toCode( render.field );
        String stepCode = step.toCode( render.field );

        render.ntab().append( "for( int %s = %s; %s <= %s; %s += %s ) {",
            loopVar, fromCode, loopVar, toCode, loopVar, stepCode );

        Render innerRender = render.tabInc().newBlock().withRangeVar( varName, loopVar );
        for( AstRender child : bodyChildren ) child.render( innerRender );

        render.ntab().append( "}" );
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        int fromVal = resolveInt( from, ctx );
        int toVal = resolveInt( to, ctx );
        int stepVal = resolveInt( step, ctx );
        for( int k = fromVal; k <= toVal; k += stepVal ) {
            RuntimeContext inner = ctx.withRangeVar( varName, k );
            bodyChildren.forEach( c -> c.interpret( inner ) );
        }
    }

    private static int resolveInt( IntRangeValue v, RuntimeContext ctx ) {
        if( v instanceof IntRangeValue.Literal lit ) return lit.value();
        IntRangeValue.Field fv = ( IntRangeValue.Field ) v;
        Object val = ReflectionCache.getFieldValue( ctx.currentObject, fv.fieldName() );
        return val instanceof Number n ? n.intValue() : Integer.parseInt( String.valueOf( val ) );
    }
}
