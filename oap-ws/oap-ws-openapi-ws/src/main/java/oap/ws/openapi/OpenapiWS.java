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

package oap.ws.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;

/**
 * Web service for openapi documentation
 */
@Slf4j
public class OpenapiWS {

    private final Openapi openapi;

    public OpenapiWS( Openapi openapi ) {
        this.openapi = openapi;
    }

    /**
     * Generates openapi documentation for all web services in appropriate oap-module.conf
     *
     * @return openapi documentation
     */
    @WsMethod( path = "/", method = GET, description = "Generates OpenAPI 3.0 json document" )
    @WsValidate( { "isValid" } )
    public OpenAPI openapi( @WsParam Optional<Boolean> skipDeprecated ) {
        return openapi.generateOpenApi( skipDeprecated.orElse( true ) );
    }

    @OpenapiIgnore
    public ValidationErrors isValid( Optional<Boolean> skipDeprecated ) {
        return ValidationErrors.empty();
    }
}
