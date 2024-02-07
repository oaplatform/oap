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

package oap.ws.admin;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import oap.application.Kernel;
import oap.http.Http;
import oap.jpath.JPath;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.QUERY;

@Slf4j
public class JPathWS {
    private final Kernel kernel;

    public JPathWS( Kernel kernel ) {
        this.kernel = kernel;
    }

    @SuppressWarnings( "unchecked" )
    @WsMethod( method = GET, path = "/" )
    public Response get( @WsParam( from = QUERY ) String query ) {
        log.debug( "query = {}", query );
        try {
            AtomicReference<Object> result = new AtomicReference<>();
            JPath.evaluate( query, ( Map<String, Object> ) ( Object ) kernel.services.moduleMap, pointer -> result.set( pointer.get() ) );
            return Response.jsonOk().withBody( result.get(), false );
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
            return new Response( Http.StatusCode.BAD_REQUEST, e.getMessage(), Http.ContentType.TEXT_PLAIN ).withBody( Throwables.getStackTraceAsString( e ), true );
        }
    }
}
