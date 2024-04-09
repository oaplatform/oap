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
public class Expression {
    public final String comment;
    public final String castType;
    public final ArrayList<Exprs> or = new ArrayList<>();
    public final String defaultValue;
    public final Func function;

    public Expression( String comment, String castType, List<Exprs> or, String defaultValue, Func function ) {
        this.comment = comment;
        this.castType = castType;
        this.or.addAll( or );
        this.defaultValue = defaultValue;
        this.function = function;
    }

    public String print() {
        StringBuilder sb = new StringBuilder();
        if( comment != null ) sb.append( "COMMENT " ).append( comment ).append( '\n' );
        if( castType != null ) sb.append( "CAST " ).append( castType ).append( '\n' );
        if( defaultValue != null ) sb.append( "DEFAULT '" ).append( defaultValue ).append( "'\n" );
        sb.append( or.size() > 1 ? "OR\n" : "ROOT\n" );

        var it = or.iterator();
        while( it.hasNext() ) {
            var orItem = it.next();
            sb.append( it.hasNext() ? "├── " : "└── " ).append( orItem.print() );
        }

        if( function != null ) sb.append( "FUNCTION " ).append( function.print() ).append( '\n' );
        return sb.toString();
    }
}
