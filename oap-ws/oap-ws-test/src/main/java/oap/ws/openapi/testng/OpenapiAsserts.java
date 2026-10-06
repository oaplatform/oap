package oap.ws.openapi.testng;

import oap.json.Binder;
import oap.ws.validate.ValidationMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static oap.http.Http.StatusCode.OK;
import static oap.http.test.HttpAsserts.assertGet;
import static org.assertj.core.api.Assertions.assertThat;

public final class OpenapiAsserts {
    private OpenapiAsserts() {
    }

    /**
     * Fetches the openapi document at {@code openapiUrl}, asserts it is served with status 200, and returns an
     * assertion over the error messages it contains.
     */
    public static OpenapiAssertion assertOpenApi( String openapiUrl ) {
        List<Map<String, Object>> messages = new ArrayList<>();
        assertGet( openapiUrl )
            .hasCode( OK )
            .satisfies( response -> messages.addAll( errorMessages( response.contentString() ) ) );

        return new OpenapiAssertion( openapiUrl, messages );
    }

    /**
     * Error messages of all operations in the openapi document, one {@code {code, message}} map per occurrence;
     * {@code code} is a {@code Long}.
     */
    @SuppressWarnings( "unchecked" )
    public static List<Map<String, Object>> errorMessages( String openapiJson ) {
        Map<String, Object> openapi = Binder.json.unmarshal( Map.class, openapiJson );
        List<Map<String, Object>> result = new ArrayList<>();

        Map<String, Map<String, Object>> paths = ( Map<String, Map<String, Object>> ) openapi.getOrDefault( "paths", Map.of() );
        for( Map<String, Object> path : paths.values() ) {
            for( Object operation : path.values() ) {
                if( !( operation instanceof Map<?, ?> op ) ) continue;
                Map<String, Map<String, Object>> responses = ( Map<String, Map<String, Object>> ) op.get( "responses" );
                if( responses == null ) continue;
                for( Map<String, Object> response : responses.values() ) {
                    Map<String, Map<String, Object>> content = ( Map<String, Map<String, Object>> ) response.get( "content" );
                    if( content == null ) continue;
                    for( Map<String, Object> mediaType : content.values() ) {
                        if( !( mediaType.get( "example" ) instanceof Map<?, ?> example ) ) continue;
                        if( !( example.get( "messages" ) instanceof List<?> messages ) ) continue;
                        for( Object message : messages ) result.add( normalize( ( Map<String, Object> ) message ) );
                    }
                }
            }
        }

        return result;
    }

    private static Map<String, Object> normalize( Map<String, Object> message ) {
        Map<String, Object> result = new LinkedHashMap<>();
        if( message.get( "code" ) instanceof Number code ) result.put( "code", code.longValue() );
        result.put( "message", message.get( "message" ) );
        return result;
    }

    private static Map<String, Object> key( ValidationMessage message ) {
        return Map.of( "code", ( long ) message.code(), "message", message.message() );
    }

    public static final class OpenapiAssertion {
        private final String openapiUrl;
        private final List<Map<String, Object>> messages;

        private OpenapiAssertion( String openapiUrl, List<Map<String, Object>> messages ) {
            this.openapiUrl = openapiUrl;
            this.messages = messages;
        }

        /** Each message occurs at least once in the document's error messages. */
        public OpenapiAssertion containsValidationMessage( ValidationMessage... expected ) {
            for( ValidationMessage message : expected ) {
                assertThat( messages )
                    .as( "%s: openapi error message %s", openapiUrl, message )
                    .contains( key( message ) );
            }
            return this;
        }

        /** None of the messages occurs in the document's error messages. */
        public OpenapiAssertion doesNotContainValidationMessage( ValidationMessage... unexpected ) {
            for( ValidationMessage message : unexpected ) {
                assertThat( messages )
                    .as( "%s: openapi error message %s", openapiUrl, message )
                    .doesNotContain( key( message ) );
            }
            return this;
        }

        /** Each message occurs exactly once in the document's error messages. */
        public OpenapiAssertion containsOnlyOnceValidationMessage( ValidationMessage... expected ) {
            for( ValidationMessage message : expected ) {
                long occurrences = messages.stream().filter( key( message )::equals ).count();
                assertThat( occurrences )
                    .as( "%s: occurrences of openapi error message %s", openapiUrl, message )
                    .isEqualTo( 1L );
            }
            return this;
        }

        /** The document's error messages are exactly the given messages, in any order and each once. */
        public OpenapiAssertion containsExactlyInAnyOrderValidationMessage( ValidationMessage... expected ) {
            List<Map<String, Object>> keys = new ArrayList<>();
            for( ValidationMessage message : expected ) keys.add( key( message ) );

            assertThat( messages )
                .as( "%s: openapi error messages", openapiUrl )
                .containsExactlyInAnyOrderElementsOf( keys );
            return this;
        }
    }
}
