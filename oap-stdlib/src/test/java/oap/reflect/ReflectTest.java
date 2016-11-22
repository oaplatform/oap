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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.testng.AbstractTest;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ReflectTest extends AbstractTest {
    @Test
    public void newInstance() {
        Reflection ref = Reflect.reflect( "oap.reflect.Bean" );
        assertEquals( ref.newInstance(), new Bean( 10 ) );
        assertEquals( ref.newInstance( Maps.of( __( "i", 1 ) ) ), new Bean( 1 ) );
        assertEquals( ref.newInstance( Maps.of( __( "i", 1 ), __( "x", 2 ) ) ), new Bean( 1, 2 ) );
        assertEquals( ref.newInstance( Maps.of( __( "x", 2 ), __( "i", 1 ) ) ), new Bean( 1, 2 ) );
    }

    @Test
    public void newInstanceComplex() {
        Reflection ref = Reflect.reflect( "oap.reflect.Bean" );
        Bean expected = new Bean( 2, 1 );
        expected.str = "bbb";
        expected.l = Lists.of( "a", "b" );
        assertEquals( ref.newInstance( Maps.of(
            __( "x", 1 ),
            __( "i", 2L ),
            __( "str", "bbb" ),
            __( "l", Lists.of( "a", "b" ) )
        ) ), expected );
    }

    @Test
    public void fields() {
        Bean bean = new Bean( 10 );
        assertThat( Reflect.reflect( bean.getClass() ).fields.stream().<Object>map( f -> f.get( bean ) ) )
            .containsExactly( 10, 1, "aaa", null );
    }

    @Test
    public void reflectToString() {
        assertEquals( new Bean( 10 ).toString(), "Bean(i=10, x=1, str=aaa, l=null)" );
    }

    @Test
    public void assignableFrom() {
        assertTrue( Reflect.reflect( Bean.class )
            .field( "l" )
            .get()
            .type()
            .assignableFrom( List.class ) );
    }


    @Test
    public void annotation() {
        assertTrue( Reflect
            .reflect( Bean.class )
            .field( "x" )
            .map( f -> f.isAnnotatedWith( Ann.class ) )
            .orElse( false ) );
        assertEquals( Reflect
            .reflect( Bean.class )
            .field( "x" )
            .map( f -> f.annotationOf( Ann.class ).get( 0 ) )
            .get()
            .a(), 10 );
    }

    @Test
    public void typeRef() {
        Reflection reflection = Reflect.reflect( new TypeRef<List<Map<RetentionPolicy, List<Integer>>>>() {
        } );
        assertEquals( reflection.toString(),
            "Reflection(java.util.List<java.util.Map<java.lang.annotation.RetentionPolicy, java.util.List<java.lang.Integer>>>)" );
    }

    @Test
    public void getCollectionElementType() {
        assertEquals( Reflect.reflect( StringList.class ).getCollectionComponentType().underlying, String.class );
    }

    @Test
    public void eval() {
        assertEquals( ( int ) Reflect.<Integer>eval( new DeepBean(), "bean.x" ), 1 );
        assertEquals( Reflect.eval( new DeepBean(), "bean.str" ), "aaa" );
        assertEquals( Reflect.eval( new DeepBean(), "obean.str" ), "aaa" );
        assertNull( Reflect.eval( new DeepBean(), "emptybean.str" ) );
    }

    @Test
    public void constructor() {
        assertThatExceptionOfType( ReflectException.class )
            .isThrownBy( () -> Reflect.reflect( MatchingConstructor.class ).newInstance( Maps.empty() ) )
            .withMessage( "cannot find matching constructor: [] in class oap.reflect.MatchingConstructor candidates: [oap.reflect.MatchingConstructor(int i,java.util.List<java.lang.Integer> list), oap.reflect.MatchingConstructor(java.util.List<java.lang.Integer> list)]" );
    }

    @Test
    public void method() throws NoSuchMethodException {
        assertThat( Reflect.reflect( C.class )
            .method( I.class.getDeclaredMethod( "m", new Class[] { String.class } ) ) )
            .isNotNull();
    }
}

interface I {
    void m( String a );
}


class C implements I {
    public void m( String a ) {

    }
}

@Target( ElementType.FIELD )
@Retention( RetentionPolicy.RUNTIME )
@interface Ann {
    int a() default 1;
}

@EqualsAndHashCode
@ToString
class Bean {
    int i;
    @Ann( a = 10 )
    int x = 1;
    String str = "aaa";
    List<String> l;

//    CHECKSTYLE:OFF
    public Bean() {
        this( 10 );
    }

    public Bean( int i ) {
        this.i = i;
    }

    public Bean( int i, int x ) {
        this.i = i;
        this.x = x;
    }
//    CHECKSTYLE:ON
}


class StringList extends ArrayList<String> {

}

class DeepBean {
    public Bean bean = new Bean();
    public Optional<Bean> obean = Optional.of( new Bean() );
    public Optional<Bean> emptybean = Optional.empty();
}
//CHECKSTYLE:OFF
class MatchingConstructor {
    public MatchingConstructor( int i, List<Integer> list ) {}

    public MatchingConstructor( List<Integer> list ) {}
}
//CHECKSTYLE:ON
