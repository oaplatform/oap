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
import oap.application.Application;
import oap.concurrent.SynchronizedThread;
import oap.http.Handler;
import oap.http.HttpResponse;
import oap.http.PlainHttpListener;
import oap.http.Request;
import oap.http.Response;
import oap.http.Server;
import oap.http.cors.GenericCorsPolicy;
import oap.metrics.Metrics;
import oap.testng.Env;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static oap.http.testng.HttpAsserts.HTTP_PREFIX;
import static oap.http.testng.HttpAsserts.assertGet;
import static oap.http.testng.HttpAsserts.assertPost;
import static oap.http.testng.HttpAsserts.reset;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.testng.Assert.assertEquals;

@Slf4j
public class ValidationTest {
    private final Server server = new Server( 100 );
    private final WebServices ws = new WebServices( server, new SessionManager( 10, null, "/" ),
       GenericCorsPolicy.DEFAULT, WsConfig.CONFIGURATION.fromResource( getClass(), "validation-services.conf" )
    );

    private SynchronizedThread listener;

    @BeforeClass
    public void startServer() {
        Application.register( "validatedWS", new ValidatedWS() );
        ws.start();
        listener = new SynchronizedThread( new PlainHttpListener( server, Env.port() ) );
        listener.start();
    }

    @AfterClass
    public void stopServer() {
        listener.stop();
        server.stop();
        ws.stop();
        reset();
    }

    @Test
    public void testBrokenValidator() {
        assertGet( HTTP_PREFIX + "/vaildation/service/methodWithBrokenValidator?requiredParameter=10" )
            .responded( 500, "CausedByException", TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ),
                "CausedByException" );
    }

    @Test
    public void testValidatorWithWrongParameters() {
        assertGet( HTTP_PREFIX + "/vaildation/service/methodWithWrongValidatorArgs?requiredParameter=10" )
            .responded( 500, "missedParam required by validator wrongArgsValidatoris not supplied by web method",
                TEXT_PLAIN.withCharset( StandardCharsets.UTF_8 ),
                "missedParam required by validator wrongArgsValidatoris not supplied by web method" );
    }

}

