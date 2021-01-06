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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@ToString
class TemplateType {
    public final Type type;
    public final boolean nullable;

    public TemplateType( Type type, boolean nullable ) {
        this.type = type;
        this.nullable = nullable;
    }

    public TemplateType( Type type ) {
        this( type, false );
    }

    /**
     * this doesnt work with TypeVariables
     * todo rewrite on TypeRef based type resolution
     */
    static Class<?> getTypeClass( Type type ) {
        if( type instanceof ParameterizedType ) return getTypeClass( ( ( ParameterizedType ) type ).getRawType() );
        return ( Class<?> ) type;
    }

    String getTypeName() {
        return type.getTypeName().replace( '$', '.' );
    }

    public boolean isInstanceOf( Class<?> clazz ) {
        return clazz.isAssignableFrom( getTypeClass( type ) );
    }

    Class<?> getTypeClass() {
        return getTypeClass( type );
    }

    public TemplateType getActualTypeArguments0() {
        return new TemplateType( ( ( ParameterizedType ) type ).getActualTypeArguments()[0] );
    }

    public TemplateType getActualTypeArguments1() {
        return new TemplateType( ( ( ParameterizedType ) type ).getActualTypeArguments()[1] );
    }

    public boolean isPrimitiveType() {
        return getTypeClass().isPrimitive();
    }
}
