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

package oap.template;

import lombok.ToString;
import oap.template.TemplateGrammarAdaptor.MaxMin;

import java.util.ArrayList;
import java.util.List;

@ToString( callSuper = true )
public class AstOr extends Ast {
    final String orVariable;
    private final ArrayList<MaxMin> or = new ArrayList<>();

    AstOr( TemplateType type ) {
        super( type );

        orVariable = newVariable();
    }

    public void addTry( List<MaxMin> asts ) {
        for( var ast : asts ) {
            var astRunnable = new AstRunnable( type );
            astRunnable.addChild( ast.top );
            or.add( new MaxMin( astRunnable, ast.bottom ) );
        }
    }

    @Override
    protected void print( StringBuilder buffer, String prefix, String childrenPrefix ) {
        printTop( buffer, prefix );
        buffer.append( childrenPrefix ).append( "│OR" );
        buffer.append( '\n' );

        for( var i = 0; i < or.size(); i++ ) {
            var cp = "│".repeat( or.size() - i );
            or.get( i ).top.print( buffer, childrenPrefix + cp + "└── ", childrenPrefix + cp + "    " );
        }

        printChildren( buffer, childrenPrefix, children );
    }

    @Override
    void render( Render render ) {
        var minMax = or.get( 0 );

        var r = render;
        r.ntab().append( render.templateAccumulator.getTypeName() ).append( " " ).append( orVariable ).append( " = null;" );
        for( var i = 0; i < or.size(); i++ ) {
            minMax = or.get( i );
            var astRunnable = ( AstRunnable ) minMax.top;

            astRunnable.render( r );
            r = r
                .ntab().append( "%s.run();", astRunnable.newFunctionId )
                .ntab().append( "if( !%s.isEmpty() ) { ", astRunnable.templateAccumulatorName )
                .tabInc().ntab().append( "%s = %s.get();", orVariable, astRunnable.templateAccumulatorName )
                .tabDec();

            if( i < or.size() - 1 ) r = r.ntab().append( "} else {" ).tabInc();
        }

        for( var i = 0; i < or.size(); i++ ) r = r.tabDec().ntab().append( "}" );

        var newRender = r.withField( orVariable ).withParentType( new TemplateType( Object.class ) );
        children.forEach( a -> a.render( newRender ) );
    }
}
