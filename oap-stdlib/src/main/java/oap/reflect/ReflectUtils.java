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
package oap.reflect;

import oap.util.Arrays;
import oap.util.Stream;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.sort;
import static oap.util.Arrays.map;

class ReflectUtils {

    static <T extends Executable> Optional<T> findExecutableByParamNames( String[] names, T[] executables ) {
        sort( names );
        return Arrays.find(
                x -> {
                    String[] paramNames = map( String.class, Parameter::getName, x.getParameters() );
                    sort( paramNames );
                    return java.util.Arrays.equals( names, paramNames );
                },
                executables );
    }

    static <T extends Executable> Optional<T> findExecutableByParamTypes( Class<?>[] types, T[] executables ) {
        return Arrays.find(
                x -> {
                    Class<?>[] paramTypes = map( Class.class, Parameter::getType, x.getParameters() );
                    if( types.length != paramTypes.length ) return false;
                    for( int i = 0; i < types.length; i++ )
                        if( !paramTypes[i].isAssignableFrom( types[i] ) ) return false;
                    return true;
                }, executables );
    }

    static <A> List<A> declared( Class<?> clazz, Function<Class<?>, A[]> collector ) {
        return Stream.<Class<?>>traverse( clazz, Class::getSuperclass )
                .flatMap( c -> Stream.of( collector.apply( c ) ) )
                .toList();
    }
}
