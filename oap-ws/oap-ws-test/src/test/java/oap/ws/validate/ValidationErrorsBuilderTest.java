package oap.ws.validate;

import oap.json.Binder;
import oap.validation.ValidationMessage;
import oap.ws.ErrorResponse;
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
            .statusCode( FORBIDDEN ).error( "b" ).endCode();

        assertThat( errors.resolvedCode() ).isEqualTo( FORBIDDEN );
        assertThat( errors.resolvedErrors().stream().map( m -> m.message ).toList() ).containsExactlyInAnyOrder( "b" );
    }

    @Test
    public void testChainedStatusCodesMergeFourXxIntoBadRequest() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( CONFLICT ).error( "a" ).endCode()
            .statusCode( UNPROCESSABLE_ENTITY ).error( "b" ).endCode();

        assertThat( errors.resolvedCode() ).isEqualTo( BAD_REQUEST );
        assertThat( errors.resolvedErrors().stream().map( m -> m.message ).toList() ).containsExactlyInAnyOrder( "a", "b" );
    }

    @Test
    public void testUnauthorizedWinsOverOtherCodes() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( "missing" ).endCode()
            .statusCode( FORBIDDEN ).error( "f" ).endCode()
            .statusCode( UNAUTHORIZED ).error( "no token" ).endCode();

        assertThat( errors.resolvedCode() ).isEqualTo( UNAUTHORIZED );
        assertThat( errors.resolvedErrors().stream().map( m -> m.message ).toList() ).containsExactly( "no token" );
    }

    @Test
    public void testErrorFormatsArguments() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( null, "item ${id} not found", Map.of( "id", "x" ), null ).endCode();

        assertThat( errors.resolvedCode() ).isEqualTo( NOT_FOUND );
        assertThat( errors.resolvedErrors().stream().map( m -> m.message ).toList() ).containsExactly( "item x not found" );
    }

    @Test
    public void testValidationMessageConstantKeepsCodeAndText() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( BAD_REQUEST ).error( Message.LARGE, null ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( new ErrorResponse.Message( "1000020", "too large" ) );
    }

    @Test
    public void testValidationMessageFormatsArguments() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( Message.T, Map.of( "id", "y" ), null ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( new ErrorResponse.Message( "1000005", "item y" ) );
    }

    @Test
    public void testMessageCodesAreKept() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( NOT_FOUND ).error( Message.MISSING, null ).endCode()
            .statusCode( NOT_FOUND ).error( "1000002", "gone", Map.of(), null ).endCode()
            .statusCode( NOT_FOUND ).error( Message.X, null ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly(
            new ErrorResponse.Message( "1000001", "missing" ),
            new ErrorResponse.Message( "1000002", "gone" ),
            new ErrorResponse.Message( "1000003", "x" ) );
    }

    @Test
    public void testPlainErrorHasNoCode() {
        ValidationErrors errors = ValidationErrors.empty()
            .statusCode( BAD_REQUEST ).error( "no code" ).endCode();

        assertThat( errors.resolvedErrors() ).containsExactly( new ErrorResponse.Message( null, "no code" ) );
    }

    private enum Message implements ValidationMessage {
        LARGE( "1000020", "too large" ),
        MISSING( "1000001", "missing" ),
        X( "1000003", "x" ),
        T( "1000005", "item ${id}" );

        private final String code;
        private final String message;

        Message( String code, String message ) {
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

    @Test
    public void testEndCodeReturnsParent() {
        ValidationErrors parent = ValidationErrors.empty();

        assertThat( parent.statusCode( BAD_REQUEST ).endCode() ).isSameAs( parent );
    }

    @Test
    public void testErrorResponseRoundTrip() {
        ValidationErrors.ErrorResponse body = ValidationErrors.ErrorResponse.of(
            List.of( new ErrorResponse.Message( "1001", "x" ), new ErrorResponse.Message( null, "y" ) ) );

        String json = Binder.json.marshal( body );

        assertThat( json ).doesNotContain( "null" );
        assertThat( Binder.json.unmarshal( ValidationErrors.ErrorResponse.class, json ).messages ).containsExactlyElementsOf( body.messages );
    }
}
