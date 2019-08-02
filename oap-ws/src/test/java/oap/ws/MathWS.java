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
package oap.ws;

import oap.http.HttpResponse;
import oap.http.Request;

import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Optional;

import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;

class MathWS {

    public int sum( int a, List<Integer> b, Optional<Integer> c, Optional<RetentionPolicy> rp ) {
        return a + b.stream().mapToInt( Integer::intValue ).sum()
            + ( c.isPresent() ? c.get() : 0 )
            + ( rp.isPresent() ? 5 : 0 );
    }

    @WsMethod( method = GET, path = "/" )
    public int test() {
        return 2;
    }

    @WsMethod( method = GET, path = "/test/sort/{id}" )
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

    public String req( Request req ) {
        return req.getBaseUrl() + req.context.location;
    }

    public Bean bean( int i, String s ) {
        return new Bean( i, s );
    }

    public Bean json( @WsParam( from = BODY ) Bean bean ) {
        return bean;
    }

    public List<String> list( @WsParam( from = BODY ) List<String> str ) {
        return str;
    }

    public int x( int i, String s ) {
        throw new RuntimeException( "failed" );
    }

    public HttpResponse code( int code ) {
        return HttpResponse.status( code ).response();
    }

    public String bytes( @WsParam( from = BODY ) byte[] bytes ) {
        return new String( bytes );
    }

    public String string( @WsParam( from = BODY ) String bytes ) {
        return bytes;
    }

    public static class Bean {
        public int i;
        public String s;

        public Bean() {
        }

        public Bean( int i, String s ) {
            this.i = i;
            this.s = s;
        }
    }
}
