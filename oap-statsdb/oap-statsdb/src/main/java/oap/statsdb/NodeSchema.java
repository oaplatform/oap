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

package oap.statsdb;

import lombok.SneakyThrows;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@ToString( callSuper = true )
public class NodeSchema extends ArrayList<NodeSchema.NodeConfiguration> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1625813602788861879L;

    private final Map<String, Class<? extends Node.Value>> cons = new HashMap<>();

    public NodeSchema() {
    }

    @SafeVarargs
    public NodeSchema( NodeConfiguration<? extends Node.Value>... confs ) {
        this( asList( confs ) );
    }

    public NodeSchema( List<NodeConfiguration<? extends Node.Value>> confs ) {
        super( confs );
        for( var c : confs ) {
            cons.put( c.key, c.clazz );
        }
    }

    public static <T extends Node.Value> NodeConfiguration<T> nc( String key, Class<T> clazz ) {
        return new NodeConfiguration<>( key, clazz );
    }

    @ToString
    public static class NodeConfiguration<T extends Node.Value> implements Serializable {
        private static final long serialVersionUID = -2296344454378267699L;

        public final String key;
        public final Class<T> clazz;

        public NodeConfiguration( String key, Class<T> clazz ) {
            this.key = key;
            this.clazz = clazz;
        }

        @SneakyThrows
        public Node.Value newInstance() {
            return clazz.getDeclaredConstructor().newInstance();
        }
    }
}
