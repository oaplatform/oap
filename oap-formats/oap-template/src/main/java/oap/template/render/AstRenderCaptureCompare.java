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
import oap.template.TemplateConditionHelper;
import oap.template.TemplateException;
import oap.template.runtime.RuntimeContext;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;
import java.util.Map;

/**
 * Leaf node that compares the current field value against a literal using a comparison operator.
 * Operators: == (eq), != (ne), >, <, >=, <=, eqi (case-insensitive String equals), contains (List/Map).
 */
@ToString( callSuper = true )
class AstRenderCaptureCompare extends AstRender {
    private final String op;
    private final String literal;

    AstRenderCaptureCompare( TemplateType type, String op, String literal ) {
        super( type );
        this.op = op;
        this.literal = literal;
    }

    @Override
    public void render( Render render ) {
        String normOp = normalizeOp( op );
        Class<?> tc = type.getTypeClass();
        String field = render.field;
        String boolVar = render.booleanIfVar;

        switch( normOp ) {
            case "==" -> {
                String javaLiteral = javaLiteralFor( type, literal );
                if( tc.isPrimitive() ) {
                    render.ntab().append( "%s = %s == %s;", boolVar, field, javaLiteral );
                } else {
                    render.ntab().append( "%s = java.util.Objects.equals( %s, %s );", boolVar, field, javaLiteral );
                }
            }
            case "!=" -> {
                String javaLiteral = javaLiteralFor( type, literal );
                if( tc.isPrimitive() ) {
                    render.ntab().append( "%s = %s != %s;", boolVar, field, javaLiteral );
                } else {
                    render.ntab().append( "%s = !java.util.Objects.equals( %s, %s );", boolVar, field, javaLiteral );
                }
            }
            case ">", "<", ">=", "<=" -> {
                String javaLiteral = javaLiteralFor( type, literal );
                if( tc == String.class ) {
                    render.ntab().append( "%s = %s.compareTo( %s ) %s 0;", boolVar, field, javaLiteral, normOp );
                } else {
                    render.ntab().append( "%s = %s %s %s;", boolVar, field, normOp, javaLiteral );
                }
            }
            case "eqi" -> {
                if( tc != String.class ) {
                    throw new TemplateException( "eqi operator requires String field, got: " + tc.getName() );
                }
                String javaLiteral = javaLiteralFor( type, literal );
                render.ntab().append( "%s = %s.equalsIgnoreCase( %s );", boolVar, field, javaLiteral );
            }
            case "contains" -> {
                if( List.class.isAssignableFrom( tc ) ) {
                    TemplateType elementType = type.getActualTypeArguments0();
                    String javaLiteral = javaLiteralFor( elementType, literal );
                    render.ntab().append( "%s = %s.contains( %s );", boolVar, field, javaLiteral );
                } else if( Map.class.isAssignableFrom( tc ) ) {
                    TemplateType keyType = type.getActualTypeArguments0();
                    String javaLiteral = javaLiteralFor( keyType, literal );
                    render.ntab().append( "%s = %s.containsKey( %s );", boolVar, field, javaLiteral );
                } else {
                    throw new TemplateException( "contains operator requires List or Map field, got: " + tc.getName() );
                }
            }
            default -> throw new TemplateException( "Unknown comparison operator: " + op );
        }
    }

    @Override
    public void interpret( RuntimeContext ctx ) {
        if( ctx.booleanCapture != null ) {
            ctx.booleanCapture[0] = TemplateConditionHelper.compare( ctx.currentObject, op, literal );
        }
    }

    private static String normalizeOp( String op ) {
        return switch( op ) {
            case "eq" -> "==";
            case "ne" -> "!=";
            default -> op;
        };
    }

    static String javaLiteralFor( TemplateType type, String literal ) {
        Class<?> tc = type.getTypeClass();
        if( tc == String.class ) return "\"" + StringEscapeUtils.escapeJava( literal ) + "\"";
        if( tc == long.class || tc == Long.class ) return literal + "L";
        if( tc == float.class || tc == Float.class ) return literal + "f";
        return literal;
    }
}
