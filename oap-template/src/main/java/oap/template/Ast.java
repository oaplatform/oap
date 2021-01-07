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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@ToString( of = { "type" } )
public abstract class Ast {
    private static final HashMap<Class<?>, AtomicInteger> ids = new HashMap<>();
    public final TemplateType type;
    final ArrayList<Ast> children = new ArrayList<>();

    Ast( TemplateType type ) {
        this.type = type;
    }

    abstract void render( Render render );

    public void addChildren( List<? extends Ast> list ) {
        children.addAll( list );
    }

    public Ast addChild( Ast ast ) {
        children.add( ast );

        return this;
    }

    String newVariable() {
        var id = ids.computeIfAbsent( getClass(), c -> new AtomicInteger() ).incrementAndGet();
        return getClass().getSimpleName().toLowerCase() + id;
    }

    public String print() {
        var buffer = new StringBuilder();
        print( buffer, "", "" );
        return buffer.toString();
    }

    protected void print( StringBuilder buffer, String prefix, String childrenPrefix ) {
        print( buffer, prefix, childrenPrefix, children );
    }

    protected void print( StringBuilder buffer, String prefix, String childrenPrefix, List<Ast> children ) {
        printTop( buffer, prefix );
        printChildren( buffer, childrenPrefix, children );
    }

    protected void printChildren( StringBuilder buffer, String childrenPrefix, List<Ast> children ) {
        for( var it = children.iterator(); it.hasNext(); ) {
            Ast next = it.next();
            if( it.hasNext() ) {
                next.print( buffer, childrenPrefix + "├── ", childrenPrefix + "│   " );
            } else {
                next.print( buffer, childrenPrefix + "└── ", childrenPrefix + "    " );
            }
        }
    }

    protected void printTop( StringBuilder buffer, String prefix ) {
        buffer.append( prefix );
        buffer.append( toString() );
        buffer.append( '\n' );
    }

    protected boolean equalsAst( Ast ast ) {
        return false;
    }
}
