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
import oap.testng.Fixtures;
import oap.ws.testng.WsFixture;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.httpUrl;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

@Slf4j
public class ValidationTest extends Fixtures {
    {
        fixture( new WsFixture( getClass(), ( ws, kernel ) -> {
            kernel.register( "validatedWS", new TestValidatedWS() );
            ws.exceptionToHttpCode.put( IllegalAccessException.class.getName(), 400 );
            ws.exceptionToHttpCode.put( "unknownclass", 400 );
        }, "validation-services.conf" ) );
    }

    @Test
    public void brokenValidator() {
        assertGet( httpUrl( "/vaildation/service/methodWithBrokenValidator?requiredParameter=10" ) )
            .responded( 500, "CausedByException", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ),
                "CausedByException" );
    }

    @Test
    public void wrongValidatorName() {
        String errorMessage = "No such method wrongValidatorName with the following parameters: [int requiredParameter]";
        assertGet( httpUrl( "/vaildation/service/methodWithWrongValidatorName?requiredParameter=10" ) )
            .responded( 500, errorMessage, TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), errorMessage );
    }

    @Test
    public void validatorWithWrongParameters() {
        String errorMessage = "missedParam required by validator wrongArgsValidator is not supplied by web method";
        assertGet( httpUrl( "/vaildation/service/methodWithWrongValidatorArgs?requiredParameter=10" ) )
            .responded( 500, errorMessage, TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ), errorMessage );
    }

    @Test
    public void exception() {
        assertGet( httpUrl( "/vaildation/service/exceptionRuntimeException" ) )
            .hasCode( 500 );
        assertGet( httpUrl( "/vaildation/service/exceptionIllegalAccessException" ) )
            .hasCode( 400 );
    }
}

