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

import com.google.common.primitives.Primitives;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Lists;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ToString
@EqualsAndHashCode
class FieldType {
    final Class<?> type;
    final ArrayList<FieldType> parameters = new ArrayList<>();

    FieldType( Class<?> type ) {
        this.type = type;
    }

    FieldType( Class<?> type, Collection<FieldType> parameters ) {
        this.type = type;
        this.parameters.addAll( parameters );
    }

    static FieldType parse( String type ) throws ClassNotFoundException {
        var generics = new ArrayList<FieldType>();
        String baseType = type;

        var genericIndex = type.indexOf( '<' );
        if( genericIndex > 0 ) {
            baseType = type.substring( 0, genericIndex );
            var endGenericIndex = type.lastIndexOf( '>' );
            var genericString = type.substring( genericIndex + 1, endGenericIndex );

            int lastIndex = 0;
            while( true ) {
                int ch = genericString.indexOf( ',', lastIndex );
                if( ch < 0 ) break;

                String g = genericString.substring( lastIndex, ch );
                lastIndex = ch + 1;

                generics.add( parse( g ) );
            }

            generics.add( parse( genericString.substring( lastIndex ) ) );
        }
        return new FieldType( Class.forName( baseType ), generics );
    }

    boolean isAssignableFrom( TemplateType templateType ) {
        if( !type.isAssignableFrom( Primitives.wrap( templateType.getTypeClass() ) ) ) return false;

        if( !parameters.isEmpty() || templateType.type instanceof ParameterizedType ) {
            if( !parameters.isEmpty() && !( templateType.type instanceof ParameterizedType ) ) return false;

            List<TemplateType> actualArguments = templateType.getActualArguments();
            if( actualArguments.size() != parameters.size() ) return false;

            for( int i = 0; i < parameters.size(); i++ ) {
                if( !parameters.get( 0 ).isAssignableFrom( actualArguments.get( i ) ) ) return false;
            }
        }

        return true;
    }

    String print() {
        return type.getTypeName() + ( parameters.isEmpty() ? "" : "<" + String.join( ",", Lists.map( parameters, FieldType::print ) ) + ">" );
    }
}
