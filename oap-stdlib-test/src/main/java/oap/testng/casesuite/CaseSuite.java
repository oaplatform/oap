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

package oap.testng.casesuite;

import com.google.common.reflect.ClassPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.reflect.Reflect;
import oap.util.Stream;

import java.util.Optional;

@Slf4j
public class CaseSuite {

    @SneakyThrows
    @SuppressWarnings( "UnstableApiUsage" )
    public static Object[][] casesOf( Object self, Class<?> masterclass ) {
        if( self.getClass().equals( masterclass ) ) return ClassPath.from( Thread.currentThread().getContextClassLoader() )
            .getAllClasses()
            .stream()
            .<Optional<Class<?>>>map( ci -> {
                try {
                    return Optional.of( ci.load() );
                } catch( Throwable e ) {
                    return Optional.empty();
                }
            } )
            .filter( Optional::isPresent )
            .map( Optional::get )
            .filter( c -> masterclass.isAssignableFrom( c ) && masterclass != c )
            .flatMap( c -> Stream.of( casesOf( c, Reflect.reflect( c ).<Object>newInstance() ) ) )
            .toArray( size -> new Object[size][0] );
        else {
            return casesOf( self.getClass(), self );
        }
    }

    private static Object[][] casesOf( Class<?> clazz, Object instance ) {
        Object[][] caseList = Reflect.reflect( clazz )
            .annotatedMethods( CaseProvider.class )
            .stream()
            .flatMap( m -> Stream.of( ( Object[][] ) m.invoke( instance ) ) )
            .map( a -> {
                Object[] aCase = new Object[a.length + 1];
                aCase[0] = new CaseContext( clazz, a );
                System.arraycopy( a, 0, aCase, 1, a.length );
                return aCase;
            } )
            .toArray( size -> new Object[size][0] );
        log.trace( "found case class {} with {} case(s)", clazz.getSimpleName(), caseList.length );
        return caseList;
    }

    public static Object[] thecase( Object... values ) {
        return values;
    }

}
