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

import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Lists;
import org.testng.annotations.Test;

import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.ws.WsParam.From.PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public class WsMethodMatcherTest {
    @Test
    public void compile() {
        assertEquals( WsMethodMatcher.compile( "/y/{year:(\\d\\d\\d\\d)}/{month}/{date}" ).toString(), "^/y/(\\d\\d\\d\\d)/([^/]+)/([^/]+)$" );
        assertEquals( WsMethodMatcher.compile( "/y/{year:(\\d{4})}/{month}/{date}" ).toString(), "^/y/(\\d{4})/([^/]+)/([^/]+)$" );
        assertEquals( WsMethodMatcher.compile( "/" ).toString(), "^/?$" );
    }

    @Test
    public void pathParam() {
        String mapping = "/y/{year:(\\d{4})}/{month}/{date}";
        String path = "/y/2009/April/12";
        assertEquals( Optional.of( "2009" ), WsMethodMatcher.pathParam( mapping, path, "year" ) );
        assertEquals( Optional.of( "April" ), WsMethodMatcher.pathParam( mapping, path, "month" ) );
        assertEquals( Optional.of( "12" ), WsMethodMatcher.pathParam( mapping, path, "date" ) );
    }

    @Test
    public void match() {
        Reflection reflect = Reflect.reflect( WS.class );
        WsMethodMatcher matcher = new WsMethodMatcher( WS.class );
        assertThat( matcher.findMethod( "/", GET ) ).isEqualTo( reflect.method( "root" ) );
        assertThat( matcher.findMethod( "/const", GET ) ).isEqualTo( reflect.method( "c" ) );
        assertThat( matcher.findMethod( "/const/const", GET ) ).isEqualTo( reflect.method( "cc" ) );
        assertThat( matcher.findMethod( "/const2/const", GET ) ).isEqualTo( reflect.method( "c2c" ) );
        assertThat( matcher.findMethod( "/const/aaa/const", GET ) ).isEqualTo( reflect.method( "cvc" ) );

    }

    @Test
    public void matchOWS() {
        Reflection reflect = Reflect.reflect( OWS.class );
        WsMethodMatcher matcher = new WsMethodMatcher( OWS.class );
        assertThat( matcher.findMethod( "/", GET ) ).isEqualTo( reflect.method( "list" ) );
        assertThat( matcher.findMethod( "/asdAasdsd", POST ) ).isEqualTo( reflect.method( "store" ) );
        assertThat( matcher.findMethod( "/asdAasdsd", GET ) ).isEqualTo( reflect.method( "get" ) );
        assertThat( matcher.findMethod( "/register", POST ) ).isEqualTo( reflect.method( "register" ) );
        assertThat( matcher.findMethod( "/raaegister", POST ) ).isEqualTo( reflect.method( "store" ) );
        assertThat( matcher.findMethod( "/asdAasdsd/add-account", POST ) ).isEqualTo( reflect.method( "addAccount" ) );
        assertThat( matcher.findMethod( "/asdAasdsd/accounts", GET ) ).isEqualTo( reflect.method( "accounts" ) );
        assertThat( matcher.findMethod( "/asdAasdsd/accounts/asdaa", GET ) ).isEqualTo( reflect.method( "account" ) );
    }

    @Test
    public void comparator() {
        Reflection reflect = Reflect.reflect( OWS.class );
        Reflection.Method register = reflect.method( "register" ).orElseThrow();
        Reflection.Method store = reflect.method( "store" ).orElseThrow();
        var list = Lists.of( register, store );
        list.sort( WsMethodMatcher::constantFirst );
        assertThat( list ).containsExactly( register, store );
    }

    @SuppressWarnings( "unused" )
    public static class WS {
        @WsMethod( path = "/" )
        public void root() {}

        @WsMethod( path = "/const" )
        public void c() {}

        @WsMethod( path = "/const/const" )
        public void cc() {}

        @WsMethod( path = "/const2/const" )
        public void c2c() {}

        @WsMethod( path = "/const/{var1}/const" )
        public void cvc( @WsParam( from = PATH ) String var1 ) {}
    }

    @SuppressWarnings( "unused" )
    public static class OWS {

        @WsMethod( path = "/{oId}", method = POST )
        public void store( @WsParam( from = PATH ) String oId ) {}

        @WsMethod( path = "/{oId}", method = GET )
        public void get( @WsParam( from = PATH ) String oId ) {}

        @WsMethod( path = "/", method = GET )
        public void list() {}

        @WsMethod( path = "/register", method = POST )
        public void register() {}

        @WsMethod( path = "/{oId}/add-account", method = POST )
        public void addAccount( @WsParam( from = PATH ) String oId ) {}

        @WsMethod( path = "/{oId}/accounts", method = GET )
        public void accounts( @WsParam( from = PATH ) String oId ) {}

        @WsMethod( path = "/{oId}/accounts/{aId}", method = GET )
        public void account( @WsParam( from = PATH ) String oId, @WsParam( from = PATH ) String aId ) {}
    }
}
