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

import oap.util.Lists;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;

import static oap.util.Lists.concat;
import static org.assertj.core.api.Assertions.assertThat;

public class ReflectTest {

    @Test
    public void declaredMethods() {
        assertThat( Reflection.declared( B.class, Class::getDeclaredMethods ) )
            .containsAll( concat(
                Lists.of( Object.class.getDeclaredMethods() ),
                Lists.of( A.class.getDeclaredMethods() ),
                Lists.of( B.class.getDeclaredMethods() ),
                Lists.of( II.class.getDeclaredMethods() )
            ) );
    }

    @Test
    public void declaredFields() {
        assertThat( Reflection.declared( B.class, Class::getDeclaredFields ) )
            .containsAll( concat(
                Lists.of( Object.class.getDeclaredFields() ),
                Lists.of( A.class.getDeclaredFields() ),
                Lists.of( B.class.getDeclaredFields() ),
                Lists.of( II.class.getDeclaredFields() )
            ) );
    }

    @Test
    public void invokeOverloads() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        II.class.getDeclaredMethod( "i" ).invoke( new B() );
        assertThat( B.iCalled ).isEqualTo( 1 );
        B.class.getDeclaredMethod( "i" ).invoke( new B() );
        assertThat( B.iCalled ).isEqualTo( 2 );

        II.class.getDeclaredMethod( "o" ).invoke( new B() );
        assertThat( B.oCalled ).isEqualTo( 1 );
        B.class.getDeclaredMethod( "o" ).invoke( new B() );
        assertThat( B.oCalled ).isEqualTo( 2 );
    }
}

interface II {
    default void d() {
    }

    void i();

    Object o();

}

class A {
    void a() {
    }
}

class B extends A implements II {
    static int iCalled = 0;
    static int oCalled = 0;

    void b() {
    }

    @Override
    public void i() {
        iCalled++;
    }

    public String o() {
        oCalled++;
        return null;
    }
}
