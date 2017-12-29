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

package oap.security.acl;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by igor.petrenko on 29.12.2017.
 */
public interface AclSchema {
    void validateNewObject( AclObject parent, String newObjectType ) throws AclSecurityException;

    // bug
    // https://github.com/fasterxml/jackson-databind/issues/76
    // Problem handling datatypes Recursive type parameters
    @ToString
    class AclType {
        @JsonAnySetter
        public Map<String, List<AclType>> properties = new HashMap<>();

        public AclType() {
        }

        public List<AclType> get( String type ) {
            return properties.get( type );
        }

        public boolean containsKey( String objectType ) {
            return properties.containsKey( objectType );
        }
    }
}
