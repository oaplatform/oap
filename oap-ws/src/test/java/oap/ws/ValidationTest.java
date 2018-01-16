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
import oap.application.Kernel;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Collections.singletonList;
import static oap.http.testng.HttpAsserts.HTTP_URL;
import static oap.http.testng.HttpAsserts.assertGet;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;

@Slf4j
public class ValidationTest extends AbstractWebServicesTest {
    @Override
    protected void registerServices( Kernel kernel ) {
        ws.exceptionToHttpCode.put( IllegalAccessException.class.getName(), 400 );

        kernel.register( "validatedWS", new TestValidatedWS() );
    }

    @Override
    protected List<String> getConfig() {
        return singletonList( "validation-services.conf" );
    }

    @Test
    public void testBrokenValidator() {
        assertGet( HTTP_URL( "/vaildation/service/methodWithBrokenValidator?requiredParameter=10" ) )
            .responded( 500, "CausedByException", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ),
                "CausedByException" );
    }

    @Test
    public void testWrongValidatorName() {
        assertGet( HTTP_URL( "/vaildation/service/methodWithWrongValidatorName?requiredParameter=10" ) )
            .responded( 500, "no such method wrongValidatorName", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ),
                "no such method wrongValidatorName" );
    }

    @Test
    public void testValidatorWithWrongParameters() {
        assertGet( HTTP_URL( "/vaildation/service/methodWithWrongValidatorArgs?requiredParameter=10" ) )
            .responded( 500, "missedParam required by validator wrongArgsValidatoris not supplied by web method",
                TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ),
                "missedParam required by validator wrongArgsValidatoris not supplied by web method" );
    }

    @Test
    public void testException() {
        assertGet( HTTP_URL( "/vaildation/service/exceptionRuntimeException" ) )
            .hasCode( 500 );
        assertGet( HTTP_URL( "/vaildation/service/exceptionIllegalAccessException" ) )
            .hasCode( 400 );
    }
}

