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
package oap.ws.validate;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.http.Http;
import oap.reflect.Reflection;
import oap.util.Lists;
import oap.util.Mergeable;
import oap.ws.WsClientException;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static oap.util.Lists.concat;
import static oap.ws.validate.Validators.forParameter;

@ToString
@EqualsAndHashCode
@Immutable
public final class ValidationErrors implements Mergeable<ValidationErrors> {
    public static final int DEFAULT_CODE = Http.StatusCode.BAD_REQUEST;
    public final List<String> errors;
    public final int code;

    private ValidationErrors( int code, List<String> errors ) {
        this.code = code;
        this.errors = Lists.distinct( List.copyOf( errors ) );
    }

    public static ValidationErrors empty() {
        return errors( List.of() );
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
        return new ValidationErrors(
            hasDefaultCode() ? otherErrors.code : this.code, concat( this.errors, otherErrors.errors ) );
    }

    public ValidationErrors validateParameters( Map<Reflection.Parameter, Object> values, Reflection.Method method, Object instance, boolean beforeUnmarshaling ) {
        var ret = ValidationErrors.empty();

        for( var entry : values.entrySet() ) {
            ret = ret.merge( forParameter( method, entry.getKey(), instance, beforeUnmarshaling )
                .validate( entry.getValue(), values ) );
        }

        return ret;
    }

    public boolean failed() {
        return !errors.isEmpty();
    }

    public boolean hasDefaultCode() {
        return code == DEFAULT_CODE;
    }

    @Deprecated
    public ValidationErrors throwIfInvalid() throws WsClientException {
        if( failed() )
            throw new WsClientException( errors.size() > 1 ? "validation failed" : errors.get( 0 ), code, errors );
        return this;
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    @EqualsAndHashCode
    @ToString
    public static class ErrorResponse implements Serializable {
        public final List<String> errors;

        public ErrorResponse( List<String> errors ) {
            this.errors = errors;
        }
    }
}
