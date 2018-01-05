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

import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;

import static oap.http.Request.HttpMethod.GET;
import static oap.ws.WsParam.From.QUERY;

class TestValidatedWS {


    @WsMethod( method = GET )
    @WsValidate( "brokenValidator" )
    public int methodWithBrokenValidator( @WsParam( from = QUERY ) int requiredParameter ) {
        return requiredParameter;
    }

    @WsMethod( method = GET )
    @WsValidate( "wrongArgsValidator" )
    public int methodWithWrongValidatorArgs( @WsParam( from = QUERY ) int requiredParameter ) {
        return requiredParameter;
    }

    @WsMethod( method = GET )
    public int exceptionIllegalAccessException() throws IllegalAccessException {
        throw new IllegalAccessException( "" );
    }

    @WsMethod( method = GET )
    public int exceptionRuntimeException() {
        throw new RuntimeException( "" );
    }

    @WsMethod( method = GET )
    @WsValidate( "wrongValidatorName" )
    public int methodWithWrongValidatorName( @WsParam( from = QUERY ) int requiredParameter ) {
        return requiredParameter;
    }

    public ValidationErrors brokenValidator( int requiredParameter ) {
        throw new IllegalStateException( "CausedByException" );
    }

    public ValidationErrors wrongArgsValidator( int missedParam ) {
        return ValidationErrors.empty();
    }

}
