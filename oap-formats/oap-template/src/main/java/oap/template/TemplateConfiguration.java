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

import java.util.ArrayList;

public final class TemplateConfiguration {
    public static final TemplateConfiguration DEFAULT = new TemplateConfiguration()
        .withExpression( "{{", "}}" )
        .withExpression( "${", "}" );

    public final ArrayList<Expression> expressions;

    public TemplateConfiguration() {
        this.expressions = new ArrayList<>();
    }

    private TemplateConfiguration( ArrayList<Expression> expressions ) {
        this.expressions = new ArrayList<>( expressions );
    }

    public TemplateConfiguration withExpression( String prefix, String suffix ) {
        ArrayList<Expression> newExpressions = new ArrayList<>( expressions );
        newExpressions.add( new Expression( prefix, suffix ) );
        return new TemplateConfiguration( newExpressions );
    }

    public static final class Expression {
        public final String prefix;
        public final String suffix;

        public Expression( String prefix, String suffix ) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }
}
