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

package oap.template.tree;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class Exprs {
    public final ArrayList<Expr> exprs = new ArrayList<>();
    public Math math = null;
    public Concatenation concatenation = null;

    public Exprs() {
    }

    public Exprs( List<Expr> exprs ) {
        this.exprs.addAll( exprs );
    }

    public String print() {
        StringBuilder sb = new StringBuilder();

        if( !exprs.isEmpty() ) {
            sb.append( "LIST\n" );

            var it = exprs.iterator();
            while( it.hasNext() ) {
                var item = it.next();

                sb.append( it.hasNext() ? "    ├── " : "    └── " ).append( item.print() ).append( '\n' );

            }
        }

        if( concatenation != null ) sb.append( "CONCATENATION " ).append( concatenation.print() ).append( '\n' );
        if( math != null ) sb.append( "MATH " ).append( math.operation ).append( " " ).append( math.value ).append( '\n' );

        return sb.toString();
    }
}
