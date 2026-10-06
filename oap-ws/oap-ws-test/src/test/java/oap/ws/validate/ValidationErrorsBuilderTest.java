package oap.ws.validate;

import oap.util.Pair;
import oap.json.Binder;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static oap.http.Http.StatusCode.BAD_REQUEST;
import static oap.http.Http.StatusCode.CONFLICT;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.NOT_FOUND;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.http.Http.StatusCode.UNPROCESSABLE_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

public class ValidationErrorsBuilderTest {
    @Test
    public void testChainedStatusCodesForbiddenWinsOverBadRequest() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( BAD_REQUEST ).error( "a" ).endCode()
            .statusCode( FORBIDDEN ).errors( List.of( "b" ) ).endCode();

        assertThat( errors.resolvedCode() ).isEqualTo( FORBIDDEN );
        assertThat( errors.resolvedErrors().stream().map( p -> p._2 ).toList() ).containsExactlyInAnyOrder( "b" );
    }

    @Test
    public void testChainedStatusCodesMergeFourXxIntoBadRequest() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( CONFLICT ).error( "a" ).endCode()
            .statusCode( UNPROCESSABLE_ENTITY ).error( "b" ).endCode();

        assertThat( errors.resolvedCode() ).isEqualTo( BAD_REQUEST );
        assertThat( errors.resolvedErrors().stream().map( p -> p._2 ).toList() ).containsExactlyInAnyOrder( "a", "b" );
    }

    @Test
    public void testUnauthorizedWinsOverOtherCodes() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( "missing" ).endCode()
            .statusCode( FORBIDDEN ).error( "f" ).endCode()
            .statusCode( UNAUTHORIZED ).error( "no token" ).endCode();

        assertThat( errors.resolvedCode() ).isEqualTo( UNAUTHORIZED );
        assertThat( errors.resolvedErrors().stream().map( p -> p._2 ).toList() ).containsExactly( "no token" );
    }

    @Test
    public void testErrorFormatsArguments() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( "item ${id} not found", Map.of( "id", "x" ) ).endCode();

        assertThat( errors.resolvedCode() ).isEqualTo( NOT_FOUND );
        assertThat( errors.resolvedErrors().stream().map( p -> p._2 ).toList() ).containsExactly( "item x not found" );
    }

    @Test
    public void testErrorFormatsArgumentsWithMessageCode() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( 1004, "item ${id} not found", Map.of( "id", "y" ) ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( Pair.__( 1004, "item y not found" ) );
    }

    @Test
    public void testMessageCodesAreKept() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( 1001, "missing" ).endCode()
            .statusCode( NOT_FOUND ).errors( 1002, List.of( "gone" ) ).endCode()
            .statusCode( NOT_FOUND ).pairs( List.of( Pair.__( 1003, "x" ) ) ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( Pair.__( 1001, "missing" ), Pair.__( 1002, "gone" ), Pair.__( 1003, "x" ) );
    }

    @Test
    public void testEndCodeReturnsParent() {
        ValidationErrors parent = ValidationErrors.empty();

        assertThat( parent.statusCode( BAD_REQUEST ).endCode() ).isSameAs( parent );
    }

    @Test
    public void testErrorResponseRoundTrip() {
        ValidationErrors.ErrorResponse body = ValidationErrors.ErrorResponse.of( List.of( Pair.__( 1001, "x" ), Pair.__( null, "y" ) ) );

        String json = Binder.json.marshal( body );

        assertThat( json ).doesNotContain( "null" );
        assertThat( Binder.json.unmarshal( ValidationErrors.ErrorResponse.class, json ).messages ).containsExactlyElementsOf( body.messages );
    }
}
