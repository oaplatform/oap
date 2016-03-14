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

package oap.storage.migration;

import org.joda.time.DateTime;

import java.util.*;
import java.util.function.Function;

/**
 * Created by Igor Petrenko on 14.03.2016.
 */
public class JsonObject extends Json<Map<String, Object>> {
    public final Map<String, Object> underlying;

    public JsonObject( Optional<String> field, Optional<Json<?>> parent, Map<String, Object> underlying ) {
        super( underlying, field, parent );
        this.underlying = underlying;
    }

    public Optional<Json<?>> field( String name ) {
        final Object o = underlying.get( name );
        return map( Optional.of( name ), o, Optional.of( this ) );
    }

    public JsonObject rename( String oldName, String newName ) {
        final Optional<Json<?>> field = field( oldName );
        field.ifPresent( f -> {
            underlying.remove( oldName );
            traverseOrCreateParent( newName ).underlying.put( getName( newName ), f.underlying );
        } );

        return this;
    }

    private String getName( String newName ) {
        final int i = newName.lastIndexOf( '.' );
        return i > 0 ? newName.substring( i + 1 ) : newName;
    }

    private JsonObject traverseOrCreateParent( String path ) {
        final List<String> strings = Arrays.asList( path.split( "\\." ) );
        JsonObject parent = this;
        for( String f : strings.subList( 0, strings.size() - 1 ) ) {
            final JsonObject finalParent = parent;
            parent = objOpt( f ).orElseGet( () -> {
                final JsonObject jsonObject = new JsonObject( Optional.of( f ), Optional.of( finalParent ), new HashMap<>() );
                finalParent.underlying.put( f, jsonObject.underlying );
                return jsonObject;
            } );
        }
        return parent;
    }

    public JsonObject deleteField( String field ) {
        final Optional<Json<?>> f = field( field );
        f.ifPresent( ff -> underlying.remove( field ) );
        return this;
    }

    @SuppressWarnings( "unchecked" )
    public <TIn extends Json, TOut extends Json> JsonObject map( String field, Function<TIn, TOut> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( TIn ) in ).underlying ) );

        return this;
    }

    public Optional<String> sOpt( String field ) {
        final Optional<Json<?>> f = field( field );
        return f.filter( ff -> ff.underlying instanceof String ).map( ff -> ( String ) ff.underlying );
    }

    public Optional<Long> lOpt( String field ) {
        final Optional<Json<?>> f = field( field );
        return f.filter( ff -> ff.underlying instanceof Number ).map( ff -> ( ( Number ) ff.underlying ).longValue() );
    }

    public Optional<JsonObject> objOpt( String field ) {
        final Optional<Json<?>> f = field( field );
        return f.filter( ff -> ff.underlying instanceof Map ).map( ff -> ( JsonObject ) ff );
    }

    public String s( String field ) {
        return sOpt( field ).orElseThrow( () -> new FileStorageMigrationException( "String field " + field + " not found" ) );
    }

    public long l( String field ) {
        return lOpt( field ).orElseThrow( () -> new FileStorageMigrationException( "String field " + field + " not found" ) );
    }

    public <T> JsonObject mapS( String field, Function<String, T> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( String ) in.underlying ) ) );

        return this;
    }

    public <T> JsonObject mapL( String field, Function<Long, T> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( Long ) in.underlying ) ) );

        return this;
    }

    public <T> JsonObject mapD( String field, Function<Double, T> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( Double ) in.underlying ) ) );

        return this;
    }

    public <T> JsonObject mapDateTime( String field, Function<DateTime, T> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( DateTime ) in.underlying ) ) );

        return this;
    }
}
