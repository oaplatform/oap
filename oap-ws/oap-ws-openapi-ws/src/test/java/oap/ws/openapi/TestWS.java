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
package oap.ws.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import oap.http.server.nio.HttpServerExchange;
import oap.json.ext.Ext;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.sso.WsSecurity;
import org.joda.time.LocalDateTime;

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;

public class TestWS {

    @WsMethod( method = GET, path = "/", description = "Returns a simple word Two as a result." )
    public int test() {
        return 2;
    }

    @WsMethod( method = GET, path = "/test/sort/{id}" )
    @WsSecurity( realm = "organizationId", permissions = { "account:read", "permissions:read" } )
    public String test1( @WsParam( from = PATH ) String id ) {
        return id;
    }

    @WsMethod( method = GET, path = "/test/sort={id}/test" )
    @WsSecurity( realm = "organizationId", permissions = { "account:write", "permissions:write" } )
    public String testEqual( @WsParam( from = PATH ) String id ) {
        return id;
    }

    @WsMethod( method = GET, path = "/test/sort/default" )
    @WsSecurity( realm = "organizationId", permissions = { "account:read" } )
    public String test2() {
        return "__default__";
    }

    @WsMethod( method = GET, path = "generic" )
    public Map<String, List<Bean>> mapbean() {
        return Map.of();
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

    public List<Bean> bean( int i, String s ) {
        return List.of( new Bean( i, s ) );
    }

    public Bean json( @WsParam( from = BODY ) Bean bean ) {
        return bean;
    }

    public Stream<String> getStream( @WsParam( from = BODY ) List<String> str ) {
        return new ArrayList<>( str ).stream();
    }

    public int x( int i, String s ) {
        throw new RuntimeException( "failed" );
    }

    public void code( int code, HttpServerExchange exchange ) {
        exchange.setStatusCode( code );
    }

    public String bytes( @WsParam( from = BODY ) byte[] bytes ) {
        return new String( bytes );
    }

    public String string( @WsParam( from = BODY ) String bytes ) {
        return bytes;
    }

    public static class Bean2 {
        public int x;
        @Deprecated
        public boolean overpriced;
        public double price;
    }

    public static class Bean {
        public int i;
        public String s;
        public LocalDateTime dt;
        public Bean2 b2;
        public Ext ext;

        protected Bean() {
        }

        protected Bean( int i, String s ) {
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
