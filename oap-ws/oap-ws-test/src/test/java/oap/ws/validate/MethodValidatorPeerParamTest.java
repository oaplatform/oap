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
package oap.ws.validate;

import oap.application.testng.KernelFixture;
import oap.http.Http;
import oap.testng.Fixtures;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.POST;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.QUERY;
import static oap.ws.validate.ValidationErrors.empty;
import static oap.ws.validate.ValidationErrors.error;

public class MethodValidatorPeerParamTest extends Fixtures {
    public MethodValidatorPeerParamTest() {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void validationDefault() {
        assertPost( httpUrl( "/mvpp/run/validation/default?i=1" ), "test", Http.ContentType.TEXT_PLAIN )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.APPLICATION_JSON, "\"1test\"" );
    }

    @Test
    public void validationOk() {
        assertPost( httpUrl( "/mvpp/run/validation/ok?i=1" ), "test", Http.ContentType.TEXT_PLAIN )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.APPLICATION_JSON, "\"1test\"" );
    }

    @Test
    public void validationOkList() {
        assertPost( httpUrl( "/mvpp/run/validation/ok?i=1&listString=_11&listString=_12" ), "test", Http.ContentType.TEXT_PLAIN )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.APPLICATION_JSON, "\"1_11/_12test\"" );
    }

    @Test
    public void validationOkOptional() {
        assertPost( httpUrl( "/mvpp/run/validation/ok?i=1&optString=2" ), "test", Http.ContentType.TEXT_PLAIN )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.APPLICATION_JSON, "\"12test\"" );
    }

    @Test
    public void validationFail() {
        assertPost( httpUrl( "/mvpp/run/validation/fail?i=1" ), "test", Http.ContentType.TEXT_PLAIN )
            .respondedJson( Http.StatusCode.BAD_REQUEST, "validation failed", "{\"errors\": [\"error:1\", \"error:test\"]}" );
    }

    @Test
    public void validationRequiredFailed() {
        assertPost( httpUrl( "/mvpp/run/validation/ok" ), "test", Http.ContentType.TEXT_PLAIN )
            .respondedJson( Http.StatusCode.BAD_REQUEST, "'int i' is required", "{\"errors\": [\"'int i' is required\"]}" );
    }

    @Test
    public void validationTypeFailed() {
        assertPost( httpUrl( "/mvpp/run/validation/ok?i=unsupportedStringToIntCast" ), "test", Http.ContentType.TEXT_PLAIN )
            .hasCode( Http.StatusCode.BAD_REQUEST );
    }

    public static class Test2WS {
        @WsMethod( path = "/run/validation/default", method = POST )
        public String validationDefault(
            @WsParam( from = QUERY ) int i,
            @WsParam( from = BODY ) String string
        ) {
            return i + string;
        }

        @WsMethod( path = "/run/validation/ok", method = POST )
        public String validationOkTestWS(
            @WsParam( from = QUERY ) @WsValidate( "validateOkInt" ) int i,
            @WsParam( from = QUERY ) @WsValidate( "validateOkOptString" ) Optional<String> optString,
            @WsParam( from = QUERY ) @WsValidate( "validateOkListString" ) List<String> listString,
            @WsParam( from = BODY ) @WsValidate( "validateOkString" ) String string
        ) {
            return i + optString.orElse( "" ) + String.join( "/", listString ) + string;
        }

        @WsMethod( path = "/run/validation/fail", method = POST )
        public String validationFail(
            @WsParam( from = QUERY ) @WsValidate( "validateFailInt" ) int i,
            @WsParam( from = BODY ) @WsValidate( "validateFailString" ) String string
        ) {
            return i + string;
        }

        protected ValidationErrors validateOkInt( int i ) {
            return empty();
        }

        protected ValidationErrors validateOkOptString( Optional<String> optString ) {
            return empty();
        }

        protected ValidationErrors validateOkListString( List<String> listString ) {
            return empty();
        }

        protected ValidationErrors validateOkString( String string ) {
            return empty();
        }

        protected ValidationErrors validateFailInt( int i ) {
            return error( "error:" + i );
        }

        protected ValidationErrors validateFailString( String string ) {
            return error( "error:" + string );
        }
    }
}
