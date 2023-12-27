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
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

@ToString
public class Render {
    private final AtomicInteger ids;
    public final String templateName;
    public final TemplateType parentType;
    public final TemplateAccumulator<?, ?, ?> templateAccumulator;
    public final String field;
    public final String templateAccumulatorName;
    public final int tab;
    private final StringBuilder sb;
    public final String content;
    public final String tryVariable;
    public final String prefix;

    private final ArrayDeque<HashSet<String>> variables;

    private Render( String templateName, String content, TemplateType parentType, TemplateAccumulator<?, ?, ?> templateAccumulator,
                    String field, String templateAccumulatorName, int tab, AtomicInteger ids, String tryVariable ) {
        this( new StringBuilder(), templateName, content, parentType, templateAccumulator, field, templateAccumulatorName, tab, ids, tryVariable,
            "", new ArrayDeque<>() {
                {
                    this.addFirst( new HashSet<>() );
                }
            } );
    }

    public Render( StringBuilder sb, String templateName, String content, TemplateType parentType, TemplateAccumulator<?, ?, ?> templateAccumulator,
                   String field, String templateAccumulatorName, int tab, AtomicInteger ids, String tryVariable,
                   String prefix,
                   ArrayDeque<HashSet<String>> variables ) {
        this.sb = sb;
        this.templateName = templateName;
        this.content = content;
        this.parentType = parentType;
        this.templateAccumulator = templateAccumulator;
        this.field = field;
        this.templateAccumulatorName = templateAccumulatorName;
        this.tab = tab;
        this.ids = ids;
        this.tryVariable = tryVariable;
        this.prefix = prefix;

        this.variables = variables;
    }

    public static Render init( String templateName, String content, TemplateType type, TemplateAccumulator<?, ?, ?> acc ) {
        return new Render( templateName, content, type, acc, null, null, 0, new AtomicInteger(), null );
    }

    public Render withField( String field ) {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, field,
            this.templateAccumulatorName, this.tab, ids, tryVariable, variableNameWithPrefix( field ), variables );
    }

    public Render withContent( String content ) {
        return new Render( this.sb, this.templateName, content, this.parentType, this.templateAccumulator, field,
            this.templateAccumulatorName, this.tab, ids, tryVariable, prefix, variables );
    }

    public Render withTemplateAccumulatorName( String templateAccumulatorName ) {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            templateAccumulatorName, this.tab, ids, tryVariable, prefix, variables );
    }

    public Render tabInc() {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab + 1, ids, tryVariable, prefix, variables );
    }

    public Render tabDec() {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab - 1, ids, tryVariable, prefix, variables );
    }

    public Render withParentType( TemplateType parentType ) {
        return new Render( this.sb, this.templateName, this.content, parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab, ids, tryVariable, prefix, variables );
    }

    public Render n() {
        sb.append( '\n' );
        return this;
    }

    public Render ntab() {
        return this.n().tab();
    }

    public Render tab() {
        sb.append( "  ".repeat( tab ) );

        return this;
    }

    public Render append( String str ) {
        sb.append( str );

        return this;
    }

    public Render append( String format, Object... args ) {
        sb.append( String.format( format, args ) );

        return this;
    }

    public Render append( char ch ) {
        sb.append( ch );

        return this;
    }

    public String out() {
        return sb.toString();
    }

    public String nameEscaped() {
        var nameEscaped = templateName.replaceAll( "[^a-zA-Z0-9_]", "_" );
        if( nameEscaped.matches( "^[0-9].*" ) ) nameEscaped = "_" + nameEscaped;
        return nameEscaped;
    }

    public String escapeJava( String text ) {
        return StringEscapeUtils.escapeJava( text );
    }

    public String newVariable() {
        var id = ids.incrementAndGet();
        return "v" + id;
    }

    public NewVariable newVariable( String name ) {
        var fullName = variableNameWithPrefix( name );
        var it = variables.descendingIterator();

        while( it.hasNext() ) {
            var map = it.next();

            if( map.contains( fullName ) ) return new NewVariable( fullName, false );
        }

        variables.getFirst().add( fullName );
        return new NewVariable( fullName, true );
    }

    public Render withTryVariable( String tryVariable ) {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab, ids, tryVariable, prefix, variables );
    }

    public Render newBlock() {

        var newStack = new ArrayDeque<HashSet<String>>();

        for( var item : variables ) newStack.addLast( new HashSet<>( item ) );
        newStack.addFirst( new HashSet<>() );

        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab, ids, tryVariable, prefix, newStack );
    }

    @ToString
    public static class NewVariable {
        public final String name;
        public final boolean isNew;

        public NewVariable( String name, boolean isNew ) {
            this.name = name;
            this.isNew = isNew;
        }
    }

    private String variableNameWithPrefix( String name ) {
        return prefix.isEmpty() ? name : prefix + "_" + name;
    }
}
