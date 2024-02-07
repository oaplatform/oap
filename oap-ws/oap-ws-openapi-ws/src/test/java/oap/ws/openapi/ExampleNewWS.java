
package oap.ws.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import oap.http.server.nio.HttpServerExchange;
import oap.json.ext.Ext;
import oap.util.AssocList;
import oap.util.Lists;
import oap.util.Stream;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.sso.WsSecurity;
import org.joda.time.LocalDateTime;

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;

@SuppressWarnings( "unused" )
class ExampleNewWS {

    @WsSecurity( realm = "realm", permissions = "ALLOWED" )
    public int sum( @WsParam String realm, int a, @WsParam List<Integer> b, Optional<Integer> c, Optional<RetentionPolicy> rp ) {
        return 0;
    }

    @WsMethod( method = GET, path = "/" )
    public int test() {
        return 2;
    }

    @WsMethod( method = GET, path = "/test/sort/{id}" )
    @WsSecurity( permissions = { "SUPERADMIN" } )
    public String test1( @WsParam( from = PATH ) String id ) {
        return id;
    }

    @WsMethod( method = GET, path = "/test/sort={id}/test" )
    public String testEqual( @WsParam( from = PATH ) String id ) {
        return id;
    }

    @WsMethod( method = GET, path = "/test/sort/default" )
    public String test2() {
        return "__default__";
    }

    public int sumab( int a, int b ) {
        return a + b;
    }

    public int sumabopt( int a, Optional<Integer> b ) {
        return a + b.orElse( 0 );
    }

    public String id( String a ) {
        return a;
    }

    public RetentionPolicy en( RetentionPolicy a ) {
        return a;
    }

    public String req( HttpServerExchange exchange ) {
        return exchange.getRequestURI() + "-";
    }

    public List<NewWsBean> bean( int i, String s ) {
        return List.of( new NewWsBean( i, s ) );
    }

    public NewWsBean json( @WsParam( from = BODY ) NewWsBean bean ) {
        return bean;
    }

    public List<String> list( @WsParam( from = BODY ) List<String> str ) {
        return str;
    }

    public int x( int i, String s ) {
        throw new RuntimeException( "failed" );
    }

    public void code( int code, HttpServerExchange exchange ) {
        exchange.setStatusCode( code );
    }

    public String bytesParam( @WsParam( from = BODY ) byte[] bytes ) {
        return new String( bytes );
    }

    public byte[] bytesResult( @WsParam( from = BODY ) String toBytes ) {
        return Base64.getDecoder().decode( toBytes );
    }

    public List<byte[]> listOfBytesResult( @WsParam( from = BODY ) String toBytes ) {
        return Lists.of( Base64.getDecoder().decode( toBytes ), new byte[] { 0x1, 0x2, 0x3 } );
    }

    public String stringParam( @WsParam( from = BODY ) String bytes ) {
        return bytes;
    }

    public oap.util.Stream<String> getOapStreamOfStrings() {
        return Stream.of();
    }

    public java.util.stream.Stream<String> getVanillaStreamOfStrings( @WsParam( from = BODY ) List<String> str ) {
        return new ArrayList<>( str ).stream();
    }

    public CreativeUniversal getCreativeUniversal() {
        return new CreativeUniversal();
    }

    public Response response() {
        return null;
    }

    @Deprecated
    public String deprecated() {
        return null;
    }

    public static class NewWsBean2 {
        public int x;
        @Deprecated
        public double price;
    }

    public static class NewWsBean {
        public int i;
        @Deprecated
        public String s;
        public LocalDateTime dt;
        public NewWsBean2 b2;
        public Stream<String> stringStream = Stream.of( "One", "Two", "Five" );
        public Stream<Integer> intStream = Stream.of( 1, 2, 5 );
        public Ext ext;
        public Beans beans = new Beans();
        public Map<String, NewWsBean2> map = new HashMap<>();

        public class Beans extends AssocList<String, NewWsBean2> {

            @Override
            protected String keyOf( NewWsBean2 bean2 ) {
                return String.valueOf( bean2.x );
            }
        }

        protected NewWsBean() {
        }

        protected NewWsBean( int i, String s ) {
            this.i = i;
            this.s = s;
        }

        public String getSomething() {
            return null;
        }

        @JsonIgnore
        public String getIgnored() {
            return null;
        }

        public String getS() {
            return s;
        }

        public static class BeanExt extends Ext {
            public String extension;
        }
    }
}
