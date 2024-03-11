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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.concurrent.LongAdder;
import oap.json.testng.JsonAsserts;
import oap.reflect.Reflect;
import oap.reflect.TypeRef;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Pair;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;

public class BinderTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public BinderTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture( getClass() ) );
    }

    //todo generic map-list binding
    private static <T> void assertBind( Class<T> clazz, T source ) {
        System.out.println( "========================================" );
        String json = Binder.json.marshal( source );
        System.out.println( "JSON:" );
        System.out.println( json );
        T result = Binder.json.unmarshal( clazz, json );
        System.out.println( "Object:" );
        System.out.println( result );
        assertThat( result ).isEqualTo( source );
    }

    private static <T> void assertBind( TypeRef<T> ref, T source ) {
        System.out.println( "========================================" );
        String json = Binder.json.marshal( source );
        System.out.println( "JSON:" );
        System.out.println( json );
        T result = Binder.json.unmarshal( ref, json );
        System.out.println( "Object:" );
        System.out.println( result );
        assertThat( result ).isEqualTo( source );
    }

    //todo generic map-list binding
    private static <T> void assertBindXml( Class<T> clazz, T source ) {
        System.out.println( "========================================" );
        String json = Binder.xml.marshal( source );
        System.out.println( "XML:" );
        System.out.println( json );
        T result = Binder.xml.unmarshal( clazz, json );
        System.out.println( "Object:" );
        System.out.println( result );
        assertThat( result ).isEqualTo( source );
    }

    private static <T> void assertBindWithTyping( Class<T> clazz, T source ) {
        System.out.println( "========================================" );
        String json = Binder.jsonWithTyping.marshal( source );
        System.out.println( "JSON:" );
        System.out.println( json );
        T result = Binder.jsonWithTyping.unmarshal( clazz, json );
        System.out.println( "Object:" );
        System.out.println( result );
        assertThat( result ).isEqualTo( source );
    }

    @Test
    public void deserializeTimeToNumber() {
        assertThat( Binder.hocon.unmarshal( Bean.class, "{l = 1s}" ).l ).isEqualTo( 1000L );
        assertThat( Binder.hocon.unmarshal( Bean.class, "{l = 1ms}" ).l ).isEqualTo( 1L );
        assertThat( Binder.hocon.unmarshal( Bean.class, "{l = 2m}" ).l ).isEqualTo( 1000L * 60 * 2 );
        assertThat( Binder.hocon.unmarshal( Bean.class, "{l = 3h}" ).l ).isEqualTo( 1000L * 60 * 60 * 3 );
        assertThat( Binder.hocon.unmarshal( Bean.class, "{l = 4d}" ).l ).isEqualTo( 1000L * 60 * 60 * 24 * 4 );
        assertThat( Binder.hocon.unmarshal( Bean.class, "{l = 5w}" ).l ).isEqualTo( 1000L * 60 * 60 * 24 * 7 * 5 );
    }

    @Test
    public void bindPrimitives() {
        assertBind( boolean.class, true );
        assertBind( boolean.class, false );
        assertBind( Object.class, null );
        assertBind( int.class, 10 );
        assertBind( double.class, 10.3 );
        assertBind( char.class, 'a' );
        assertBind( Path.class, Paths.get( "/var/lib" ) );
    }

    @Test
    public void testUnmarshalFromAny() {
        assertThat( Binder.hocon.unmarshalFromAny( Bean.class, "{l = 1s}" ) ).isEqualTo( new Bean( 1000L ) );
        assertThat( Binder.hocon.unmarshalFromAny( Bean.class, "{\"l\": \"1s\"}" ) ).isEqualTo( new Bean( 1000L ) );
        assertThat( Binder.hocon.unmarshalFromAny( Bean.class, Map.of( "l", 1000L ) ) ).isEqualTo( new Bean( 1000L ) );
        assertThat( Binder.hocon.unmarshalFromAny( Bean.class, "/oap/json/BinderTest/bean.conf" ) ).isEqualTo( new Bean( 1000L ) );
    }

    @Test
    public void bindString() {
        assertBind( String.class, "test" );
        assertThat( Binder.json.unmarshal( String.class, "\"test\"" ) ).isEqualTo( "test" );
        assertThat( Binder.json.<Double>unmarshal( double.class, "\"1.1\"" ) ).isEqualTo( 1.1d );
    }

    @Test
    public void bindEnum() {
        assertBind( TestEnum.class, TestEnum.C );
        assertBind( EnumBean.class, new EnumBean( TestEnum.B ) );
    }

    @Test
    public void bindList() {
        assertBind( new TypeRef<>() {
        }, Lists.of( 1, 2, 3 ) );
    }

    @Test
    public void bindObject() {
        assertBind( Bean.class, new Bean( "x", 10, new Bean2( "y", 15, Lists.of( 1, 2, 3 ) ) ) );
    }

    @Test
    public void bindLongAdder() {
        assertBind( LongAdderBean.class, new LongAdderBean().add( 10 ) );
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
    public void bindEmptyObject() {
        assertBind( EmptyBean.class, new EmptyBean() );
    }

    @Test
    public void bindDateTime() {
        assertBind( DateTimeBean.class, new DateTimeBean( Dates.nowUtc() ) );
    }

    @Test
    public void bindLocalDate() {
        assertBind( LocalDateBean.class, new LocalDateBean( LocalDate.now() ) );
        assertThat( Binder.json.<LocalDate>unmarshal( Reflect.reflect( LocalDate.class ), "\"2021-01-10\"" ) )
            .isEqualTo( LocalDate.of( 2021, 1, 10 ) );
    }

    @Test
    public void bindAtomicLong() {
        assertBind( AtomicLongBean.class, new AtomicLongBean( 10 ) );
        assertBindWithTyping( AtomicLongBean.class, new AtomicLongBean( 10 ) );
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
    }

    @Test
    public void bindNamed() {
        assertBind( NamedBean.class, new NamedBean( 10 ) );
        assertThat( Binder.json.marshal( new NamedBean( 10 ) ) ).isEqualTo( "{\"y\":10}" );
    }

    @Test
    public void bindMap() {
        assertBind( MapBean.class, new MapBean( __( "a", 1L ), __( "b", 2L ) ) );
        assertThat( Binder.json.marshal( new MapBean( __( "a", 1L ), __( "b", 2L ) ) ) )
            .isEqualTo( "{\"map\":{\"a\":1,\"b\":2}}" );
    }

    @Test
    public void bindMapXml() {
        assertBindXml( MapBean.class, new MapBean( __( "a", 1L ), __( "b", 2L ) ) );
        assertThat( Binder.xml.marshal( new MapBean( __( "a", 1L ), __( "b", 2L ) ) ) )
            .isEqualTo( "<?xml version='1.0' encoding='UTF-8'?><MapBean><map><a>1</a><b>2</b></map></MapBean>" );
    }

    @Test( enabled = false )
    public void bindMapXmlCaseInsensitive() {
        assertThat( Binder.xml.marshal( Binder.xml.unmarshal( CaseSensXmlBean.class,
            "<?xml version='1.0' encoding='UTF-8'?><CaseSensXmlBean><Bean></Bean></CaseSensXmlBean>" ) ) )
            .isEqualTo( "<?xml version='1.0' encoding='UTF-8'?><CaseSensXmlBean><bean><i>0</i><l>0</l><bean></CaseSensXmlBean>" );
    }

    @Test
    public void emptyListNotIncluded() {
        JsonAsserts.assertJson( Binder.json.marshal( new Complex( null, Lists.empty(), Map.of() ) ) )
            .isEqualTo( "{}" );
    }


    @Test
    void optional() {
        assertBind( OptBean.class, new OptBean() );
    }

    @Test
    public void customLong() {
        assertThat( Binder.hocon.unmarshal( LongBean.class, "{l = 2s}" ) ).isEqualTo( new LongBean( 2000 ) );
        assertThat( Binder.json.unmarshal( LongBean.class, "{\"l\" : \"2kb\"}" ) ).isEqualTo( new LongBean( 2048 ) );
    }

    @Test
    public void map() {
        LinkedHashMap<String, Object> map = Maps.of(
            __( "str", "aaa" ),
            __( "i", 1 ),
            __( "sb2", Maps.of(
                __( "s2", "bbb" ),
                __( "i2", 2 ),
                __( "list", Lists.of( 1, 2, 3, 4 ) )
            ) )
        );
        Bean expected = new Bean( "aaa", 1, new Bean2( "bbb", 2, Lists.of( 1, 2, 3, 4 ) ) );
        assertThat( Binder.json.unmarshal( Bean.class, map ) ).isEqualTo( expected );

    }

    @Test
    public void marshalToPath() {
        Path path = testDirectoryFixture.testPath( "test.json" );
        Binder.json.marshal( path, new MapBean( __( "a", 1L ), __( "b", 2L ) ) );

        assertThat( path ).hasContent( "{\"map\":{\"a\":1,\"b\":2}}" );
    }

    @Test
    public void testUpdate() {
        var obj = new Bean( "1", 1, null );
        Binder.update( obj, Map.of( "str", "test" ) );
        assertThat( obj.str ).isEqualTo( "test" );
//todo fix it after kernel cleanup
        Binder.update( obj, Maps.of( __( "str", null ) ) );
        assertThat( obj.str ).isNull();

        Binder.update( obj, "{str = test}" );
        assertThat( obj.str ).isEqualTo( "test" );

        Binder.update( obj, "{str = null}" );
        assertThat( obj.str ).isNull();

    }
}

@ToString
@EqualsAndHashCode
class EmptyBean {
}

@EqualsAndHashCode
@ToString
class Bean {
    public String str;
    public int i;
    public long l;
    public Bean2 sb2;

    Bean() {
    }

    Bean( long l ) {
        this.l = l;
    }

    Bean( String str, int i, Bean2 sb2 ) {
        this( str, i, 0, sb2 );
    }


    Bean( String str, int i, long l, Bean2 sb2 ) {
        this.str = str;
        this.i = i;
        this.l = l;
        this.sb2 = sb2;
    }

}

@EqualsAndHashCode
@ToString
class Bean2 {
    String s2;
    int i2;
    List<Integer> list = new ArrayList<>();

    Bean2() {
    }

    Bean2( String s2, int i2, List<Integer> list ) {
        this.s2 = s2;
        this.i2 = i2;
        this.list = list;
    }
}

@EqualsAndHashCode
@ToString
class Bean3 {
    public ArrayList<Integer> list = new ArrayList<>();


    Bean3() {
    }
}

@EqualsAndHashCode
@ToString
class ListBean {
    public List<List<Integer>> l;

    ListBean() {
    }

    ListBean( List<List<Integer>> l ) {
        this.l = l;
    }

}

@EqualsAndHashCode
@ToString
class BeanGeneric<A> {
    A a;

    BeanGeneric() {
    }

    BeanGeneric( A a ) {
        this.a = a;
    }
}

@EqualsAndHashCode
@ToString
class BeanGB {
    BeanGeneric<List<Integer>> bg;


    BeanGB() {
    }

    BeanGB( BeanGeneric<List<Integer>> bg ) {
        this.bg = bg;
    }

}


@EqualsAndHashCode
@ToString
class BeanGB2 {
    BeanGeneric<List<BeanGeneric<List<Integer>>>> bg;
    BeanGeneric<List<BeanGeneric<List<Integer>>>> bg2;

    BeanGB2() {
    }

    BeanGB2( BeanGeneric<List<BeanGeneric<List<Integer>>>> bg,
             BeanGeneric<List<BeanGeneric<List<Integer>>>> bg2 ) {
        this.bg = bg;
        this.bg2 = bg2;
    }
}

@EqualsAndHashCode
@ToString
class EnumBean {
    TestEnum v;

    EnumBean() {
    }

    EnumBean( TestEnum v ) {
        this.v = v;
    }
}

@EqualsAndHashCode
@ToString
class BeanAnyRef {
    @JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "a:type" )
    public Object a;

    BeanAnyRef() {
    }

    BeanAnyRef( Object a ) {
        this.a = a;
    }

}

@EqualsAndHashCode
@ToString
class OptBean {
    Optional<Integer> i1 = Optional.empty();
    Optional<Integer> i2 = Optional.of( 1 );
    Optional<List<String>> list = Optional.of( Lists.of( "a", "b" ) );
    Optional<List<String>> emptyList = Optional.empty();

    OptBean() {
    }

}

@EqualsAndHashCode
@ToString
class NamedBean {
    @JsonProperty( "y" )
    public int x;

    NamedBean() {

    }

    NamedBean( int x ) {
        this.x = x;
    }
}

@EqualsAndHashCode
@ToString
class MapBean {
    public LinkedHashMap<String, Long> map;

    MapBean() {

    }

    @SafeVarargs
    MapBean( Pair<String, Long>... pairs ) {
        this.map = Maps.of( pairs );
    }
}

@ToString
class AtomicLongBean {
    public AtomicLong v = new AtomicLong();

    AtomicLongBean() {
    }

    AtomicLongBean( long v ) {
        this.v.set( v );
    }

    @Override
    public boolean equals( Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        AtomicLongBean that = ( AtomicLongBean ) o;

        return v.get() == that.v.get();

    }

    @Override
    public int hashCode() {
        return Long.hashCode( v.get() );
    }
}

@ToString
@EqualsAndHashCode
class LongBean {
    long l;

    LongBean( long l ) {
        this.l = l;
    }

    LongBean() {

    }
}

@ToString
@EqualsAndHashCode
class DateTimeBean {
    DateTime date;

    @JsonCreator
    DateTimeBean( DateTime date ) {
        this.date = date;
    }
}

@ToString
@EqualsAndHashCode
class LocalDateBean {
    LocalDate date;

    @JsonCreator
    LocalDateBean( LocalDate date ) {
        this.date = date;
    }
}

@ToString
@EqualsAndHashCode
class LongAdderBean {
    public final LongAdder la = new LongAdder();

    public LongAdderBean add( long value ) {
        la.add( value );
        return this;
    }
}

@ToString
@EqualsAndHashCode
class Complex {
    Bean bean;
    List<Bean> list;
    Map<String, Bean> map;


    Complex( Bean bean, List<Bean> list, Map<String, Bean> map ) {
        this.bean = bean;
        this.list = list;
        this.map = map;
    }
}

class CaseSensXmlBean {
    @JacksonXmlProperty( localName = "bean" )
    @JacksonXmlElementWrapper( useWrapping = false )
    List<Bean> beans = new ArrayList<>();
}
