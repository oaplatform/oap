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

import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static oap.http.ContentTypes.TEXT_PLAIN;
import static oap.http.Request.HttpMethod.POST;
import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.ws.WsParam.From.BODY;
import static oap.ws.WsParam.From.PATH;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

/**
 * Created by igor.petrenko on 02.03.2017.
 */
public class WsValidatePartialJsonTest extends AbstractWsValidateTest {

    private static TestBean testBean;

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        testBean = new TestBean();
    }

    @Override
    protected List<Object> getWsInstances() {
        return singletonList( new TestWS() );
    }

    @Test
    public void testValidation1() {
        assertPost( HTTP_PREFIX + "/test/run/validation/1/id1", "{\"b\":1}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{\"a\":[{\"b\":1}],\"id\":\"id1-1\"}" );
        assertPost( HTTP_PREFIX + "/test/run/validation/1/id2", "{}", APPLICATION_JSON )
            .responded( 400, "/b: required property is missing", TEXT_PLAIN, "/b: required property is missing" );
    }

    @Test
    public void testValidation2() {
        assertPost( HTTP_PREFIX + "/test/run/validation/2/id1", "{\"b\":1}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{\"a\":[{\"b\":1}],\"id\":\"id1-2\"}" );
        assertPost( HTTP_PREFIX + "/test/run/validation/2/id2", "{}", APPLICATION_JSON )
            .responded( 200, "OK", APPLICATION_JSON, "{\"a\":[{}],\"id\":\"id2-2\"}" );
        assertPost( HTTP_PREFIX + "/test/run/validation/2/id3", "{\"c\":1}", APPLICATION_JSON )
            .responded( 400, "additional properties are not permitted [c]", TEXT_PLAIN, "additional properties are not permitted [c]" );
    }

    public static class TestWS implements WsPartialValidateJson.PartialValidateJsonRootLoader<TestBean> {
        @WsMethod( path = "/run/validation/1/{id}", method = POST )
        public TestBean validation1(
            @WsParam( from = PATH ) String id,
            @WsPartialValidateJson(
                root = TestWS.class,
                idParameterName = "id",
                path = "a",
                schema = "/oap/ws/validate/WsValidateJsonTest/partial-schema.conf" )
            @WsParam( from = BODY ) TestBean.TestItem body
        ) {
            testBean.id = id + "-1";
            testBean.a.clear();
            testBean.a.add( body );
            return testBean;
        }

        @WsMethod( path = "/run/validation/2/{id}", method = POST )
        public TestBean validation2(
            @WsParam( from = PATH ) String id,
            @WsPartialValidateJson(
                root = TestWS.class,
                idParameterName = "id",
                path = "a",
                schema = "/oap/ws/validate/WsValidateJsonTest/partial-schema.conf",
                ignoreRequired = true )
            @WsParam( from = BODY ) TestBean.TestItem body
        ) {
            testBean.id = id + "-2";
            testBean.a.clear();
            testBean.a.add( body );
            return testBean;
        }

        @Override
        public Optional<TestBean> get( String id ) {
            return Optional.of( testBean );
        }
    }

    public static class TestBean {
        public ArrayList<TestItem> a = new ArrayList<>();

        public String id;

        public static class TestItem {
            public Integer b;
        }
    }
}
