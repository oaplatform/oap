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

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@ToString( of = { "type" } )
public abstract class AstRender {
    public final TemplateType type;
    public final ArrayList<AstRender> children = new ArrayList<>();

    public AstRender( TemplateType type ) {
        this.type = type;
    }

    public abstract void render( Render render );

    public void addChild( AstRender astRender ) {
        children.add( astRender );
    }

    public String print() {
        var buffer = new StringBuilder();
        print( buffer, "", "" );
        return buffer.toString();
    }

    public void print( StringBuilder buffer, String prefix, String childrenPrefix ) {
        print( buffer, prefix, childrenPrefix, children );
    }

    public void print( StringBuilder buffer, String prefix, String childrenPrefix, List<AstRender> children ) {
        printTop( buffer, prefix );
        printChildren( buffer, childrenPrefix, children );
    }

    public void printChildren( StringBuilder buffer, String childrenPrefix, List<AstRender> children ) {
        for( var it = children.iterator(); it.hasNext(); ) {
            AstRender next = it.next();
            if( it.hasNext() ) {
                next.print( buffer, childrenPrefix + "├── ", childrenPrefix + "│   " );
            } else {
                next.print( buffer, childrenPrefix + "└── ", childrenPrefix + "    " );
            }
        }
    }

    public void printTop( StringBuilder buffer, String prefix ) {
        buffer.append( prefix );
        buffer.append( this );
        buffer.append( '\n' );
    }

}
