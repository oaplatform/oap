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
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static oap.testng.Asserts.assertString;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class ReflectionTest {
    @Test
    public void newInstance() {
        Reflection ref = Reflect.reflect( "oap.reflect.Bean" );
        assertThat( ref.<Bean>newInstance() ).isEqualTo( new Bean( 10 ) );
        assertThat( ref.<Bean>newInstance( Maps.of( __( "i", 1 ) ) ) )
            .isEqualTo( new Bean( 1 ) );
        assertThat( ref.<Bean>newInstance( Maps.of( __( "i", 1 ), __( "x", 2 ) ) ) )
            .isEqualTo( new Bean( 1, 2 ) );
        assertThat( ref.<Bean>newInstance( Maps.of( __( "x", 2 ), __( "i", 1 ) ) ) )
            .isEqualTo( new Bean( 1, 2 ) );
        assertThat( ref.<Bean>newInstance( Maps.of(
            __( "list", Lists.of(
                Maps.of(
                    __( "i", 1 ),
                    __( "list", Lists.of(
                        Maps.of( __( "i", 5 ), __( "x", 6 ) )
                    ) ),
                    __( "map", Maps.of(
                        __( "b", Maps.of( __( "i", 7 ), __( "x", 8 ) ) )
                    ) )
                ),
                Maps.of( __( "i", 3 ), __( "x", 4 ) ) )
            ),
            __( "map", Maps.of(
                __( "a", Maps.of( __( "i", 9 ), __( "x", 10 ) ) )
            ) ),
            __( "i", 1 ) ) ) )
            .isEqualTo( new Bean( 1, Lists.of(
                new Bean( 1, Lists.of( new Bean( 5, 6 ) ), Maps.of( __( "b", new Bean( 7, 8 ) ) ) ),
                new Bean( 3, 4 )
            ), Maps.of( __( "a", new Bean( 9, 10 ) ) ) ) );
    }

    @Test
    public void newInstanceComplex() {
        Reflection ref = Reflect.reflect( "oap.reflect.Bean" );
        Bean expected = new Bean( 2, 1 );
        expected.str = "bbb";
        expected.l = Lists.of( "a", "b" );
        assertThat( ref.<Bean>newInstance( Maps.of(
            __( "x", 1 ),
            __( "i", 2L ),
            __( "str", "bbb" ),
            __( "l", Lists.of( "a", "b" ) )
        ) ) ).isEqualTo( expected );
    }

    @Test
    public void fields() {
        Bean bean = new Bean( 10 );
        assertThat( Reflect.reflect( bean.getClass() ).fields.values().stream().map( f -> f.get( bean ) ) )
            .containsExactly( 10, 1, "aaa", null, Optional.empty(), null, null );
    }

    @Test
    public void assignableFrom() {
        assertThat( Reflect.reflect( Bean.class )
            .field( "l" )
            .orElseThrow()
            .type()
            .assignableFrom( List.class ) ).isTrue();
    }


    @Test
    public void annotation() {
        assertThat( Reflect
            .reflect( Bean.class )
            .field( "x" )
            .orElseThrow()
            .isAnnotatedWith( Ann.class ) )
            .isTrue();
        assertThat( Reflect
            .reflect( Bean.class )
            .field( "x" )
            .orElseThrow()
            .annotationOf( Ann.class ).get( 0 )
            .a() ).isEqualTo( 10 );
    }

    @Test
    public void typeRef() {
        Reflection reflection = Reflect.reflect( new TypeRef<List<Map<RetentionPolicy, List<Integer>>>>() {
        } );
        assertString( reflection.toString() ).isEqualTo(
            "Reflection(java.util.List<java.util.Map<java.lang.annotation.RetentionPolicy, java.util.List<java.lang.Integer>>>)" );
    }

    @Test
    public void getCollectionElementType() {
        assertThat( Reflect.reflect( StringList.class ).getCollectionComponentType().underlying ).isEqualTo( String.class );
    }

    @Test
    public void get() {
        Bean bean = new Bean( 1, "bbb" );
        DeepBean deepBean = new DeepBean( bean, Optional.of( bean ), Lists.of( bean ), Maps.of(
            __( "x", Maps.of(
                __( "1", 1 ),
                __( "2", 2 )
            ) )
        ) );
        assertThat( Reflect.<Integer>get( deepBean, "bean.x" ) ).isEqualTo( 1 );
        assertString( Reflect.<String>get( deepBean, "bean.str" ) ).isEqualTo( "bbb" );
        assertString( Reflect.<String>get( deepBean, "beanOptional.str" ) ).isEqualTo( "bbb" );
        assertThat( Reflect.<Bean>get( deepBean, "list.[0]" ) ).isEqualTo( bean );
        assertThat( Reflect.<Bean>get( deepBean, "list.[2]" ) ).isNull();
        assertThat( Reflect.<Integer>get( deepBean, "map.[x].[1]" ) ).isEqualTo( 1 );
        assertThat( Reflect.<Integer>get( deepBean, "map.[x].[2]" ) ).isEqualTo( 2 );
        assertThat( Reflect.<Integer>get( deepBean, "map.[x].[3]" ) ).isNull();
        assertThat( Reflect.<Integer>get( deepBean, "map.[z]" ) ).isNull();
        assertThat( Reflect.<String>get( new DeepBean( new Bean(), Optional.empty() ), "beenOptional.str" ) )
            .isNull();
    }

    @Test
    public void set() {
        DeepBean deepBean = new DeepBean( new Bean( 10, "aaa" ), Optional.empty() );

        Reflect.set( deepBean, "bean.str", "new string" );
        Reflect.set( deepBean, "bean.x.y.z", "anything" );
        Reflect.set( deepBean, "list.[0]", new Bean( 10, "aaa" ) );
        Reflect.set( deepBean, "list.[1]", new Bean( 11, "bbb" ) );
        Reflect.set( deepBean, "list.[*]", new Bean( 12, "ccc" ) );
        Reflect.set( deepBean, "map.[x]", Maps.of() );
        Reflect.set( deepBean, "map.[x].[1]", 1 );
        Reflect.set( deepBean, "bean.optional", "optional present" );
        Reflect.set( deepBean, "bean.i", 42 );


        assertThat( deepBean )
            .isEqualTo( new DeepBean(
                new Bean( 42, "new string", Optional.of( "optional present" ) ),
                Optional.empty(),
                Lists.of( new Bean( 10, "aaa" ), new Bean( 11, "bbb" ), new Bean( 12, "ccc" ) ),
                Maps.of( __( "x", Maps.of( __( "1", 1 ) ) ) )
            ) );
    }

    @Test
    public void constructor() {
        assertThat( Reflect.reflect( MatchingConstructor.class ).constructors ).hasSize( 2 );
        assertThatExceptionOfType( ReflectException.class )
            .isThrownBy( () -> Reflect.reflect( MatchingConstructor.class ).newInstance() )
            .withMessage( "class oap.reflect.MatchingConstructor: cannot find matching constructor: {} candidates: [oap.reflect.MatchingConstructor(int i,java.util.List<java.lang.Integer> list), oap.reflect.MatchingConstructor(java.util.List<java.lang.Integer> list)]. Classes must be compiled with '-parameters' option of javac." );
        assertThat( Reflect.reflect( NoConstructors.class ).constructors ).hasSize( 1 );
        assertThatExceptionOfType( ReflectException.class )
            .isThrownBy( () -> Reflect.reflect( MatchingConstructor.class ).newInstance( Map.of() ) )
            .withMessage( "class oap.reflect.MatchingConstructor: cannot find matching constructor: {} candidates: [oap.reflect.MatchingConstructor(int i,java.util.List<java.lang.Integer> list), oap.reflect.MatchingConstructor(java.util.List<java.lang.Integer> list)]. Classes must be compiled with '-parameters' option of javac." );
    }

    @Test
    public void method() throws NoSuchMethodException {
        assertThat( Reflect.reflect( C.class )
            .method( I.class.getDeclaredMethod( "m", String.class ) ) )
            .isNotNull();
    }

    @Test
    public void castMap() {
        Pair<Reflection, Reflection> params = Reflect.reflect( CForComponentType.class )
            .field( "map" )
            .orElseThrow()
            .type()
            .getMapComponentsType();
        assertThat( params ).isNotNull();
        assertThat( params._1 ).isEqualTo( Reflect.reflect( String.class ) );
        assertThat( params._2 ).isEqualTo( Reflect.reflect( String.class ) );
    }

}

interface I {
    void m( String a );
}

@Target( ElementType.FIELD )
@Retention( RetentionPolicy.RUNTIME )
@interface Ann {
    int a() default 1;
}


class C implements I {
    public void m( String a ) {

    }
}

@EqualsAndHashCode
@ToString
class Bean {
    int i;
    @Ann( a = 10 )
    int x = 1;
    String str = "aaa";
    List<String> l;
    Optional<String> optional = Optional.empty();
    List<Bean> list;
    Map<String, Bean> map;

    Bean() {
        this( 10 );
    }

    Bean( int i ) {
        this.i = i;
    }

    Bean( int i, String str ) {
        this.i = i;
        this.str = str;
    }

    Bean( int i, String str, Optional<String> optional ) {
        this.i = i;
        this.str = str;
        this.optional = optional;
    }

    Bean( int i, List<Bean> list, Map<String, Bean> map ) {
        this.i = i;
        this.list = list;
        this.map = map;
    }

    Bean( int i, int x ) {
        this.i = i;
        this.x = x;
    }
}


class StringList extends ArrayList<String> {

}

@EqualsAndHashCode
@ToString
class DeepBean {
    public Bean bean = new Bean();
    public Optional<Bean> beanOptional = Optional.of( new Bean() );
    public List<Bean> list = new ArrayList<>();
    public Map<String, Map<String, Integer>> map = Maps.empty();

    DeepBean( Bean bean, Optional<Bean> beanOptional ) {
        this.bean = bean;
        this.beanOptional = beanOptional;
    }

    DeepBean( Bean bean, Optional<Bean> beanOptional, List<Bean> list, Map<String, Map<String, Integer>> map ) {
        this.bean = bean;
        this.beanOptional = beanOptional;
        this.list = list;
        this.map = map;
    }

    @SuppressWarnings( "unused" )
    DeepBean() {
    }
}

@SuppressWarnings( "unused" )
class MatchingConstructor {
    MatchingConstructor( int i, List<Integer> list ) {}

    MatchingConstructor( List<Integer> list ) {}
}

class NoConstructors {}


@SuppressWarnings( "unused" )
class CForComponentType {
    ForComponentType map;
}

class ForComponentType implements Map<String, String> {

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsKey( Object key ) {
        return false;
    }

    @Override
    public boolean containsValue( Object value ) {
        return false;
    }

    @Override
    public String get( Object key ) {
        return null;
    }

    @Override
    public String put( String key, String value ) {
        return null;
    }

    @Override
    public String remove( Object key ) {
        return null;
    }

    @Override
    public void putAll( Map<? extends String, ? extends String> m ) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        return emptySet();
    }

    @Override
    public Collection<String> values() {
        return emptyList();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return emptySet();
    }
}
