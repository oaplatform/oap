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
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import oap.ws.sso.WsSecurity;
import oap.ws.sso.interceptor.JWTSecurityInterceptor;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static dev.khbd.interp4j.core.Interpolations.s;
import static oap.http.Http.StatusCode.BAD_REQUEST;
import static oap.http.Http.StatusCode.CONFLICT;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.QUERY;
import static org.assertj.core.api.Assertions.assertThat;

public class OpenapiErrorResponseTest {
    private OpenAPI api;

    private static void assertJsonError( ApiResponse response, String description ) {
        assertThat( response.getDescription() ).isEqualTo( description );
        assertThat( response.getContent().get( "application/json" ).getSchema().get$ref() )
            .isEqualTo( "#/components/schemas/ErrorResponse" );
    }

    @BeforeClass
    public void testGenerate() {
        OpenapiGenerator generator = new OpenapiGenerator( "title", "description" );
        generator.processWebservice( Fixture.class, "ctx" );
        api = generator.build();
    }

    @Test
    public void testValidationErrorsFromMethodValidatorAreReported() {
        ApiResponses responses = responses( "validated" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "400" );
        assertJsonError( responses.get( "400" ), "Bad Request" );
    }

    @Test
    public void testValidationErrorsFromMethodBodyAreReported() {
        ApiResponses responses = responses( "validationBody" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "403" );
        assertJsonError( responses.get( "403" ), "Forbidden" );
    }

    @Test
    public void testVoidMethodKeepsErrorResponsesFromValidator() {
        ApiResponses responses = responses( "voidWithError" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "403" );
        assertJsonError( responses.get( "403" ), "Forbidden" );
    }

    @Test
    public void testBuild401IsReferencedAsUnauthorizedComponent() {
        ApiResponses responses = responses( "unauthorized" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "401" );
        assertThat( responses.get( "401" ).get$ref() ).isEqualTo( "#/components/responses/UnauthorizedError" );
    }

    @Test
    public void testBuild403IsReported() {
        ApiResponses responses = responses( "forbidden" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "403" );
        assertJsonError( responses.get( "403" ), "Forbidden" );
    }

    @Test
    public void testBuild404IsReported() {
        ApiResponses responses = responses( "missing" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "404" );
        assertJsonError( responses.get( "404" ), "Not Found" );
    }

    @Test
    public void testHttpResponseCodeIsReported() {
        ApiResponses responses = responses( "upstream" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "409" );
        assertJsonError( responses.get( "409" ), "Conflict" );
    }

    @Test
    public void testErrorResponseExampleCarriesMessages() {
        assertThat( responses( "validated" ).get( "400" ).getContent().get( "application/json" ).getExample() )
            .isEqualTo( Map.of( "messages", List.of( Map.of( "message", "bad code" ) ) ) );
    }

    @Test
    public void testMethodWithoutErrorSourcesHasOnlySuccess() {
        assertThat( responses( "success" ).keySet() ).containsExactly( "200" );
    }

    @Test
    public void testInnerValidationReportsStatusCodeCodeAndMessage() {
        OpenapiGenerator generator = new OpenapiGenerator( "title", "description" );
        generator.processWebservice( Fixture.class, "ctx", List.of( JWTSecurityInterceptor.class ) );
        generator.processWebservice( Fixture2.class, "ctx2", List.of( JWTSecurityInterceptor.class ) );
        OpenAPI localApi = generator.build();

        ApiResponses responses = localApi.getPaths().get( "/ctx/validateMethods" ).getGet().getResponses();
        ApiResponses responses2 = localApi.getPaths().get( "/ctx2/validateMethods2" ).getGet().getResponses();

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "400", "401", "403" );
        assertJsonError( responses.get( "400" ), "Bad Request" );
        assertThat( responses.get( "400" ).getContent().get( "application/json" ).getExample() )
            .isEqualTo( Map.of( "messages", List.of( Map.of( "code", 1023, "message", "${c} - v" ) ) ) );

        assertThat( responses2.keySet() ).containsExactlyInAnyOrder( "200", "400", "401", "403" );
        assertJsonError( responses2.get( "400" ), "Bad Request" );
        assertThat( responses2.get( "400" ).getContent().get( "application/json" ).getExample() )
            .isEqualTo( Map.of( "messages", List.of( Map.of( "message", "bad code" ) ) ) );
    }

    private ApiResponses responses( String name ) {
        return api.getPaths().get( s( "/ctx/${name}" ) ).getGet().getResponses();
    }

    public static class Fixture {
        public static final int ERROR_CODE_1 = 1023;

        @WsMethod( path = "/validated", method = GET )
        @WsValidate( "validateCode" )
        public Response validated() {
            return Response.ok();
        }

        @WsMethod( path = "/validationBody", method = GET )
        public Response validationBody() {
            ValidationErrors.empty().statusCode( FORBIDDEN ).errors( List.of( "denied" ) ).endCode().throwIfInvalid();
            return Response.ok();
        }

        @WsMethod( path = "/validateMethods", method = GET )
        @WsSecurity( permissions = "a:test" )
        public void testInnerValidation( @WsParam( from = QUERY ) int c ) {
            if( c > 1 ) {
                validateC( c );
            }
        }

        private void validateC( int c ) {
            for( int i = 10; i < 100; i++ ) {
                if( c > i ) {
                    ValidationErrors.empty().statusCode( BAD_REQUEST ).error( ERROR_CODE_1, "${c} - v", Map.of( "c", c ) ).endCode().throwIfInvalid();
                }
            }
        }

        @WsMethod( path = "/voidWithError", method = GET )
        @WsValidate( "validateForbidden" )
        public void voidWithError() {
        }

        @WsMethod( path = "/unauthorized", method = GET )
        public Response unauthorized() {
            return Response.build401().message( "no token" ).build();
        }

        @WsMethod( path = "/forbidden", method = GET )
        public Response forbidden() {
            return Response.build403().message( "no access" ).build();
        }

        @WsMethod( path = "/missing", method = GET )
        public Response missing() {
            return Response.build404().message( "no item" ).build();
        }

        @WsMethod( path = "/upstream", method = GET )
        public void upstream() {
            // the constructed response is discarded; only its literal status code matters to the scanner
            new oap.http.Response( "http://upstream", CONFLICT, "Conflict", List.of() );
        }

        @WsMethod( path = "/success", method = GET )
        public Response success() {
            return Response.ok();
        }

        @OpenApiIgnore
        public ValidationErrors validateCode() {
            return ValidationErrors.empty().statusCode( BAD_REQUEST ).error( "bad code" ).endCode();
        }

        @OpenApiIgnore
        public ValidationErrors validateForbidden() {
            return ValidationErrors.empty().statusCode( FORBIDDEN ).errors( List.of( "denied" ) ).endCode();
        }
    }

    public static class Fixture2 {
        private final Fixture fixture;

        public Fixture2( Fixture fixture ) {
            this.fixture = fixture;
        }

        @WsMethod( path = "/validateMethods2", method = GET )
        @WsSecurity( permissions = "a:test" )
        public void testInnerValidation2( @WsParam( from = QUERY ) int c ) {
            fixture.validateCode();
        }
    }
}
