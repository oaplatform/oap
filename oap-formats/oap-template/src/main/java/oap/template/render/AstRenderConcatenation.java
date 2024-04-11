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
import oap.template.TemplateAccumulator;

import java.util.ArrayList;
import java.util.List;

@ToString( callSuper = true )
public class AstRenderConcatenation extends AstRender {
    final ArrayList<AstRender> items = new ArrayList<>();

    public AstRenderConcatenation( TemplateType type, List<AstRender> items ) {
        super( type );

        this.items.addAll( items );
    }

    @Override
    public void render( Render render ) {
        var templateAccumulatorName = "acc_" + render.newVariable();
        render
            .ntab().append( "var %s = new TemplateAccumulatorString();", templateAccumulatorName );

        for( var item : items ) {
            item.render( render.withTemplateAccumulatorName( templateAccumulatorName ) );
        }

        var newRender = render.withField( templateAccumulatorName ).withParentType( new TemplateType( TemplateAccumulator.class ) );
        children.forEach( a -> a.render( newRender ) );
    }

    @Override
    public void print( StringBuilder buffer, String prefix, String childrenPrefix ) {
        printTop( buffer, prefix );
        buffer.append( childrenPrefix ).append( "│CONCATENATION" );
        buffer.append( '\n' );

        for( var i = 0; i < items.size(); i++ ) {
            var cp = "│".repeat( items.size() - i );
            items.get( i ).print( buffer, childrenPrefix + cp + "└── ", childrenPrefix + cp + "    " );
        }

        printChildren( buffer, childrenPrefix, children );
    }
}
