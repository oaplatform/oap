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
package oap.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.io.Files;
import oap.testng.AbstractTest;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static oap.util.Pair.__;
import static org.testng.Assert.assertEquals;

public class BinderTest extends AbstractTest {

    //todo generic map-list binding
    private static <T> void assertBind( Class<T> clazz, T source ) {
        System.out.println( "========================================" );
        String json2 = Binder.json.marshal( source );
        System.out.println( "JSON2:" );
        System.out.println( json2 );
        T result = Binder.json.unmarshal( clazz, json2 );
        System.out.println( "Object:" );
        System.out.println( result );
        assertEquals( result, source );
    }

    private static <T> void assertBind( TypeReference<T> ref, T source ) {
        System.out.println( "========================================" );
        String json = Binder.json.marshal( source );
        System.out.println( "JSON:" );
        System.out.println( json );
        T result = Binder.json.unmarshal( ref, json );
        System.out.println( "Object:" );
        System.out.println( result );
        assertEquals( result, source );
    }

    @Test
    public void bindPrimitives() {
        assertBind( boolean.class, true );
        assertBind( boolean.class, false );
        assertBind( Object.class, null );
        assertBind( int.class, 10 );
        assertBind( double.class, 10.3 );
        assertBind( char.class, 'a' );
        assertBind( Path.class, Files.path( "/var/lib" ) );
    }

    @Test
    public void bindString() {
        assertBind( String.class, "test" );
        assertEquals( "test", Binder.json.unmarshal( String.class, "\"test\"" ) );
        assertEquals( 1.1d, Binder.json.unmarshal( double.class, "\"1.1\"" ) );
    }

    @Test
    public void bindEnum() {
        assertBind( TestEnum.class, TestEnum.C );
        assertBind( EnumBean.class, new EnumBean( TestEnum.B ) );
    }

    @Test
    public void bindList() {
        assertBind( new TypeReference<ArrayList<Integer>>() {
        }, Lists.of( 1, 2, 3 ) );
    }

    @Test
    public void bindObject() {
        assertBind( Bean.class, new Bean( "x", 10, new Bean2( "y", 15, Lists.of( 1, 2, 3 ) ) ) );
    }

    @Test
    public void bindQuote() {
        assertBind( Bean.class, new Bean( "\\\n\r\t\"'x", 10, null ) );
    }

    @Test
    public void bindNulls() {
        assertBind( Bean.class, new Bean( null, 10, null ) );
    }

    @Test
    public void bindBeanListList() {
        assertBind( ListBean.class, new ListBean( Lists.of( Lists.of( 1, 2, 3 ), Lists.of( 4, 5, 6 ) ) ) );
    }

    @Test
    public void bindBeanEmptyList() {
        assertBind( Bean3.class, new Bean3() );
    }

    @Test
    public void bindGenericObject() {
        assertBind( BeanGB.class, new BeanGB( new BeanGeneric<>( Lists.of( 1, 2, 3 ) ) ) );
    }

    @Test
    public void bindAtomicLong() {
        assertBind( AtomicLongBean.class, new AtomicLongBean( 10 ) );
    }

    @Test
    public void bindDeepGeneric() {
        assertBind( BeanGB2.class, new BeanGB2(
            new BeanGeneric<>( Lists.of( new BeanGeneric<>( Lists.of( 1, 2 ) ),
                new BeanGeneric<>( Lists.of( 3, 4 ) ) ) ),
            new BeanGeneric<>(
                Lists.of( new BeanGeneric<>( Lists.of( 5, 6 ) ), new BeanGeneric<>( Lists.of( 7, 8 ) ) ) )
        ) );
    }

    @Test
    public void bindAnyRefField() {
        assertBind( BeanAnyRef.class, new BeanAnyRef( 1 ) );
        assertBind( BeanAnyRef.class, new BeanAnyRef( "str" ) );
        assertBind( BeanAnyRef.class, new BeanAnyRef( TestEnum.B ) );
        assertBind( BeanAnyRef.class, new BeanAnyRef( new EnumBean( TestEnum.B ) ) );
//        assertException( new JsonException( "a requires :type hint" ),
//            () -> Binder.unmarshalString( BeanAnyRef.class, "{\"a\":\"str\"}" ) );
    }

    @Test
    public void bindNamed() {
        assertBind( NamedBean.class, new NamedBean( 10 ) );
        assertEquals( Binder.json.marshal( new NamedBean( 10 ) ), "{\"y\":10}" );
    }

    @Test
    public void bindMap() {
        assertBind( MapBean.class, new MapBean( __( "a", 1l ), __( "b", 2l ) ) );
        assertEquals( Binder.json.marshal( new MapBean( __( "a", 1l ), __( "b", 2l ) ) ),
            "{\"map\":{\"a\":1,\"b\":2}}" );
    }

    @Test
    void optional() {
        assertBind( OptBean.class, new OptBean() );
    }

}

@EqualsAndHashCode
@ToString
class Bean {
    public String str;
    public int i;
    public Bean2 sb2;

    public Bean() {
    }

    public Bean( String str, int i, Bean2 sb2 ) {
        this.str = str;
        this.i = i;
        this.sb2 = sb2;
    }

}

@EqualsAndHashCode
@ToString
class Bean2 {
    String s2;
    int i2;
    ArrayList<Integer> list = new ArrayList<>();

    public Bean2() {
    }

    public Bean2( String s2, int i2, ArrayList<Integer> list ) {
        this.s2 = s2;
        this.i2 = i2;
        this.list = list;
    }
}

@EqualsAndHashCode
@ToString
class Bean3 {
    public ArrayList<Integer> list = new ArrayList<>();

    public Bean3() {
    }
}

@EqualsAndHashCode
@ToString
class ListBean {
    public ArrayList<ArrayList<Integer>> l;

    public ListBean() {
    }

    public ListBean( ArrayList<ArrayList<Integer>> l ) {
        this.l = l;
    }

}

@EqualsAndHashCode
@ToString
class BeanGeneric<A> {
    A a;

    public BeanGeneric() {
    }

    public BeanGeneric( A a ) {
        this.a = a;
    }
}

@EqualsAndHashCode
@ToString
class BeanGB {
    BeanGeneric<ArrayList<Integer>> bg;

    public BeanGB() {
    }

    public BeanGB( BeanGeneric<ArrayList<Integer>> bg ) {
        this.bg = bg;
    }

}


@EqualsAndHashCode
@ToString
class BeanGB2 {
    BeanGeneric<ArrayList<BeanGeneric<ArrayList<Integer>>>> bg;
    BeanGeneric<ArrayList<BeanGeneric<ArrayList<Integer>>>> bg2;

    public BeanGB2() {
    }

    public BeanGB2( BeanGeneric<ArrayList<BeanGeneric<ArrayList<Integer>>>> bg,
        BeanGeneric<ArrayList<BeanGeneric<ArrayList<Integer>>>> bg2 ) {
        this.bg = bg;
        this.bg2 = bg2;
    }
}

@EqualsAndHashCode
@ToString
class EnumBean {
    TestEnum v;

    public EnumBean() {
    }

    public EnumBean( TestEnum v ) {
        this.v = v;
    }
}

@EqualsAndHashCode
@ToString
class BeanAnyRef {
    @JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "a:type" )
    public Object a;

    public BeanAnyRef() {
    }

    public BeanAnyRef( Object a ) {
        this.a = a;
    }

}

@EqualsAndHashCode
@ToString
class OptBean {
    Optional<Integer> i1 = Optional.empty();
    Optional<Integer> i2 = Optional.of( 1 );

    public OptBean() {
    }

}

@EqualsAndHashCode
@ToString
class NamedBean {
    @JsonProperty( "y" )
    public int x;

    public NamedBean() {

    }

    public NamedBean( int x ) {
        this.x = x;
    }
}

@EqualsAndHashCode
@ToString
class MapBean {
    public LinkedHashMap<String, Long> map;

    public MapBean() {

    }

    @SafeVarargs
    public MapBean( Pair<String, Long>... pairs ) {
        this.map = Maps.of( pairs );
    }
}

@ToString
class AtomicLongBean {
    public AtomicLong v = new AtomicLong();

    public AtomicLongBean() {
    }

    public AtomicLongBean( long v ) {
        this.v.set( v );
    }

    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        AtomicLongBean that = (AtomicLongBean) o;

        return v.get() == that.v.get();

    }

    @Override
    public int hashCode() {
        return Long.hashCode( v.get() );
    }
}
