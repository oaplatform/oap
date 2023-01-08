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

import com.google.common.base.Preconditions;
import lombok.ToString;
import oap.util.Dates;
import oap.util.Strings;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.Collection;

@ToString( callSuper = true )
public class AstPrintValue extends Ast {
    private final String value;
    private final FieldType castType;

    public AstPrintValue( TemplateType type, String value, FieldType castType ) {
        super( type );
        this.value = value;
        this.castType = castType;
    }

    @Override
    public void render( Render render ) {
        var r = render.ntab();

        var defaultValue = value;

        if( defaultValue == null ) defaultValue = r.templateAccumulator.getDefault( type.getTypeClass() );

        if( defaultValue == null ) {
            r.append( "%s.acceptNull( %s.class );", r.templateAccumulatorName, type.getTypeClass().getSimpleName() );
        } else {
            String cast = "";
            if( castType != null ) cast = "(" + castType.type.getTypeName() + ")";

            r.append( "%s.accept( %s( %s ) );", r.templateAccumulatorName, cast, format( type, defaultValue ) );
        }
    }

    @SuppressWarnings( { "checkstyle:ParameterAssignment" } )
    private String format( TemplateType parentType, String defaultValue ) {
        Preconditions.checkNotNull( defaultValue );

        Class<?> typeClass = parentType.isOptional() ? parentType.getActualTypeArguments0().getTypeClass() : parentType.getTypeClass();

        if( String.class.equals( typeClass ) ) return "\"" + StringUtils.replace( defaultValue, "\"", "\\\"" ) + "\"";
        else if( Byte.class.isAssignableFrom( typeClass ) || byte.class.equals( typeClass ) ) return "(byte)%s".formatted( defaultValue );
        else if( Short.class.isAssignableFrom( typeClass ) || short.class.equals( typeClass ) ) return "(short)%s".formatted( defaultValue );
        else if( Long.class.isAssignableFrom( typeClass ) || long.class.equals( typeClass ) ) return "%sL".formatted( defaultValue );
        else if( Float.class.isAssignableFrom( typeClass ) || float.class.equals( typeClass ) ) return "%sf".formatted( defaultValue );
        else if( Double.class.isAssignableFrom( typeClass ) || double.class.equals( typeClass ) ) return "%sd".formatted( defaultValue );
        else if( Collection.class.isAssignableFrom( typeClass ) ) {
            return "java.util.List.of()";
        } else if( Enum.class.isAssignableFrom( typeClass ) ) {
            return "%s.%s".formatted( parentType.getTypeName(), defaultValue.isEmpty() ? Strings.UNKNOWN : defaultValue );
        } else if( DateTime.class.equals( typeClass ) ) {
            DateTime dateTime = Dates.PARSER_MULTIPLE_DATETIME.parseDateTime( defaultValue );
            return "new org.joda.time.DateTime( " + dateTime.getMillis() + "L, org.joda.time.DateTimeZone.UTC )";
        }
        return defaultValue;
    }
}
