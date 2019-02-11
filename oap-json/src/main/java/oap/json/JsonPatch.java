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

package oap.json;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import oap.reflect.TypeRef;
import oap.util.Stream;

import java.util.Map;

@Slf4j
public class JsonPatch {
    public static <T> T patchObject( Object dest, String draftJson ) {
        Map<String, Object> destSchema = Binder.json.unmarshal( new TypeReference<Map<String, Object>>() {}, dest );
        Map<String, Object> sourceSchema = Binder.json.unmarshal( new TypeRef<Map<String, Object>>() {}, draftJson );

        Stream.of( destSchema.entrySet() ).filter( e -> !e.getKey().equals( "id" ) )
            .filter( e -> sourceSchema.containsKey( e.getKey() ) )
            .forEach( e -> e.setValue( sourceSchema.get( e.getKey() ) ) );

        return Binder.json.unmarshal( dest.getClass(), destSchema );
    }
}
