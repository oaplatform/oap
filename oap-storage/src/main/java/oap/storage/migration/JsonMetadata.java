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

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by Igor Petrenko on 14.03.2016.
 */
public class JsonMetadata extends JsonObject {
    public JsonMetadata( Map<String, Object> underlying ) {
        super( Optional.empty(), Optional.empty(), underlying );
    }

    public JsonObject object() {
        return ( JsonObject ) field( "object" ).get();
    }

    public String id() {
        return s( "id" );
    }

    public long version() {
        return l( "version" );
    }

    public void incVersion() {
        this.mapL( "version", v -> v + 1 );
    }

    @Override
    public <T> JsonMetadata mapS( String field, Function<String, T> func ) {
        return ( JsonMetadata ) super.mapS( field, func );
    }
}
