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
import org.apache.commons.text.StringEscapeUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by igor.petrenko on 2020-07-13.
 */
@ToString
class Render {
    final String templateName;
    final TemplateType parentType;
    final TemplateAccumulator<?, ?> templateAccumulator;
    final String field;
    final String templateAccumulatorName;
    final int tab;
    private final StringBuilder sb;

    private Render( String templateName, TemplateType parentType, TemplateAccumulator<?, ?> templateAccumulator,
                    String field, String templateAccumulatorName, int tab ) {
        this( new StringBuilder(), templateName, parentType, templateAccumulator, field, templateAccumulatorName, tab );
    }

    public Render( StringBuilder sb, String templateName, TemplateType parentType, TemplateAccumulator<?, ?> templateAccumulator,
                   String field, String templateAccumulatorName, int tab ) {
        this.sb = sb;
        this.templateName = templateName;
        this.parentType = parentType;
        this.templateAccumulator = templateAccumulator;
        this.field = field;
        this.templateAccumulatorName = templateAccumulatorName;
        this.tab = tab;
    }

    public static Render init( String templateName, TemplateType type, TemplateAccumulator<?, ?> acc ) {
        return new Render( templateName, type, acc, null, null, 0 );
    }

    public Render withField( String field ) {
        return new Render( this.sb, this.templateName, this.parentType, this.templateAccumulator, field, this.templateAccumulatorName, this.tab );
    }

    public Render withTemplateAccumulatorName( String templateAccumulatorName ) {
        return new Render( this.sb, this.templateName, this.parentType, this.templateAccumulator, this.field, templateAccumulatorName, this.tab );
    }

    public Render tabInc() {
        return new Render( this.sb, this.templateName, this.parentType, this.templateAccumulator, this.field, this.templateAccumulatorName, this.tab + 1 );
    }

    public Render tabDec() {
        return new Render( this.sb, this.templateName, this.parentType, this.templateAccumulator, this.field, this.templateAccumulatorName, this.tab - 1 );
    }

    public Render withParentType( TemplateType parentType ) {
        return new Render( this.sb, this.templateName, parentType, this.templateAccumulator, this.field, this.templateAccumulatorName, this.tab );
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

    public Class<?> getClass( Type type ) {
        if( type instanceof ParameterizedType ) return getClass( ( ( ParameterizedType ) type ).getRawType() );
        return ( Class<?> ) type;
    }

    public String nameEscaped() {
        return templateName.replaceAll( "[\\s%\\-;\\\\/:*?\"<>|]", "_" );
    }

    public String escapeJava( String text ) {
        return StringEscapeUtils.escapeJava( text );
    }
}
