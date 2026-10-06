package oap.ws.validate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.Http;
import oap.reflect.Reflection;
import oap.util.Mergeable;
import oap.util.Pair;
import oap.ws.WsClientException;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static oap.http.Http.StatusCode.BAD_REQUEST;
import static oap.http.Http.StatusCode.FORBIDDEN;
import static oap.http.Http.StatusCode.NOT_FOUND;
import static oap.http.Http.StatusCode.UNAUTHORIZED;
import static oap.ws.validate.Validators.forParameter;

@ToString
@EqualsAndHashCode
public final class ValidationErrors implements Mergeable<ValidationErrors> {
    private static final List<Integer> PRIORITY_CODES = List.of( UNAUTHORIZED, FORBIDDEN, BAD_REQUEST, NOT_FOUND );

    /** HTTP status code to messages; each message is a pair of (message code, message). */
    public final HashMap<Integer, LinkedHashSet<Pair<Integer, String>>> messages = new HashMap<>();

    private ValidationErrors() {
    }

    public static ValidationErrors empty() {
        return new ValidationErrors();
    }

    /** Starts the messages of one HTTP status code; finish with {@link ValidationErrorsBuilder#endCode()}. */
    public ValidationErrorsBuilder statusCode( int httpStatusCode ) {
        return new ValidationErrorsBuilder( this, httpStatusCode );
    }

    public ValidationErrors merge( ValidationErrors otherErrors ) {
        otherErrors.messages.forEach( this::add );

        return this;
    }

    public ValidationErrors validateParameters( Map<Reflection.Parameter, Object> values, Reflection.Method method, Object instance, boolean beforeUnmarshaling ) {
        ValidationErrors ret = ValidationErrors.empty();

        for( Map.Entry<Reflection.Parameter, Object> entry : values.entrySet() ) {
            ret.merge( forParameter( method, entry.getKey(), instance, beforeUnmarshaling ).validate( entry.getValue(), values ) );
        }

        return ret;
    }

    public boolean failed() {
        return !messages.isEmpty();
    }

    public boolean hasNonDefaultCode() {
        return messages.keySet().stream().anyMatch( code -> code != BAD_REQUEST );
    }

    public ValidationErrors throwIfInvalid() throws WsClientException {
        if( failed() ) {
            List<Pair<Integer, String>> messages = resolvedErrors();
            int code = resolvedCode();
            String reason = Http.StatusCode.getReason( code );
            throw new WsClientException( reason, code, messages );
        }
        return this;
    }

    /**
     * Single status code for the whole result: 401, 403, 400, 404 (the first one present wins, its messages only),
     * then other 4xx (merged into 400), 502 (5xx merged), then any other code.
     */
    public int resolvedCode() {
        for( int code : PRIORITY_CODES ) {
            if( messages.containsKey( code ) ) return code;
        }
        normalize();
        List<Integer> rest = messages.keySet().stream().sorted().toList();
        return rest.stream().filter( code -> code >= 400 && code < 500 ).findFirst()
            .or( () -> rest.stream().filter( code -> code == Http.StatusCode.BAD_GATEWAY ).findFirst() )
            .orElseGet( () -> rest.isEmpty() ? BAD_REQUEST : rest.getFirst() );
    }

    /** Messages (message code, message) of the resolved status code. */
    public List<Pair<Integer, String>> resolvedErrors() {
        return List.copyOf( messages.getOrDefault( resolvedCode(), new LinkedHashSet<>() ) );
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    void add( int httpStatusCode, Collection<Pair<Integer, String>> messages ) {
        if( messages.isEmpty() ) return;
        this.messages.computeIfAbsent( httpStatusCode, c -> new LinkedHashSet<>() ).addAll( messages );
    }

    private void normalize() {
        List<Integer> fourXx = codesIn( 400, 500 );
        if( fourXx.size() > 1 ) mergeCodes( fourXx, BAD_REQUEST );

        List<Integer> fiveXx = codesIn( 500, 600 );
        if( fiveXx.size() > 1 ) mergeCodes( fiveXx, Http.StatusCode.BAD_GATEWAY );
    }

    private List<Integer> codesIn( int from, int to ) {
        return messages.keySet().stream().filter( code -> code >= from && code < to ).toList();
    }

    private void mergeCodes( List<Integer> codes, int target ) {
        LinkedHashSet<Pair<Integer, String>> merged = new LinkedHashSet<>();
        for( int code : codes ) merged.addAll( messages.remove( code ) );
        add( target, merged );
    }

    /** Validation failure body: {@code {"messages": [{"code": ..., "message": ...}]}}. */
    @EqualsAndHashCode
    @ToString
    public static class ErrorResponse implements Serializable {
        public final LinkedHashSet<oap.ws.ErrorResponse.Message> messages = new LinkedHashSet<>();

        @JsonCreator
        public ErrorResponse( @JsonProperty( "messages" ) Collection<oap.ws.ErrorResponse.Message> messages ) {
            this.messages.addAll( messages );
        }

        public static ErrorResponse of( Collection<Pair<Integer, String>> messages ) {
            return new ErrorResponse( messages.stream()
                .map( m -> new oap.ws.ErrorResponse.Message( m._1, m._2 ) )
                .toList() );
        }
    }
}
