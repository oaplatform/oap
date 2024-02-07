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

import lombok.extern.slf4j.Slf4j;
import oap.application.testng.KernelFixture;
import oap.http.Http;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static oap.io.Resources.urlOrThrow;

@Slf4j
public class ValidationTest extends Fixtures {

    public ValidationTest() {
        fixture( new KernelFixture( urlOrThrow( getClass(), "/application.test.conf" ) ) );
    }

    @Test
    public void brokenValidator() {
        assertGet( httpUrl( "/validation/service/methodWithBrokenValidator?requiredParameter=10" ) )
            .respondedJson( Http.StatusCode.INTERNAL_SERVER_ERROR, "CausedByException", "{\"message\":\"CausedByException\"}" );
    }

    @Test
    public void wrongValidatorName() {
        String errorMessage = "No such method wrongValidatorName with the following parameters: [int requiredParameter]";
        assertGet( httpUrl( "/validation/service/methodWithWrongValidatorName?requiredParameter=10" ) )
            .respondedJson( Http.StatusCode.INTERNAL_SERVER_ERROR, errorMessage, "{\"message\":\"" + errorMessage + "\"}" );
    }

    @Test
    public void validatorWithWrongParameters() {
        String errorMessage = "missedParam required by validator wrongArgsValidator is not supplied by web method";
        assertGet( httpUrl( "/validation/service/methodWithWrongValidatorArgs?requiredParameter=10" ) )
            .responded( Http.StatusCode.INTERNAL_SERVER_ERROR, errorMessage, Http.ContentType.APPLICATION_JSON, "{\"message\":\"" + errorMessage + "\"}" );
    }

    @Test
    public void validatorMethodWithArgs() {
        assertGet( httpUrl( "/validation/service/methodWithValidatorArgs?oddParam=1" ) )
            .responded( Http.StatusCode.BAD_REQUEST, "validation failed", Http.ContentType.APPLICATION_JSON, "{\"errors\":[\"" + "non odd param" + "\"]}" );
        assertGet( httpUrl( "/validation/service/methodWithValidatorArgs?oddParam=2" ) )
            .responded( Http.StatusCode.OK, "OK", Http.ContentType.APPLICATION_JSON, "true" );
    }

    @Test
    public void exception() {
        assertGet( httpUrl( "/validation/service/exceptionRuntimeException" ) )
            .hasCode( Http.StatusCode.INTERNAL_SERVER_ERROR );
    }
}
