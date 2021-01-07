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

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class CompactAstPostProcessor implements Consumer<Ast> {
    public static final CompactAstPostProcessor INSTANCE = new CompactAstPostProcessor();

    @SuppressWarnings( "checkstyle:ModifiedControlVariable" )
    @Override
    public void accept( Ast ast ) {
        var children = ast.children;

        for( var i = 0; i < children.size(); i++ ) {
            var left = children.get( i );
            if( left instanceof AstText ) continue;

            if( i >= children.size() - 1 ) return;
            i++;
            AstText astText = null;
            var right = children.get( i );
            if( right instanceof AstText ) {
                astText = ( AstText ) right;
                if( i >= children.size() - 1 ) return;
                i++;
                right = children.get( i );
            }

            if( merge( left, right, astText ) ) {
                children.remove( i );
                i--;

                if( astText != null ) {
                    children.remove( i );
                    i--;
                }

                i--;
            }
        }

        log.trace( "\n --- compact ---\n" + ast.print() );
    }

    public boolean merge( Ast root, Ast ast, AstText optText ) {
        if( !( root.getClass().equals( ast.getClass() ) ) ) return false;

        if( root instanceof AstExpression ) {
            if( ast.children.size() == 1 && root.children.size() > 0 ) {
                var c = ast.children.get( 0 );
                var sc = root.children.get( 0 );
                if( sc.equalsAst( c ) ) {
                    merge( sc, c, optText );
                    ( ( AstExpression ) root ).content.addAll( ( ( AstExpression ) ast ).content );
                    return true;
                }
            }

            return false;
        } else if( root instanceof AstIfElse ) {
            var r = defaultMerge( root, ast, optText );

            if( optText != null ) {
                var astIfElse = ( AstIfElse ) root;
                if( astIfElse.printIfOptEmpty == null )
                    astIfElse.printIfOptEmpty = optText;
                else astIfElse.printIfOptEmpty = new AstText( astIfElse.printIfOptEmpty.text + optText.text );
            }

            return r;
        } else {
            return defaultMerge( root, ast, optText );
        }
    }


    private boolean defaultMerge( Ast root, Ast ast, AstText optText ) {
        if( ast.children.size() == 1 && root.children.size() > 0 ) {
            var c = ast.children.get( 0 );
            var sc = root.children.get( 0 );
            if( sc.equalsAst( c ) ) {
                merge( sc, c, optText );
                return true;
            } else {
                if( optText != null ) root.children.add( 0, optText );
                root.children.add( 0, c );
            }
        }
        return false;
    }
}
