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
import oap.util.Dates;
import oap.util.Strings;
import org.apache.commons.lang3.EnumUtils;
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

        String cast = "";
        if( castType != null ) cast = "(" + castType.type.getTypeName() + ")";

        r.append( "%s.accept( %s( %s ) );", r.templateAccumulatorName, cast, format( type, value ) );
    }

    @SuppressWarnings( { "checkstyle:ParameterAssignment", "unchecked" } )
    private String format( TemplateType parentType, String defaultValue ) {
        Class<?> typeClass = parentType.isOptional() ? parentType.getActualTypeArguments0().getTypeClass() : parentType.getTypeClass();

        if( defaultValue == null ) {
            if( String.class.equals( typeClass ) ) defaultValue = "";
            else if( Boolean.class.equals( typeClass ) ) defaultValue = "false";
            else if( Collection.class.isAssignableFrom( typeClass ) ) defaultValue = "[]";
            else if( Enum.class.isAssignableFrom( typeClass ) ) {
                try {
                    defaultValue = Enum.valueOf( ( Class<Enum> ) typeClass, Strings.UNKNOWN ).name();
                } catch( IllegalArgumentException ignored ) {
                    defaultValue = EnumUtils.getEnumList( ( Class<Enum> ) typeClass ).get( 0 ).toString();
                }
            } else if( typeClass.isPrimitive() ) defaultValue = "0";
        }

        if( String.class.equals( typeClass ) ) return "\"" + StringUtils.replace( defaultValue, "\"", "\\\"" ) + "\"";
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
