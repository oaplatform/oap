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

import oap.http.testng.HttpAsserts;
import oap.util.Lists;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.Test;

import java.util.List;

import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.http.Request.HttpMethod.POST;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.QUERY;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class WsValidateJsonTest extends AbstractWsValidateTest {
    @Override
    protected List<Object> getWsInstances() {
        return Lists.of( new TestWS() );
    }

    @Test
    public void validation1() {
        assertPost( HttpAsserts.httpUrl( "/test/run/validation/1" ), "{\"a\":1}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{\"a\":1}" );
        assertPost( HttpAsserts.httpUrl( "/test/run/validation/1" ), "{}", APPLICATION_JSON )
            .responded( 400, "/a: required property is missing", TEXT_PLAIN, "/a: required property is missing" );
    }

    @Test
    public void validation2() {
        assertPost( HttpAsserts.httpUrl( "/test/run/validation/2" ), "{\"a\":1}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{\"a\":1}" );
        assertPost( HttpAsserts.httpUrl( "/test/run/validation/2" ), "{}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{}" );
        assertPost( HttpAsserts.httpUrl( "/test/run/validation/2" ), "{\"b\":1}", APPLICATION_JSON )
            .responded( 400, "additional properties are not permitted [b]", TEXT_PLAIN, "additional properties are not permitted [b]" );
    }

    @Test
    public void validation3() {
        assertPost( HttpAsserts.httpUrl( "/test/run/validation/3?type=type1" ), "{\"a\":1}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{\"a\":1}" );
        assertPost( HttpAsserts.httpUrl( "/test/run/validation/3?type=type2" ), "{\"b\":1}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{\"b\":1}" );
        assertPost( HttpAsserts.httpUrl( "/test/run/validation/3?type=type1" ), "{\"b\":1}", APPLICATION_JSON )
            .responded( 400, "/a: required property is missing", TEXT_PLAIN, "/a: required property is missing" );
    }

    public static class TestWS {
        @WsMethod( path = "/run/validation/1", method = POST )
        public TestBean validation1(
            @WsValidateJson( schema = "/oap/ws/validate/WsValidateJsonTest/schema.conf" )
            @WsParam( from = BODY ) TestBean body
        ) {
            return body;
        }

        @WsMethod( path = "/run/validation/2", method = POST )
        public TestBean validation2(
            @WsValidateJson( schema = "/oap/ws/validate/WsValidateJsonTest/schema.conf", ignoreRequired = true )
            @WsParam( from = BODY ) TestBean body
        ) {
            return body;
        }

        @WsMethod( path = "/run/validation/3", method = POST )
        public TestBean validation3(
            @WsValidateJson( schema = "/oap/ws/validate/WsValidateJsonTest/${type}-schema.conf" )
            @WsParam( from = BODY ) TestBean body,
            @WsParam( from = QUERY ) String type
        ) {
            return body;
        }
    }

    public static class TestBean {
        public Integer a;
        public Integer b;
    }
}
