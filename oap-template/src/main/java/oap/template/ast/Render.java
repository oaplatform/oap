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

package oap.template.ast;

import lombok.ToString;
import oap.template.TemplateAccumulator;
import org.apache.commons.text.StringEscapeUtils;

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

    private Render( String templateName, String content, TemplateType parentType, TemplateAccumulator<?, ?, ?> templateAccumulator,
                    String field, String templateAccumulatorName, int tab, AtomicInteger ids, String tryVariable ) {
        this( new StringBuilder(), templateName, content, parentType, templateAccumulator, field, templateAccumulatorName, tab, ids, tryVariable );
    }

    public Render( StringBuilder sb, String templateName, String content, TemplateType parentType, TemplateAccumulator<?, ?, ?> templateAccumulator,
            String field, String templateAccumulatorName, int tab, AtomicInteger ids, String tryVariable ) {
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
    }

    public static Render init( String templateName, String content, TemplateType type, TemplateAccumulator<?, ?, ?> acc ) {
        return new Render( templateName, content, type, acc, null, null, 0, new AtomicInteger(), null );
    }

    public Render withField( String field ) {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, field,
            this.templateAccumulatorName, this.tab, ids, tryVariable );
    }

    public Render withContent( String content ) {
        return new Render( this.sb, this.templateName, content, this.parentType, this.templateAccumulator, field,
            this.templateAccumulatorName, this.tab, ids, tryVariable );
    }

    public Render withTemplateAccumulatorName( String templateAccumulatorName ) {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            templateAccumulatorName, this.tab, ids, tryVariable );
    }

    public Render tabInc() {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab + 1, ids, tryVariable );
    }

    public Render tabDec() {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab - 1, ids, tryVariable );
    }

    public Render withParentType( TemplateType parentType ) {
        return new Render( this.sb, this.templateName, this.content, parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab, ids, tryVariable );
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

    public Render withTryVariable( String tryVariable ) {
        return new Render( this.sb, this.templateName, this.content, this.parentType, this.templateAccumulator, this.field,
            this.templateAccumulatorName, this.tab, ids, tryVariable );
    }
}
