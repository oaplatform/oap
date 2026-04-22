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

import oap.template.render.AstRenderRoot;
import oap.template.runtime.RuntimeContext;

/**
 * A {@link Template} implementation that interprets the AST directly at runtime
 * using Java reflection — no code generation or compilation required.
 *
 * <p>Use {@link TemplateEngine#getRuntimeTemplate} to obtain an instance.
 */
public class RuntimeTemplate<TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    implements Template<TIn, TOut, TOutMutable, TA> {

    private final TA acc;
    private final AstRenderRoot ast;

    public RuntimeTemplate( TA acc, AstRenderRoot ast ) {
        this.acc = acc;
        this.ast = ast;
    }

    @Override
    public TA render( TIn obj, boolean eol ) {
        TA newAcc = acc.newInstance();
        try {
            ast.interpret( RuntimeContext.root( obj, newAcc ) );
        } catch( TemplateException e ) {
            throw e;
        } catch( Exception e ) {
            throw new TemplateException( e );
        }
        return newAcc.addEol( eol );
    }

    @Override
    public TA render( TIn obj, boolean eol, TOutMutable tOut ) {
        TA newAcc = acc.newInstance( tOut );
        try {
            ast.interpret( RuntimeContext.root( obj, newAcc ) );
        } catch( TemplateException e ) {
            throw e;
        } catch( Exception e ) {
            throw new TemplateException( e );
        }
        return newAcc.addEol( eol );
    }
}
