package oap.ws.validate;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.Http;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.util.Mergeable;
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
    public static final int DEFAULT_CODE = BAD_REQUEST;

    private static final List<Integer> PRIORITY_CODES = List.of( UNAUTHORIZED, FORBIDDEN, BAD_REQUEST, NOT_FOUND );

    public final HashMap<Integer, LinkedHashSet<String>> errors = new HashMap<>();

    private ValidationErrors() {
    }

    private ValidationErrors( int code, Collection<String> errors ) {
        add( code, errors );
    }

    public static ValidationErrors empty() {
        return new ValidationErrors();
    }

    public static ValidationErrors error( String error ) {
        return errors( List.of( error ) );
    }

    public static ValidationErrors error( String message, Object... args ) {
        return errors( List.of( String.format( message, args ) ) );
    }

    public static ValidationErrors error( int code, String error ) {
        return errors( code, List.of( error ) );
    }

    public static ValidationErrors error( int code, String message, Object... args ) {
        return errors( code, List.of( String.format( message, args ) ) );
    }

    public static ValidationErrors errors( List<String> errors ) {
        return new ValidationErrors( DEFAULT_CODE, errors );
    }

    public static ValidationErrors errors( int code, List<String> errors ) {
        return new ValidationErrors( code, errors );
    }

    @Deprecated
    public static ValidationErrors create( List<String> errors ) {
        return new ValidationErrors( DEFAULT_CODE, errors );
    }

    @Deprecated
    public static ValidationErrors create( String error ) {
        return errors( List.of( error ) );
    }

    @Deprecated
    public static ValidationErrors create( int code, List<String> errors ) {
        return new ValidationErrors( code, errors );
    }

    @Deprecated
    public static ValidationErrors create( int code, String error ) {
        return errors( code, Lists.of( error ) );
    }

    public ValidationErrors merge( ValidationErrors otherErrors ) {
        otherErrors.errors.forEach( this::add );

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
        return !errors.isEmpty();
    }

    public boolean hasNonDefaultCode() {
        return errors.keySet().stream().anyMatch( code -> code != DEFAULT_CODE );
    }

    public ValidationErrors throwIfInvalid() throws WsClientException {
        if( failed() ) {
            List<String> messages = resolvedErrors();
            throw new WsClientException( messages.size() > 1 ? "validation failed" : messages.getFirst(), resolvedCode(), messages );
        }
        return this;
    }

    /**
     * Single status code for the whole result: 401, 403, 400, 404, other 4xx, 502, then any other code.
     */
    public int resolvedCode() {
        normalize();
        for( int code : PRIORITY_CODES ) {
            if( errors.containsKey( code ) ) return code;
        }
        List<Integer> rest = errors.keySet().stream().sorted().toList();
        return rest.stream().filter( code -> code >= 400 && code < 500 ).findFirst()
            .or( () -> rest.stream().filter( code -> code == Http.StatusCode.BAD_GATEWAY ).findFirst() )
            .orElseGet( () -> rest.isEmpty() ? DEFAULT_CODE : rest.getFirst() );
    }

    public List<String> resolvedErrors() {
        return List.copyOf( errors.getOrDefault( resolvedCode(), new LinkedHashSet<>() ) );
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    private void add( int code, Collection<String> messages ) {
        if( messages.isEmpty() ) return;
        errors.computeIfAbsent( code, c -> new LinkedHashSet<>() ).addAll( messages );
    }

    private void normalize() {
        List<Integer> fourXx = codesIn( 400, 500 );
        if( fourXx.size() > 1 ) mergeCodes( fourXx, DEFAULT_CODE );

        List<Integer> fiveXx = codesIn( 500, 600 );
        if( fiveXx.size() > 1 ) mergeCodes( fiveXx, Http.StatusCode.BAD_GATEWAY );
    }

    private List<Integer> codesIn( int from, int to ) {
        return errors.keySet().stream().filter( code -> code >= from && code < to ).toList();
    }

    private void mergeCodes( List<Integer> codes, int target ) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for( int code : codes ) merged.addAll( errors.remove( code ) );
        add( target, merged );
    }

    @EqualsAndHashCode
    @ToString
    public static class ErrorResponse implements Serializable {
        public final LinkedHashSet<String> errors = new LinkedHashSet<>();

        public ErrorResponse( Collection<String> errors ) {
            this.errors.addAll( errors );
        }
    }
}
