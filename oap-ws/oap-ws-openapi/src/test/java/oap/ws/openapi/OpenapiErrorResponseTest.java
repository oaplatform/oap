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
import oap.http.server.nio.HttpServerExchange.HttpMethod;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.validate.ValidationErrors;
import oap.ws.validate.WsValidate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenapiErrorResponseTest {
    private OpenAPI api;

    @BeforeClass
    public void generate() {
        OpenapiGenerator generator = new OpenapiGenerator( "title", "description" );
        generator.processWebservice( Fixture.class, "ctx" );
        api = generator.build();
    }

    @Test
    public void validationErrorsFromMethodValidatorAreReported() {
        ApiResponses responses = responses( "validated" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "400" );
        assertJsonError( responses.get( "400" ), "Bad Request" );
    }

    @Test
    public void validationErrorsFromMethodBodyAreReported() {
        ApiResponses responses = responses( "validationBody" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "403" );
        assertJsonError( responses.get( "403" ), "Forbidden" );
    }

    @Test
    public void voidMethodKeepsErrorResponsesFromValidator() {
        ApiResponses responses = responses( "voidWithError" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "403" );
        assertJsonError( responses.get( "403" ), "Forbidden" );
    }

    @Test
    public void build401IsReferencedAsUnauthorizedComponent() {
        ApiResponses responses = responses( "unauthorized" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "401" );
        assertThat( responses.get( "401" ).get$ref() ).isEqualTo( "#/components/responses/UnauthorizedError" );
    }

    @Test
    public void build403IsReported() {
        ApiResponses responses = responses( "forbidden" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "403" );
        assertJsonError( responses.get( "403" ), "Forbidden" );
    }

    @Test
    public void build404IsReported() {
        ApiResponses responses = responses( "missing" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "404" );
        assertJsonError( responses.get( "404" ), "Not Found" );
    }

    @Test
    public void httpResponseCodeIsReported() {
        ApiResponses responses = responses( "upstream" );

        assertThat( responses.keySet() ).containsExactlyInAnyOrder( "200", "409" );
        assertJsonError( responses.get( "409" ), "Conflict" );
    }

    @Test
    public void errorResponseExampleCarriesMessages() {
        assertThat( responses( "validated" ).get( "400" ).getContent().get( "application/json" ).getExample() )
            .isEqualTo( Map.of( "messages", List.of( Map.of( "message", "bad code" ) ) ) );
    }

    @Test
    public void methodWithoutErrorSourcesHasOnlySuccess() {
        assertThat( responses( "success" ).keySet() ).containsExactly( "200" );
    }

    private ApiResponses responses( String name ) {
        return api.getPaths().get( "/ctx/" + name ).getGet().getResponses();
    }

    private static void assertJsonError( ApiResponse response, String description ) {
        assertThat( response.getDescription() ).isEqualTo( description );
        assertThat( response.getContent().get( "application/json" ).getSchema().get$ref() )
            .isEqualTo( "#/components/schemas/ErrorResponse" );
    }

    public static class Fixture {
        @WsMethod( path = "/validated", method = HttpMethod.GET )
        @WsValidate( "validateCode" )
        public Response validated() {
            return Response.ok();
        }

        @WsMethod( path = "/validationBody", method = HttpMethod.GET )
        public Response validationBody() {
            ValidationErrors.empty().statusCode( 403 ).errors( List.of( "denied" ) ).endCode().throwIfInvalid();
            return Response.ok();
        }

        @WsMethod( path = "/voidWithError", method = HttpMethod.GET )
        @WsValidate( "validateForbidden" )
        public void voidWithError() {
        }

        @WsMethod( path = "/unauthorized", method = HttpMethod.GET )
        public Response unauthorized() {
            return Response.build401().message( "no token" ).build();
        }

        @WsMethod( path = "/forbidden", method = HttpMethod.GET )
        public Response forbidden() {
            return Response.build403().message( "no access" ).build();
        }

        @WsMethod( path = "/missing", method = HttpMethod.GET )
        public Response missing() {
            return Response.build404().message( "no item" ).build();
        }

        @WsMethod( path = "/upstream", method = HttpMethod.GET )
        public void upstream() {
            // the constructed response is discarded; only its literal status code matters to the scanner
            new oap.http.Response( "http://upstream", 409, "Conflict", List.of() );
        }

        @WsMethod( path = "/success", method = HttpMethod.GET )
        public Response success() {
            return Response.ok();
        }

        @OpenApiIgnore
        public ValidationErrors validateCode() {
            return ValidationErrors.empty().statusCode( 400 ).error( "bad code" ).endCode();
        }

        @OpenApiIgnore
        public ValidationErrors validateForbidden() {
            return ValidationErrors.empty().statusCode( 403 ).errors( List.of( "denied" ) ).endCode();
        }
    }
}
