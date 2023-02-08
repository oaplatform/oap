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
import oap.template.LogConfiguration.FieldType;

@ToString( callSuper = true )
public class AstField extends Ast {
    final String fieldName;
    final boolean forceCast;
    final FieldType castType;

    public AstField( String fieldName, TemplateType fieldType, boolean forceCast, FieldType castType ) {
        super( fieldType );

        this.fieldName = fieldName;
        this.forceCast = forceCast;
        this.castType = castType;
    }

    @Override
    void render( Render render ) {
        if( castType != null ) {
            var targetType = type;
            if( type.isOptional() ) targetType = type.getActualTypeArguments0();

            if( !castType.isAssignableFrom( targetType ) ) {
                throw new ClassCastException( "fieldName '" + fieldName + "' path '" + render.content + "': current '" + type + "' required '" + castType + "'" );
            }
        }

        var variableName = render.newVariable();

        render.ntab()
            .append( "%s %s = ", type.getTypeName(), variableName );

        if( forceCast ) render.append( "( %s ) ", type.getTypeName() );

        render.append( "%s.%s;", render.field, fieldName );

        var newRender = render.withField( variableName ).withParentType( type );
        children.forEach( a -> a.render( newRender ) );
    }

    @Override
    protected boolean equalsAst( Ast ast ) {
        if( !( ast instanceof AstField ) ) return false;
        return ( ( AstField ) ast ).fieldName.equals( fieldName );
    }
}
