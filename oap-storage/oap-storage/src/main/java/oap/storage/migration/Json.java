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

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class Json<T> {
    public final Optional<String> field;
    public final Optional<Json<?>> parent;
    public final T underlying;

    protected Json( T underlying, Optional<String> field, Optional<Json<?>> parent ) {
        this.underlying = underlying;
        this.field = field;
        this.parent = parent;
    }

    @SuppressWarnings( "unchecked" )
    static Optional<Json<?>> map( Optional<String> name, Object o, Optional<Json<?>> parent ) {
        if( o instanceof List<?> )
            return Optional.of( new JsonArray( name, parent, ( List<?> ) o ) );
        else if( o instanceof Map<?, ?> )
            return Optional.of( new JsonObject( name, parent, ( Map<String, Object> ) o ) );
        else if( o == null )
            return Optional.empty();
        return Optional.of( new JsonValue<>( o, name, parent ) );
    }

    public String getPath() {
        final String parentPath = parent.map( p -> getPath() ).orElse( "" );
        return parentPath.length() > 0 ? parentPath + "." + field.orElseThrow() : "";
    }

    @SuppressWarnings( "unchecked" )
    public <P extends JsonObject> P topParent() {
        JsonObject parent = ( JsonObject ) this.parent.orElseThrow();
        while( parent.parent.isPresent() ) parent = ( JsonObject ) parent.parent.get();
        return ( P ) parent;
    }
}
