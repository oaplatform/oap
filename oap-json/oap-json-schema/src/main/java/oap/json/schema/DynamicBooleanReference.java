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

package oap.json.schema;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Optional;

@ToString
@EqualsAndHashCode
public class DynamicBooleanReference implements BooleanReference {
    private final String jsonPath;
    private final OperationFunction of;

    public DynamicBooleanReference( String jsonPath, OperationFunction of ) {
        this.jsonPath = jsonPath;
        this.of = of;
    }

    @Override
    public boolean apply( Object rootJson, Object currentJson, Optional<String> currentPath, Optional<String> prefix ) {
        var isRoot = !prefix.isPresent() || !jsonPath.startsWith( prefix.get() );
        var localJsonPath = isRoot ? jsonPath : jsonPath.substring( prefix.get().length() );
        var traverseObject = isRoot ? rootJson : currentJson;

        final List<Object> traverse = new JsonPath( localJsonPath, currentPath ).traverse( traverseObject );
        return !traverse.isEmpty() && of.apply( traverseObject, currentPath, traverse.get( 0 ) );
    }
}
