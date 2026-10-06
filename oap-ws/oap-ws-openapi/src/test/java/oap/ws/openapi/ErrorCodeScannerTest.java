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

import oap.ws.Response;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.ValidationMessage;
import oap.ws.validate.WsValidate;
import org.testng.annotations.Test;

import java.util.List;
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
            new ErrorCodeScanner.ScannedMessage( 1_000_001, "a" ),
            new ErrorCodeScanner.ScannedMessage( null, null ),
            new ErrorCodeScanner.ScannedMessage( 1_000_003, "item ${id}" ),
            new ErrorCodeScanner.ScannedMessage( null, "ctx ${id}" ) );
        assertThat( responses.get( 404 ) ).isEmpty();
    }

    @Test
    public void testCollectsEnumMessages() throws NoSuchMethodException {
        SortedMap<Integer, Set<ErrorCodeScanner.ScannedMessage>> responses = scanner.errorResponses( method( "enumMessages" ) );

        assertThat( responses.get( 400 ) ).containsExactly( new ErrorCodeScanner.ScannedMessage( 1_000_010, "name is required" ) );
    }

    @Test
    public void testCollectsCodesFromMethodValidatorsAndResponses() throws NoSuchMethodException {
        assertThat( scanner.errorCodes( method( "web" ) ) ).containsExactly( 400, 401, 403, 404 );
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
                .statusCode( 400 ).error( Err.A ).endCode()
                .statusCode( 400 ).error( "a" + param, Map.of() ).endCode()
                .statusCode( 400 ).error( Err.B ).endCode()
                .statusCode( 400 ).error( "ctx ${id}", Map.of( "id", param ) ).endCode()
                .statusCode( 404 ).errors( 1_000_003, List.of( "gone" ) ).endCode();
        }

        public ValidationErrors enumMessages( String param ) {
            return ValidationErrors.empty().statusCode( 400 ).error( Err.NAME ).endCode();
        }

        public enum Err implements ValidationMessage {
            A( 1_000_001, "a" ),
            B( 1_000_003, "item ${id}" ),
            NAME( 1_000_010, "name is required" );

            private final int code;
            private final String message;

            Err( int code, String message ) {
                this.code = code;
                this.message = message;
            }

            @Override
            public int code() {
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
