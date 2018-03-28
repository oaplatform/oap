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
import oap.util.Lists;
import oap.util.Mergeable;
import oap.ws.WsClientException;

import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

@ToString
@EqualsAndHashCode
public class ValidationErrors implements Mergeable<ValidationErrors> {
    public static final int DEFAULT_CODE = HTTP_BAD_REQUEST;
    public final List<String> errors = new ArrayList<>();
    public int code;

    private ValidationErrors( int code, List<String> errors ) {
        this.code = code;
        this.errors.addAll( errors );
    }

    public static ValidationErrors empty() {
        return errors( Lists.empty() );
    }

    @Deprecated
    public static ValidationErrors create( String error ) {
        return errors( Lists.of( error ) );
    }

    public static ValidationErrors error( String error ) {
        return errors( Lists.of( error ) );
    }

    @Deprecated
    public static ValidationErrors create( List<String> errors ) {
        return new ValidationErrors( DEFAULT_CODE, errors );
    }

    public static ValidationErrors errors( List<String> errors ) {
        return new ValidationErrors( DEFAULT_CODE, errors );
    }

    @Deprecated
    public static ValidationErrors create( int code, List<String> errors ) {
        return new ValidationErrors( code, errors );
    }

    public static ValidationErrors errors( int code, List<String> errors ) {
        return new ValidationErrors( code, errors );
    }

    @Deprecated
    public static ValidationErrors create( int code, String error ) {
        return errors( code, Lists.of( error ) );
    }

    public static ValidationErrors error( int code, String error ) {
        return errors( code, Lists.of( error ) );
    }

    public ValidationErrors merge( ValidationErrors result ) {
        if( hasDefaultCode() ) this.code = result.code;
        this.errors.addAll( result.errors );
        return this;
    }

    public boolean isFailed() {
        return !errors.isEmpty();
    }

    public boolean hasDefaultCode() {
        return code == DEFAULT_CODE;
    }

    public void throwIfInvalid() throws WsClientException {
        if( isFailed() )
            throw new WsClientException( errors.size() > 1 ? "validation failed" : errors.get( 0 ), code, errors );
    }
}
