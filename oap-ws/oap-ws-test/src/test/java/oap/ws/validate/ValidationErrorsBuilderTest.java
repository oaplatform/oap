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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    public void testValidationMessageConstantKeepsCodeAndText() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( BAD_REQUEST ).error( Message.LARGE ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( Pair.__( 1_000_020, "too large" ) );
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).error( Message.SMALL ) )
            .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testValidationMessageFormatsArguments() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( Message.T, Map.of( "id", "y" ) ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( Pair.__( 1_000_005, "item y" ) );
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).error( Message.SMALL, Map.of() ) )
            .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testMessageCodesAreKept() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( Message.MISSING ).endCode()
            .statusCode( NOT_FOUND ).errors( 1_000_002, List.of( "gone" ) ).endCode()
            .statusCode( NOT_FOUND ).pairs( List.of( Message.X ) ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( Pair.__( 1_000_001, "missing" ), Pair.__( 1_000_002, "gone" ), Pair.__( 1_000_003, "x" ) );
    }

    @Test
    public void testPublicCodeMustBeAboveLimit() {
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).error( Message.SMALL ) )
            .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).error( Message.LIMIT ) )
            .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).errors( 1, List.of( "a" ) ) )
            .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).pairs( List.of( Message.SMALL ) ) )
            .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testPlainErrorHasNoCode() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( BAD_REQUEST ).error( "no code" ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( Pair.__( null, "no code" ) );
    }

    @Test
    public void testInternalCodeMustBeAtMostLimit() {
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).internalError( 1_000_001, "big" ) )
            .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).internalErrors( 1_000_001, List.of( "a" ) ) )
            .isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> ValidationErrors.empty().statusCode( BAD_REQUEST ).internalError( 1_000_001, "big", Map.of() ) )
            .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testInternalCodesUpToLimitAreKept() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).internalError( 1001, "missing" ).endCode()
            .statusCode( NOT_FOUND ).internalErrors( 1_000_000, List.of( "limit" ) ).endCode()
            .statusCode( NOT_FOUND ).internalError( 2, "fmt ${id}", Map.of( "id", "x" ) ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( Pair.__( 1001, "missing" ), Pair.__( 1_000_000, "limit" ), Pair.__( 2, "fmt x" ) );
    }

    private enum Message implements ValidationMessage {
        LARGE( 1_000_020, "too large" ),
        SMALL( 5, "too small" ),
        MISSING( 1_000_001, "missing" ),
        X( 1_000_003, "x" ),
        T( 1_000_005, "item ${id}" ),
        LIMIT( 1_000_000, "limit" );

        private final int code;
        private final String message;

        Message( int code, String message ) {
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
