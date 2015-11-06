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

package oap.application.remote;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class RemoteInvocation {
    public String service;
    public String method;
    public List<Argument> arguments = new ArrayList<>();

    public RemoteInvocation() {
    }

    public RemoteInvocation( String service, String method, List<Argument> arguments ) {
        this.service = service;
        this.method = method;
        this.arguments = arguments;
    }

    public Class<?>[] types() {
        return arguments.stream().map( v -> v.type ).toArray( Class[]::new );
    }

    public Object[] values() {
        return arguments.stream().map( v -> v.value ).toArray();
    }

    @ToString
    public static class Argument {
        public String name;
        @JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type" )
        public Class<?> type;
        public Object value;

        public Argument() {
        }

        public Argument( String name, Class<?> type, Object value ) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }
}
