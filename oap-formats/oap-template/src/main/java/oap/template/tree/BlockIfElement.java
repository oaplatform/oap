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

import javax.annotation.Nullable;

@ToString
public class BlockIfElement implements Element {
    public final String conditionPath;
    public final Elements thenElements;
    @Nullable
    public final Elements elseElements;
    public final boolean trimLeft;

    public BlockIfElement( String conditionPath, Elements thenElements, @Nullable Elements elseElements ) {
        this( conditionPath, thenElements, elseElements, false );
    }

    public BlockIfElement( String conditionPath, Elements thenElements, @Nullable Elements elseElements, boolean trimLeft ) {
        this.conditionPath = conditionPath;
        this.thenElements = thenElements;
        this.elseElements = elseElements;
        this.trimLeft = trimLeft;
    }

    @Override
    public void print( ToStringRender render ) {
        render.append( "IF" ).append( trimLeft ? " [LTRIM]" : "" ).append( ' ' ).append( conditionPath ).nspace();
        render.append( elseElements != null ? "├── " : "└── " );
        thenElements.print( render.spaceInc( 4 ) );

        if( elseElements != null ) {
            render.nspace();
            render.append( "└── ELSE " );
            elseElements.print( render.spaceInc( 9 ) );
        }
    }
}
