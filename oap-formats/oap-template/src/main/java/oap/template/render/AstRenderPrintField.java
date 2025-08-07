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

import com.google.common.base.Preconditions;
import lombok.ToString;

import javax.annotation.Nullable;

@ToString( callSuper = true )
public class AstRenderPrintField extends AstRender {
    @Nullable
    private final FieldType castType;

    public AstRenderPrintField( TemplateType type, @Nullable FieldType castType ) {
        super( type );
        this.castType = castType;
    }

    @Override
    public void render( Render render ) {
        Render r = render.ntab();
        r.append( "%s.accept( %s );", r.templateAccumulatorName, format( castType != null ? new TemplateType( castType.type ) : type, r.field ) );
    }

    @SuppressWarnings( { "checkstyle:ParameterAssignment" } )
    private String format( TemplateType castType, String value ) {
        Preconditions.checkNotNull( value );

        if( castType == null || type.equals( castType ) ) {
            return value;
        }

        Class<?> typeClass = castType.isOptional() ? castType.getActualTypeArguments0().getTypeClass() : castType.getTypeClass();

        if( byte.class.equals( typeClass ) ) return "(byte)%s".formatted( value );
        if( Byte.class.isAssignableFrom( typeClass ) ) return "( ( Number ) %s ).byteValue()".formatted( value );
        else if( short.class.equals( typeClass ) ) return "(short)%s".formatted( value );
        else if( Short.class.isAssignableFrom( typeClass ) ) return "( ( Number ) %s ).shortValue()".formatted( value );
        else if( int.class.equals( typeClass ) ) return "(int)%s".formatted( value );
        else if( Integer.class.isAssignableFrom( typeClass ) ) return "( ( Number ) %s ).intValue()".formatted( value );
        else if( long.class.equals( typeClass ) ) return "(long)%s".formatted( value );
        else if( Long.class.isAssignableFrom( typeClass ) ) return "( ( Number ) %s ).longValue()".formatted( value );
        else if( float.class.equals( typeClass ) ) return "(float)%s".formatted( value );
        else if( Float.class.isAssignableFrom( typeClass ) ) return "( ( Number ) %s ).floatValue()".formatted( value );
        else if( double.class.equals( typeClass ) ) return "(double)%s".formatted( value );
        else if( Double.class.isAssignableFrom( typeClass ) ) return "( ( Number ) %s ).doubleValue()".formatted( value );

        return value;
    }
}
