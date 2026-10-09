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

import external.validation.ExternalValidators;
import oap.ws.Response;
import oap.ws.validate.ValidationErrors;
import oap.validation.ValidationMessage;
import oap.ws.validate.WsValidate;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorCodeScannerTest {
    private final ErrorCodeScanner scanner = new ErrorCodeScanner();

    private static java.lang.reflect.Method method( String name ) throws NoSuchMethodException {
        return Fixture.class.getMethod( name, String.class );
    }

    @Test
    public void testCollectsMessageCodesAndTexts() throws NoSuchMethodException {
        SortedMap<Integer, Set<ErrorCodeScanner.ScannedMessage>> responses = scanner.errorResponses( method( "messages" ) );

        assertThat( responses.get( 400 ) ).containsExactly(
            new ErrorCodeScanner.ScannedMessage( "1000001", "a" ),
            new ErrorCodeScanner.ScannedMessage( null, null ),
            new ErrorCodeScanner.ScannedMessage( "1000003", "item ${id}" ),
            new ErrorCodeScanner.ScannedMessage( null, "ctx ${id}" ) );
    }

    @Test
    public void testCollectsEnumMessages() throws NoSuchMethodException {
        SortedMap<Integer, Set<ErrorCodeScanner.ScannedMessage>> responses = scanner.errorResponses( method( "enumMessages" ) );

        assertThat( responses.get( 400 ) ).containsExactly( new ErrorCodeScanner.ScannedMessage( "1000010", "name is required" ) );
    }

    @Test
    public void testCollectsCodesFromMethodValidatorsAndResponses() throws NoSuchMethodException {
        assertThat( scanner.errorCodes( method( "web" ) ) ).containsExactly( 400, 401, 403, 404 );
    }

    @Test
    public void testFollowsStaticCallIntoExternalPackage() throws NoSuchMethodException {
        SortedMap<Integer, Set<ErrorCodeScanner.ScannedMessage>> responses = scanner.errorResponses( method( "external" ) );

        assertThat( responses.get( 400 ) ).containsExactly( new ErrorCodeScanner.ScannedMessage( null, "payload too large" ) );
    }

    @Test
    public void testSkipsSuccessCodesAndNonConstantCodes() throws NoSuchMethodException {
        assertThat( scanner.errorCodes( method( "builders" ) ) ).containsExactly( 403, 404 );
        assertThat( scanner.errorCodes( method( "okOnly" ) ) ).isEmpty();
        assertThat( scanner.errorCodes( method( "nonConstant" ) ) ).isEmpty();
    }

    public static class Fixture {
        @WsValidate( "validateCode" )
        public Response web( @WsValidate( "validateParam" ) String param ) {
            return switch( param ) {
                case "forbidden" -> Response.build403().message( "denied" ).build();
                case "missing" -> Response.build404().build();
                case "anonymous" -> Response.build401().message( "no token" ).build();
                case null, default -> Response.ok();
            };
        }

        public Response okOnly( String param ) {
            return Response.ok();
        }

        public ValidationErrors messages( String param ) {
            return ValidationErrors.empty()
                .statusCode( 400 ).error( Err.A, null ).endCode()
                .statusCode( 400 ).error( null, "a" + param, Map.of(), null ).endCode()
                .statusCode( 400 ).error( Err.B, null ).endCode()
                .statusCode( 400 ).error( null, "ctx ${id}", Map.of( "id", param ), null ).endCode();
        }

        public ValidationErrors enumMessages( String param ) {
            return ValidationErrors.empty().statusCode( 400 ).error( Err.NAME, null ).endCode();
        }

        public ValidationErrors external( String param ) {
            return ExternalValidators.tooLarge();
        }

        public enum Err implements ValidationMessage {
            A( "1000001", "a" ),
            B( "1000003", "item ${id}" ),
            NAME( "1000010", "name is required" );

            private final String code;
            private final String message;

            Err( String code, String message ) {
                this.code = code;
                this.message = message;
            }

            @Override
            public String code() {
                return code;
            }

            @Override
            public String message() {
                return message;
            }
        }

        public Response builders( String param ) {
            if( "forbidden".equals( param ) ) return Response.build403().message( "no access" ).build();
            return Response.build404().build();
        }

        public Response nonConstant( String param ) {
            int code = param.length();
            return new Response( code );
        }

        public ValidationErrors validateCode( String value ) {
            return ValidationErrors.empty().statusCode( 400 ).error( "bad code" ).endCode();
        }

        public ValidationErrors validateParam( String value ) {
            return ValidationErrors.empty().statusCode( value.length() ).error( "non-constant code" ).endCode();
        }
    }
}
