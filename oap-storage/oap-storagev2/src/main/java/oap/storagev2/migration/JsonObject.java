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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static oap.util.Lists.headOf;
import static oap.util.Lists.tailOf;

@Slf4j
public class JsonObject extends Json<Map<String, Object>> {
    public final Map<String, Object> underlying;

    public JsonObject( Optional<String> field, Optional<Json<?>> parent, Map<String, Object> underlying ) {
        super( underlying, field, parent );
        this.underlying = underlying;
    }

    public Optional<Json<?>> field( String name ) {
        Object o = underlying.get( name );
        return map( Optional.of( name ), o, Optional.of( this ) );
    }

    private boolean rename( JsonObject root, JsonObject parent, List<String> oldName, List<String> newName ) {
        if( oldName.size() > 1 ) {
            String oldFirst = headOf( oldName ).orElseThrow();
            String newFirst = headOf( newName ).orElseThrow();
            boolean equals = oldFirst.equals( newFirst ) && root == parent;

            Optional<Json<?>> v = parent.field( oldFirst );
            if( v.isEmpty() ) return false;

            Json<?> json = v.get();

            if( json instanceof JsonObject ) {
                JsonObject jo = ( JsonObject ) json;
                return rename(
                    equals ? jo : root,
                    jo,
                    tailOf( oldName ),
                    equals ? tailOf( newName ) : newName
                );
            } else if( json instanceof JsonArray ) {
                if( !equals ) {
                    log.error( "array chains" );
                    return false;
                }

                JsonArray jsonA = ( JsonArray ) json;
                jsonA
                    .stream()
                    .filter( j -> j instanceof JsonObject )
                    .map( j -> ( JsonObject ) j )
                    .forEach( jo -> rename( jo, jo, tailOf( oldName ), tailOf( newName ) ) );
            } else {
                return false;
            }
        } else {
            String oldField = oldName.get( oldName.size() - 1 );
            Optional<Json<?>> field = parent.field( oldField );
            field.ifPresent( f -> {
                parent.deleteField( oldField );

                JsonObject np = root;
                for( String nf : newName.subList( 0, newName.size() - 1 ) ) {
                    JsonObject finalParent = np;
                    np = finalParent.objectOpt( nf ).orElseGet( () -> {
                        JsonObject jsonObject = new JsonObject( Optional.of( nf ), Optional.of( finalParent ), new HashMap<>() );
                        finalParent.underlying.put( nf, jsonObject.underlying );
                        return jsonObject;
                    } );

                    finalParent.underlying.put( nf, np.underlying );
                }

                np.underlying.put( newName.get( newName.size() - 1 ), f.underlying );
            } );
        }

        return true;
    }

    public JsonObject rename( String oldName, String newName ) {
        rename(
            this,
            this,
            asList( StringUtils.split( oldName, '.' ) ),
            asList( StringUtils.split( newName, '.' ) )
        );

        return this;
    }

    public JsonObject deleteField( String field ) {
        field( field ).ifPresent( ff -> underlying.remove( field ) );
        return this;
    }

    @SuppressWarnings( "unchecked" )
    public <I extends Json, O extends Json> JsonObject map( String field, Function<I, O> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( I ) in ).underlying ) );

        return this;
    }

    public Optional<String> stringOpt( String field ) {
        Optional<Json<?>> f = field( field );
        return f.filter( ff -> ff.underlying instanceof String ).map( ff -> ( String ) ff.underlying );
    }

    public Optional<Long> longOpt( String field ) {
        Optional<Json<?>> f = field( field );
        return f.filter( ff -> ff.underlying instanceof Number ).map( ff -> ( ( Number ) ff.underlying ).longValue() );
    }

    public Optional<JsonObject> objectOpt( String field ) {
        Optional<Json<?>> f = field( field );
        return f.filter( ff -> ff.underlying instanceof Map ).map( ff -> ( JsonObject ) ff );
    }

    public String stringField( String field ) {
        return stringOpt( field ).orElseThrow( () -> new MigrationException( "String field " + field + " not found" ) );
    }

    public long longField( String field ) {
        return longOpt( field ).orElseThrow( () -> new MigrationException( "String field " + field + " not found" ) );
    }

    public <T> JsonObject mapString( String field, Function<String, T> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( String ) in.underlying ) ) );

        return this;
    }

    public <T> JsonObject mapLong( String field, Function<Long, T> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( Long ) in.underlying ) ) );

        return this;
    }

    public <T> JsonObject mapDouble( String field, Function<Double, T> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( Double ) in.underlying ) ) );

        return this;
    }

    public <T> JsonObject mapDateTime( String field, Function<DateTime, T> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( DateTime ) in.underlying ) ) );

        return this;
    }
}
