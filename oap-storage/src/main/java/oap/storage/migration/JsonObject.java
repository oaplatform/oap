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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static oap.util.Lists.head;
import static oap.util.Lists.tail;

@Slf4j
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

    public JsonObject mapScript( String javascript ) throws ScriptException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName( "nashorn" );
        engine.put( "obj", underlying );

        engine.eval( javascript );

        return this;
    }

    @SneakyThrows
    public JsonObject mapScriptFromResource( String resource ) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName( "nashorn" );
        engine.put( "obj", underlying );

        try( InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ) ) {
            engine.eval( new InputStreamReader( inputStream ) );
        }

        return this;
    }

    private boolean rename( JsonObject root, JsonObject parent, List<String> oldName, List<String> newName ) {
        if( oldName.size() > 1 ) {
            final String oldFirst = head( oldName );
            final String newFirst = head( newName );
            final boolean equals = oldFirst.equals( newFirst ) && root == parent;

            final Optional<Json<?>> v = parent.field( oldFirst );
            if( !v.isPresent() ) {
                return false;
            }

            final Json<?> json = v.get();

            if( json instanceof JsonObject ) {
                final JsonObject jo = ( JsonObject ) json;
                return rename(
                    equals ? jo : root,
                    jo,
                    tail( oldName ),
                    equals ? tail( newName ) : newName
                );
            } else if( json instanceof JsonArray ) {
                if( !equals ) {
                    log.error( "array chains" );
                    return false;
                }

                final JsonArray jsonA = ( JsonArray ) json;
                jsonA
                    .stream()
                    .filter( j -> j instanceof JsonObject )
                    .map( j -> ( JsonObject ) j )
                    .forEach( jo -> rename( jo, jo, tail( oldName ), tail( newName ) ) );
            } else {
                return false;
            }
        } else {
            final String oldField = oldName.get( oldName.size() - 1 );
            final Optional<Json<?>> field = parent.field( oldField );
            final JsonObject finalP = parent;
            field.ifPresent( f -> {
                finalP.deleteField( oldField );

                JsonObject np = root;
                for( String nf : newName.subList( 0, newName.size() - 1 ) ) {
                    final JsonObject finalParent = np;
                    np = finalParent.objectOpt( nf ).orElseGet( () -> {
                        final JsonObject jsonObject = new JsonObject( Optional.of( nf ), Optional.of( finalParent ), new HashMap<>() );
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
        final Optional<Json<?>> f = field( field );
        f.ifPresent( ff -> underlying.remove( field ) );
        return this;
    }

    @SuppressWarnings( "unchecked" )
    public <TIn extends Json, TOut extends Json> JsonObject map( String field, Function<TIn, TOut> func ) {
        field( field ).ifPresent( in -> underlying.put( field, func.apply( ( TIn ) in ).underlying ) );

        return this;
    }

    public Optional<String> stringOpt( String field ) {
        final Optional<Json<?>> f = field( field );
        return f.filter( ff -> ff.underlying instanceof String ).map( ff -> ( String ) ff.underlying );
    }

    public Optional<Long> longOpt( String field ) {
        final Optional<Json<?>> f = field( field );
        return f.filter( ff -> ff.underlying instanceof Number ).map( ff -> ( ( Number ) ff.underlying ).longValue() );
    }

    public Optional<JsonObject> objectOpt( String field ) {
        final Optional<Json<?>> f = field( field );
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
